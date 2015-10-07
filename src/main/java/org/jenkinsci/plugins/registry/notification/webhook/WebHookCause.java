package org.jenkinsci.plugins.registry.notification.webhook;

import hudson.model.Cause;

import javax.annotation.Nonnull;

/**
 * Created by lguminski on 07/10/15.
 */
public abstract class WebHookCause extends Cause {
    @Nonnull
    protected final PushNotification pushNotification;

    public WebHookCause(@Nonnull PushNotification pushNotification) {
        this.pushNotification = pushNotification;
    }

    @Nonnull
    public PushNotification getPushNotification() {
        return pushNotification;
    }
}
