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

import org.jenkinsci.plugins.dockerhub.notification.webhook.CallbackPayload;
import org.jenkinsci.plugins.dockerhub.notification.webhook.Http;
import org.jenkinsci.plugins.dockerhub.notification.webhook.WebHookPayload;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ProxyConfiguration;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates and sends back a {@link org.jenkinsci.plugins.dockerhub.notification.webhook.CallbackPayload}
 * to Docker Hub when all builds are finalized.
 */
@Extension
public class Coordinator extends RunListener<Run<?, ?>> {

    public void onTriggered(@Nonnull Job job, @Nonnull WebHookPayload payload) {
        logger.log(Level.FINER, "Job {0} triggered for payload: {1}", new Object[]{job.getFullDisplayName(), payload});
        TriggerStore.getInstance().triggered(payload, job);
    }

    @Override
    public void onStarted(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        DockerHubWebHookCause cause = run.getCause(DockerHubWebHookCause.class);
        if (cause != null) {
            logger.log(Level.FINER, "Build {0} started for cause: {1}", new Object[]{run.getFullDisplayName(), cause});
            TriggerStore.getInstance().started(cause.getPayload(), run);
        }
    }

    @Override
    public void onFinalized(@Nonnull Run<?, ?> run) {
        DockerHubWebHookCause cause = run.getCause(DockerHubWebHookCause.class);
        if (cause != null) {
            logger.log(Level.FINER, "Build {0} done for cause: [{1}]", new Object[]{run.getFullDisplayName(), cause});
            TriggerStore.TriggerEntry entry = TriggerStore.getInstance().finalized(cause.getPayload(), run);
            if (entry != null) {
                if(entry.areAllDone()) {
                    logger.log(Level.FINE, "All builds for [{0}] are done, preparing callback to Docker Hub", cause);
                    try {
                        CallbackPayload callback = CallbackPayload.from(entry);
                        if (callback != null) {
                            entry.setCallbackData(callback);
                            TriggerStore.getInstance().save(entry);
                            sendResponse(cause.getPayload(), callback);
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to update Docker Hub!", e);
                    }
                }
            } else {
                logger.log(Level.SEVERE, "Failed to do final evaluation of builds for cause [{0}]", cause);
            }
        }
    }

    private void sendResponse(@Nonnull final WebHookPayload payload, @Nonnull final CallbackPayload callback) throws IOException, ExecutionException, InterruptedException {
        final String callbackUrl = payload.getCallbackUrl();
        if (!StringUtils.isBlank(callbackUrl)) {
            logger.log(Level.FINE, "Sending callback to Docker Hub");
            logger.log(Level.FINER, "Callback: {0}", callback);
            int response = Http.post(callbackUrl, callback.toJSON());
            logger.log(Level.FINE, "Docker Hub returned {0}", response);
        } else {
            logger.log(Level.WARNING, "No callback URL specified in {0}", payload);
        }
    }

    @Override
    public void onDeleted(@Nonnull Run<?, ?> run) {
        DockerHubWebHookCause cause = run.getCause(DockerHubWebHookCause.class);
        if (cause != null) {
            TriggerStore.getInstance().removed(cause.getPayload(), run);
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
