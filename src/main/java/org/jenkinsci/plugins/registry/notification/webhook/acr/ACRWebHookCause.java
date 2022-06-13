package org.jenkinsci.plugins.registry.notification.webhook.acr;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookCause;


public class ACRWebHookCause extends WebHookCause {
    public ACRWebHookCause(@NonNull ACRPushNotification notification) {
        super(notification);
    }

    @Override
    public String getShortDescription() {
        return String.format("Triggered by %s", getPushNotification().getShortDescription());
    }

    @Override
    public String toString() {
        return "ACRWebHookCause{" +
                "payload=" + getPushNotification().getWebHookPayload() +
                '}';
    }
}
