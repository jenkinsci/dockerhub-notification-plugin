/**
 * The MIT License
 *
 * Copyright (c) 2015, HolidayCheck AG.
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
package org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.dockerregistry;

import hudson.Extension;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.JSONWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

import java.util.logging.Logger;

/**
 * The terminal point for the DockerTrustedRegistry web hook.
 * See <a href="https://docs.docker.com/datacenter/dtr/2.4/guides/user/create-and-manage-webhooks">Reference</a>
 */
@Extension
public class DockerTrustedRegistryWebHook extends JSONWebHook {
    private static final Logger logger = Logger.getLogger(DockerTrustedRegistryWebHook.class.getName());

    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_NAME = "dockertrustedregistry-webhook";

    @Override
    protected WebHookPayload createPushNotification(JSONObject payload) {
        return new DockerTrustedRegistryWebHookPayload(payload);
    }

    public String getUrlName() {
        return URL_NAME;
    }
}
