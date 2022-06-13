package org.jenkinsci.plugins.registry.notification.webhook.acr;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

import java.util.HashSet;
import java.util.Set;

public class ACRPushNotification extends PushNotification {
    public static final String KEY_REPO_NAME = WebHookPayload.PREFIX + "REPO_NAME";
    public static final String KEY_DOCKER_REGISTRY_HOST = WebHookPayload.PREFIX + "DOCKER_REGISTRY_HOST";
    public static final String KEY_TAG = WebHookPayload.PREFIX + "TAG";

    private String registryHost;
    private String tag;

    public ACRPushNotification(ACRWebHookPayload webHookPayload, String repoName) {
        super(webHookPayload);
        if (webHookPayload != null) {
            JSONObject data = webHookPayload.getData();
            if (data != null) {
                JSONObject target = data.getJSONObject("target");
                if (target != null) {
                    this.tag = target.optString("tag");
                }
            }
        }
        this.repoName = repoName;
    }

    @CheckForNull
    public String getRegistryHost() {
        return registryHost;
    }

    public void setRegistryHost(String registryHost) {
        this.registryHost = registryHost;
    }

    public String getTag() {
        return this.tag;
    }

    @Override
    public Cause getCause() {
        return new ACRWebHookCause(this);
    }

    @Override
    public Set<ParameterValue> getRunParameters() {
        Set<ParameterValue> parameters = new HashSet<ParameterValue>();
        parameters.add(new StringParameterValue(KEY_REPO_NAME, getRepoName()));
        parameters.add(new StringParameterValue(KEY_TAG, getTag()));
        String host = getRegistryHost();
        if (!StringUtils.isBlank(host)) {
            parameters.add(new StringParameterValue(KEY_DOCKER_REGISTRY_HOST, host));
        }
        return parameters;
    }

    @Override
    public String getCauseMessage() {
        return "Docker image " + getRepoName() + " has been rebuilt by ACR@" + getRegistryHost();
    }

    public String sha() {
        return Util.getDigestOf("acrNotification:" + repoName + Long.toBinaryString(getReceived()));
    }

    @Override
    public String getShortDescription() {
        return String.format("push of %s to ACR@%s", getRepoName(), getRegistryHost());

    }
}
