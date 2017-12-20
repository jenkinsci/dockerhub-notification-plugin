package org.jenkinsci.plugins.registry.notification.events;

import hudson.EnvVars;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

import javax.annotation.Nonnull;

public abstract class EventType extends AbstractDescribableImpl<EventType> {

    public abstract boolean accepts(WebHookPayload payload);

    public void buildEnvironment(@Nonnull Run r, @Nonnull EnvVars envs) {
        //No-Op
    }
}
