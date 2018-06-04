package org.jenkinsci.plugins.registry.notification.webhook.acr;

import hudson.Extension;;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.JSONWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

/**
 * The ACRWebHook handles incoming updates from the Azure Container Registry. The provided payload differs minimally
 * from what is transmitted by a standard Docker Registry v2 server, which made this separate implementation necessary.
 */
@Extension
public class ACRWebHook extends JSONWebHook {
    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_NAME = "acr-webhook";

    @Override
    protected WebHookPayload createPushNotification(JSONObject payload) {
        return new ACRWebHookPayload(payload);
    }

    public String getUrlName() {
        return URL_NAME;
    }
}
