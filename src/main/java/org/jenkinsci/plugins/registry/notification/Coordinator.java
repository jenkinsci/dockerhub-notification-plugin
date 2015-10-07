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
package org.jenkinsci.plugins.registry.notification;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.registry.notification.webhook.*;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubCallbackPayload;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHookCause;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates and sends back a {@link DockerHubCallbackPayload}
 * to Docker Hub when all builds are finalized.
 */
@Extension
public class Coordinator extends RunListener<Run<?, ?>> {

    public void onTriggered(@Nonnull Job job, @Nonnull PushNotification pushNotification) {
        logger.log(Level.FINER, "Job {0} triggered for payload: {1}", new Object[]{job.getFullDisplayName(), pushNotification});
        TriggerStore.getInstance().triggered(pushNotification, job);
    }

    @Override
    public void onStarted(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        DockerHubWebHookCause cause = run.getCause(DockerHubWebHookCause.class);
        if (cause != null) {
            logger.log(Level.FINER, "Build {0} started for cause: {1}", new Object[]{run.getFullDisplayName(), cause});
            TriggerStore.getInstance().started(cause.getPushNotification(), run);
        }
    }

    @Override
    public void onFinalized(@Nonnull Run<?, ?> run) {
        WebHookCause cause = run.getCause(WebHookCause.class);
        if (cause != null) {
            logger.log(Level.FINER, "Build {0} done for cause: [{1}]", new Object[]{run.getFullDisplayName(), cause});
            TriggerStore.TriggerEntry entry = TriggerStore.getInstance().finalized(cause.getPushNotification(), run);
            if (entry != null) {
                if(entry.areAllDone()) {
                    logger.log(Level.FINE, "All builds for [{0}] are done, preparing callback to Docker Hub", cause);
                    try {
                        sendResponse(cause.getPushNotification(), run);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to update Docker Hub!", e);
                    }
                }
            } else {
                logger.log(Level.SEVERE, "Failed to do final evaluation of builds for cause [{0}]", cause);
            }
        }
    }

    private void sendResponse(@Nonnull final PushNotification pushNotification, Run<?, ?> run) throws IOException, ExecutionException, InterruptedException {
        pushNotification.getCallbackHandler().notify(pushNotification, run);
    }

    @Override
    public void onDeleted(@Nonnull Run<?, ?> run) {
        DockerHubWebHookCause cause = run.getCause(DockerHubWebHookCause.class);
        if (cause != null) {
            TriggerStore.getInstance().removed(cause.getPushNotification(), run);
        }
    }

    @CheckForNull
    public static Coordinator getInstance() {
        Jenkins j = Jenkins.getInstance();
        if (j != null) {
            ExtensionList<Coordinator> list = j.getExtensionList(Coordinator.class);
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    private static final Logger logger = Logger.getLogger(Coordinator.class.getName());
}
