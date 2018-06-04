package org.jenkinsci.plugins.registry.notification.webhook.acr;

import org.jenkinsci.plugins.registry.notification.webhook.WebHookCause;

import javax.annotation.Nonnull;

public class ACRWebHookCause extends WebHookCause {
    public ACRWebHookCause(@Nonnull ACRPushNotification notification) {
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
