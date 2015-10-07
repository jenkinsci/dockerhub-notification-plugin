package org.jenkinsci.plugins.registry.notification;

import hudson.Main;
import hudson.model.*;
import hudson.model.Queue;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.registry.notification.webhook.ResultPage;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHookPayload;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by lguminski on 06/10/15.
 */
public abstract class JSONWebHook implements UnprotectedRootAction {
    private static final Logger logger = Logger.getLogger(JSONWebHook.class.getName());

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "DockerHub web hook";
    }

    public String getUrlName() {
        return DockerHubWebHook.URL_NAME;
    }

    abstract public void doNotify(@QueryParameter(required = false) String payload, StaplerRequest request, StaplerResponse response) throws IOException;

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
        trigger(response, new DockerHubWebHookPayload(image)); //TODO pre-filled json data when needed.
    }

    protected void trigger(StaplerResponse response, final WebHookPayload hookPayload) throws IOException {
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
                        schedule((Job) p, hookPayload);
                    }
                }
            }
        });
        response.sendRedirect("../");
    }

    private void schedule(@Nonnull final Job job, @Nonnull final WebHookPayload payload) {
        if (new JobbMixIn(job).schedule(payload.getCause(), payload.getJobParamerers())) {
            logger.info(payload.getCauseMessage());
            Coordinator coordinator = Coordinator.getInstance();
            if (coordinator != null) {
                coordinator.onTriggered(job, payload);
            }
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

    /**
     * Workaround until {@link ParameterizedJobMixIn#getDefaultParametersValues()} gets public.
     */
    static class JobbMixIn<JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends ParameterizedJobMixIn<JobT, RunT> {

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

        public boolean schedule(Cause cause, Set<ParameterValue> parameters) {
            if (!asJob().isBuildable()) {
                return false;
            }
            List<Action> queueActions = new LinkedList<Action>();

            queueActions.add(new ParametersAction(getParameterValues(parameters)));
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

        private List<ParameterValue> getParameterValues(Set<ParameterValue> parameters) {
            Set<ParameterValue> result = new HashSet<ParameterValue>();
            if (isParameterized()) {
                result.addAll(getDefaultParametersValues());
            }
            result.addAll(parameters);
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
}
