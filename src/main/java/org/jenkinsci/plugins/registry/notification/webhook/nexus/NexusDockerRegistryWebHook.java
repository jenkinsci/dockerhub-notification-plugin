/**
 * The MIT License
 * <p>
 * Copyright (c) 2015, HolidayCheck AG.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.registry.notification.webhook.nexus;

import hudson.Extension;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.JSONWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.kohsuke.stapler.StaplerRequest;

/**
 * The terminal point for the Nexus DockerRegistry web hook.
 */
@Extension
public class NexusDockerRegistryWebHook extends JSONWebHook {
    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_NAME = "nexusregistry-webhook";

    @Override
    protected WebHookPayload createPushNotification(JSONObject payload, StaplerRequest request) {
        return new NexusDockerRegistryWebHookPayload(payload, request.getParameter("host"));
    }

    public String getUrlName() {
        return URL_NAME;
    }
}
