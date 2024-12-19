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
package org.jenkinsci.plugins.registry.notification.webhook.nexus;

import hudson.Util;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.jenkinsci.plugins.registry.notification.webhook.dockerregistry.DockerRegistryWebHookCause;
import org.jenkinsci.plugins.registry.notification.webhook.dockerregistry.DockerRegistryWebHookPayload;

import javax.annotation.CheckForNull;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Received notification from the Nexus Docker Registry web hook.
 */
public class NexusDockerRegistryPushNotification extends PushNotification {
    private static final long serialVersionUID = 207798312860576090L;
    private static final Logger logger = Logger.getLogger(NexusDockerRegistryPushNotification.class.getName());
    public static final String KEY_REPO_NAME = WebHookPayload.PREFIX + "REPO_NAME";
    public static final String KEY_DOCKER_REGISTRY_HOST = WebHookPayload.PREFIX + "DOCKER_REGISTRY_HOST";
    private String registryHost;
    private String tag;

    public NexusDockerRegistryPushNotification(NexusDockerRegistryWebHookPayload webHookPayload, String repoName, String tag) {
        super(webHookPayload);
        this.repoName = repoName;
        this.tag = tag;
    }

    @CheckForNull
    public String getRegistryHost() {
        return registryHost;
    }

    public String getTag() {
        return this.tag;
    }

    public void setRegistryHost(String registryHost) {
        this.registryHost = registryHost;
    }

    @Override
    public Cause getCause() {
        return new NexusDockerRegistryWebHookCause(this);
    }

    @Override
    public Set<ParameterValue> getRunParameters() {
        Set<ParameterValue> parameters = new HashSet<ParameterValue>();
        parameters.add(new StringParameterValue(KEY_REPO_NAME, getRepoName()));
        String host = getRegistryHost();
        if (!StringUtils.isBlank(host)) {
            parameters.add(new StringParameterValue(KEY_DOCKER_REGISTRY_HOST, host));
        }
        return parameters;
    }

    @Override
    public String getCauseMessage() {
        return "Docker image " + getRepoName() + " has been rebuilt by Nexus DockerRegistry@" + getRegistryHost();
    }

    public String sha() {
        return Util.getDigestOf("dockerRegistryNotification:" + repoName + Long.toBinaryString(getReceived()));
    }

    @Override
    public String getShortDescription() {
        return String.format("push of %s to Nexus DockerRegistry@%s", getRepoName(), getRegistryHost());

    }

}
