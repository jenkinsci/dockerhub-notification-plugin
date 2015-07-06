/**
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.dockerhub.notification;

import org.jenkinsci.plugins.dockerhub.notification.webhook.ResultPage;
import org.jenkinsci.plugins.dockerhub.notification.webhook.WebHookPayload;
import hudson.Extension;
import hudson.Main;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The terminal point for the DockerHub web hook.
 * See <a href="http://docs.docker.io/docker-hub/repos/#webhooks">Reference</a>
 */
@Extension
public class DockerHubWebHook implements UnprotectedRootAction {

    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_NAME = "dockerhub-webhook";

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "DockerHub web hook";
    }

    public String getUrlName() {
        return URL_NAME;
    }

    @RequirePOST
    public void doNotify(@QueryParameter(required = false) String payload, StaplerRequest request, StaplerResponse response) throws IOException {
        WebHookPayload hookPayload = null;
        if (payload != null) {
            try {
                hookPayload = new WebHookPayload(JSONObject.fromObject(payload));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not parse the web hook payload!", e);
            }
        } else {
            hookPayload = parse(request);
        }
        if (hookPayload != null) {
            trigger(response, hookPayload);
        }
    }

    /**
     * Stapler entry for the multi build result page
     * @param sha the id of the trigger data.
     */
    @Nonnull
    public ResultPage getDetails(@Nonnull final String sha) throws IOException, InterruptedException {
        TriggerStore.TriggerEntry entry = TriggerStore.getInstance().getEntry(sha);
        if (entry != null) {
            return new ResultPage(entry);
        } else {
            return ResultPage.NO_RESULT;
        }
    }

    private boolean isDebugMode() {
        if (Main.isDevelopmentMode || Main.isUnitTest) {
            return true;
        } else if (System.getProperty("hudson.hpi.run") != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper for development without Dockerub integration.
     *
     * @param image    the docker image to trigger
     * @param response to send a redirect to
     * @throws IOException if so
     */
    public void doDebug(@QueryParameter(required = true) String image, StaplerResponse response) throws IOException {
        if (!isDebugMode()) {
            throw new IllegalStateException("This endpoint can only be used during development!");
        }
        trigger(response, new WebHookPayload(image)); //TODO pre-filled json data when needed.
    }

    private void trigger(StaplerResponse response, final WebHookPayload hookPayload) throws IOException {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return;
        }
        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                // search all jobs for DockerHubTrigger
                for (ParameterizedJobMixIn.ParameterizedJob p : jenkins.getAllItems(ParameterizedJobMixIn.ParameterizedJob.class)) {
                    DockerHubTrigger trigger = DockerHubTrigger.getTrigger(p);
                    if (trigger == null) {
                        logger.log(Level.FINER, "job {0} doesn't have DockerHubTrigger set", p.getName());
                        continue;
                    }
                    logger.log(Level.FINER, "Inspecting candidate job {0}", p.getName());
                    if (trigger.getAllRepoNames().contains(hookPayload.getRepoName())) {
                        schedule((Job)p, hookPayload);
                    }
                }
            }
        });
        response.sendRedirect("../");
    }

    private void schedule(@Nonnull final Job job, @Nonnull final WebHookPayload payload) {
        if (new JobbMixIn(job).schedule(payload)) {
            logger.info("Scheduled job " + job.getName() + " as Docker image " + payload.getRepoName() + " has been rebuilt by DockerHub@" + payload.getCallbackHost());
            Coordinator coordinator = Coordinator.getInstance();
            if (coordinator != null) {
                coordinator.onTriggered(job, payload);
            }
        }
    }

    private WebHookPayload parse(StaplerRequest req) throws IOException {
        //TODO Actually test what duckerhub is really sending
        String body = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
        String contentType = req.getContentType();
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
            body = URLDecoder.decode(body, req.getCharacterEncoding());
        }
        logger.log(Level.FINER, "Received commit hook notification : {0}", body);
        try {
            JSONObject payload = JSONObject.fromObject(body);
            return new WebHookPayload(payload);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse the web hook payload!", e);
            return null;
        }

    }


    private static final Logger logger = Logger.getLogger(DockerHubWebHook.class.getName());

    /**
     * Workaround until {@link ParameterizedJobMixIn#getDefaultParametersValues()} gets public.
     */
    static class JobbMixIn<JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends ParameterizedJobMixIn<JobT, RunT> {
        public static final String PREFIX = "DOCKER_TRIGGER_";
        public static final String KEY_REPO_NAME = PREFIX + "REPO_NAME";
        public static final String KEY_DOCKER_HUB_HOST = PREFIX + "DOCKER_HUB_HOST";
        /**
         * Some breathing room to iterate through most/all of the jobs before the first triggered build starts.
         */
        public static final int MIN_QUIET = 3;

        private JobT the;


        public JobbMixIn(JobT the) {
            this.the = the;
        }

        @Override
        protected JobT asJob() {
            return the;
        }

        public boolean schedule(WebHookPayload payload) {
            if (!asJob().isBuildable()) {
                return false;
            }
            List<ParameterValue> parameters = getParameterValues(payload);
            List<Action> queueActions = new LinkedList<Action>();

            queueActions.add(new ParametersAction(parameters));
            queueActions.add(new CauseAction(new DockerHubWebHookCause(payload)));

            int quiet = Math.max(MIN_QUIET, asJob().getQuietPeriod());

            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                logger.log(Level.WARNING, "Tried to schedule a build while Jenkins was gone.");
                return false;
            }
            final Queue queue = jenkins.getQueue();
            if (queue == null) {
                throw new IllegalStateException("The queue is not initialized?!");
            }
            Queue.Item i = queue.schedule2(asJob(), quiet, queueActions).getItem();
            return i != null && i.getFuture() != null;
        }

        private List<ParameterValue> getParameterValues(WebHookPayload payload) {
            List<ParameterValue> parameters = new LinkedList<ParameterValue>();
            if (isParameterized()) {
                Collection<ParameterValue> defaults = getDefaultParametersValues();
                for (ParameterValue value : defaults) {
                    if (!value.getName().equalsIgnoreCase(KEY_REPO_NAME) && !value.getName().equalsIgnoreCase(KEY_DOCKER_HUB_HOST)) {
                        parameters.add(value);
                    }
                }
            }
            parameters.add(new StringParameterValue(KEY_REPO_NAME, payload.getRepoName()));
            String host = payload.getCallbackHost();
            if (!StringUtils.isBlank(host)) {
                parameters.add(new StringParameterValue(KEY_DOCKER_HUB_HOST, host));
            }
            return parameters;
        }

        /**
         * Direct copy from {@link ParameterizedJobMixIn#getDefaultParametersValues()} (version 1.580).
         *
         * @return the configured parameters with their default values.
         */
        private List<ParameterValue> getDefaultParametersValues() {
            ParametersDefinitionProperty paramDefProp = asJob().getProperty(ParametersDefinitionProperty.class);
            ArrayList<ParameterValue> defValues = new ArrayList<ParameterValue>();

        /*
         * This check is made ONLY if someone will call this method even if isParametrized() is false.
         */
            if (paramDefProp == null)
                return defValues;

        /* Scan for all parameter with an associated default values */
            for (ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
                ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

                if (defaultValue != null)
                    defValues.add(defaultValue);
            }

            return defValues;
        }

    }

    /**
     * If someone wanders in to the index page, redirect to Jenkins root.
     *
     * @param response the response object
     * @throws IOException if so
     */
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/");
    }
}
