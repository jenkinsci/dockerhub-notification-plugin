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

import hudson.Util;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

import javax.annotation.CheckForNull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Received payload of data from the Docker Hub web hook.
 * See <a href="http://docs.docker.io/docker-hub/repos/#webhooks">Reference</a>
*/
public class DockerHubPushNotification extends PushNotification {
    private static final long serialVersionUID = 207798312860576090L;
    public static final String KEY_REPO_NAME = WebHookPayload.PREFIX + "REPO_NAME";
    public static final String KEY_DOCKER_HUB_HOST = WebHookPayload.PREFIX + "DOCKER_HUB_HOST";
    public static final String KEY_PUSHER = WebHookPayload.PREFIX + "PUSHER";
    public static final String KEY_TAG = WebHookPayload.PREFIX + "TAG";
    private static final Logger logger = Logger.getLogger(DockerHubPushNotification.class.getName());
    private String callbackUrl;

    public DockerHubPushNotification(DockerHubWebHookPayload webHookPayload, String repoName) {
        super(webHookPayload);
        this.repoName = repoName;
    }

    @CheckForNull
    public String getCallbackUrl() {
        return this.callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    @CheckForNull
    public String getRegistryHost() {
        String urlS = getCallbackUrl();
        if (urlS != null) {
            try {
                URL url = new URL(urlS);
                return url.getHost();
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "DockerHub is sending malformed data. ", e);
            }
        }
        return null;
    }

    @CheckForNull
    public String getPusher() {
        JSONObject data = getWebHookPayload().getData();
        if (data != null) {
            JSONObject push_data = data.optJSONObject("push_data");
            if (push_data != null) {
                return push_data.optString("pusher");
            }
        }
        return null;
    }

    @CheckForNull
    public String getTag() {
        JSONObject data = getWebHookPayload().getData();
        if (data != null) {
            JSONObject push_data = data.optJSONObject("push_data");
            if (push_data != null) {
                return push_data.optString("tag");
            }
        }
        return null;
    }

    @Override
    public Cause getCause() {
        return new DockerHubWebHookCause(this);
    }

    @Override
    public Set<ParameterValue> getRunParameters() {
        Set<ParameterValue> parameters = new HashSet<ParameterValue>();
        parameters.add(new StringParameterValue(KEY_REPO_NAME, getRepoName()));
        String host = getRegistryHost();
        if (!StringUtils.isBlank(host)) {
            parameters.add(new StringParameterValue(KEY_DOCKER_HUB_HOST, host));
        }
        String tag = getTag();
        if (!StringUtils.isBlank(tag)) {
            parameters.add(new StringParameterValue(KEY_TAG, tag));
        }
        String pusher = getPusher();
        if (!StringUtils.isBlank(pusher)) {
            parameters.add(new StringParameterValue(KEY_PUSHER, pusher));
        }
        return parameters;
    }

    @Override
    public String getCauseMessage() {
        return "Docker image " + getRepoName() + " has been rebuilt by DockerHub@" + getRegistryHost();
    }

    public String sha() {
        return Util.getDigestOf("dockerHubNotification:" + repoName + Long.toBinaryString(getReceived()));
    }

    @Override
    public String getShortDescription() {
        return String.format("push of %s to DockerHub@%s", getRepoName(), getRegistryHost());

    }

}
