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
package org.jenkinsci.plugins.registry.notification.webhook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.*;
import hudson.model.Queue;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.Coordinator;
import org.jenkinsci.plugins.registry.notification.DockerHubTrigger;
import org.jenkinsci.plugins.registry.notification.TriggerStore;
import org.jenkinsci.plugins.registry.notification.token.ApiTokens;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.interceptor.RespondSuccess;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JSONWebHook implements UnprotectedRootAction {
    private static final Logger logger = Logger.getLogger(JSONWebHook.class.getName());
    private static /*almost final*/ boolean DO_NOT_REQUIRE_API_TOKEN = jenkins.util.SystemProperties.getBoolean(JSONWebHook.class.getName() + "DO_NOT_REQUIRE_API_TOKEN");

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "DockerHub web hook";
    }

    @RequirePOST
    @RespondSuccess
    public void doNotify(@QueryParameter(required = false) String payload, StaplerRequest2 request, StaplerResponse2 response) throws IOException {
        if (!DO_NOT_REQUIRE_API_TOKEN) {
            checkValidApiToken(request, response);
        }
        WebHookPayload hookPayload = null;
        if (payload != null) {
            try {
                hookPayload = createPushNotification(JSONObject.fromObject(payload));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not parse the web hook payload!", e);
            }
        } else {
            hookPayload = parse(request);
        }
        if (hookPayload != null) {
            for (PushNotification pushNotification : hookPayload.getPushNotifications()) {
                try {
                    trigger(response, pushNotification);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Could not trigger a job!", e);
                }
            }
        }
    }

    private void checkValidApiToken(final StaplerRequest2 request, final StaplerResponse2 response) throws IOException {
        final Ancestor ancestor = request.findAncestor(ValidApiToken.class);
        if (ancestor == null) {
            response.sendError(403, "No valid API token provided.");
            throw new AccessDeniedException("No valid API token provided.");
        }
    }

    public ValidApiToken getDynamic(String token, StaplerResponse2 rsp) throws IOException {
        if (ApiTokens.get().isValidApiToken(token)) {
            return new ValidApiToken(token, this);
        } else {
            rsp.sendError(403, "No valid API token provided.");
            return null;
        }
    }

    /**
     * Stapler entry for the multi build result page
     * @param sha the id of the trigger data.
     * @return the details
     * @throws IOException if so
     * @throws InterruptedException if so
     */
    @NonNull
    public ResultPage getDetails(@NonNull final String sha) throws IOException, InterruptedException {
        TriggerStore.TriggerEntry entry = TriggerStore.getInstance().getEntry(sha);
        if (entry != null) {
            return new ResultPage(entry);
        } else {
            return ResultPage.NO_RESULT;
        }
    }

    protected void trigger(StaplerResponse2 response, final PushNotification pushNotification) throws IOException {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return;
        }
        ACL.impersonate2(ACL.SYSTEM2, new Runnable() {
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
                    Set<String> allRepoNames = trigger.getAllRepoNames();
                    String repoName = pushNotification.getRepoName();
                    if (allRepoNames.contains(repoName)) {
                        schedule((Job) p, pushNotification);
                    }
                }
            }
        });
    }

    private void schedule(@NonNull final Job job, @NonNull final PushNotification pushNotification) {
        if (new JobbMixIn(job).schedule(pushNotification.getCause())) {
            logger.info(pushNotification.getCauseMessage());
            Coordinator coordinator = Coordinator.getInstance();
            if (coordinator != null) {
                coordinator.onTriggered(job, pushNotification);
            }
        }
    }

    /**
     * If someone wanders in to the index page, redirect to Jenkins root.
     *
     * @param request the request object
     * @param response the response object
     * @throws IOException if so
     */
    public void doIndex(StaplerRequest2 request, StaplerResponse2 response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/");
    }

    protected abstract WebHookPayload createPushNotification(JSONObject data);

    private WebHookPayload parse(StaplerRequest2 req) throws IOException {
        //TODO Actually test what duckerhub is really sending
        String body = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
        String contentType = req.getContentType();
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
            body = URLDecoder.decode(body, req.getCharacterEncoding());
        }
        logger.log(Level.FINER, "Received commit hook notification : {0}", body);
        try {
            JSONObject payload = JSONObject.fromObject(body);
            return createPushNotification(payload);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse the web hook payload!", e);
            return null;
        }
    }

    /**
     * Workaround until {@link ParameterizedJobMixIn#getDefaultParametersValues()} gets public.
     */
    static class JobbMixIn<JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob<JobT, RunT> & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends ParameterizedJobMixIn<JobT, RunT> {

        /**
         * Some breathing room to iterate through most/all of the jobs before the first triggered build starts.
         */
        public static final int MIN_QUIET = 3;

        private JobT the;


        JobbMixIn(JobT the) {
            this.the = the;
        }

        @Override
        protected JobT asJob() {
            return the;
        }

        boolean schedule(Cause cause) {
            if (!asJob().isBuildable()) {
                return false;
            }
            List<Action> queueActions = new LinkedList<Action>();

            queueActions.add(new ParametersAction(getParameterValues()));
            queueActions.add(new CauseAction(cause));

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

        private List<ParameterValue> getParameterValues() {
            Set<ParameterValue> result = new HashSet<ParameterValue>();
            if (isParameterized()) {
                result.addAll(getDefaultParametersValues());
            }
            return Collections.unmodifiableList(new LinkedList<ParameterValue>(result));
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

    public static class ValidApiToken {
        private final String token;
        private final JSONWebHook delegate;

        public ValidApiToken(final String token, final JSONWebHook delegate) {
            this.token = token;
            this.delegate = delegate;
        }

        public String getToken() {
            return token;
        }

        @RequirePOST
        @RespondSuccess
        public void doNotify(@QueryParameter(required = false) String payload, StaplerRequest2 request, StaplerResponse2 response) throws IOException {
            delegate.doNotify(payload, request, response);
        }
    }
}
