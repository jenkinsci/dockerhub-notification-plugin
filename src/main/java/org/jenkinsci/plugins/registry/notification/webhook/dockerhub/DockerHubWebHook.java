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
package org.jenkinsci.plugins.registry.notification.webhook.dockerhub;

import hudson.Extension;
import hudson.Main;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.JSONWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The terminal point for the DockerHub web hook.
 * See <a href="http://docs.docker.io/docker-hub/repos/#webhooks">Reference</a>
 */
@Extension
public class DockerHubWebHook extends JSONWebHook {
    private static final Logger logger = Logger.getLogger(DockerHubWebHook.class.getName());

    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_NAME = "dockerhub-webhook";

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
        DockerHubWebHookPayload dockerHubWebHookPayload = new DockerHubWebHookPayload(image);
        for (PushNotification pushNotification : dockerHubWebHookPayload.getPushNotifications()) {
            trigger(response, pushNotification); //TODO pre-filled json data when needed.
        }
    }

    @Override
    protected void trigger(StaplerResponse response, PushNotification pushNotification) throws IOException {
        super.trigger(response, pushNotification);
        response.sendRedirect("../");
    }

    @Override
    protected WebHookPayload createPushNotification(JSONObject payload) {
        return new DockerHubWebHookPayload(payload);
    }

    public String getUrlName() {
        return URL_NAME;
    }
}
