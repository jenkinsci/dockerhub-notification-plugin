package org.jenkinsci.plugins.registry.notification.events.impl;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.EventTypeDescriptor;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class ManifestDeleted extends EventType {

    @DataBoundConstructor
    public ManifestDeleted() {
    }

    @Override
    public boolean accepts(WebHookPayload payload) {
        return false; //TODO
    }

    @Override
    public void buildEnvironment(@Nonnull Run r, @Nonnull EnvVars envs) {
        envs.put(EventTypeDescriptor.ENVIRONMENT_KEY, "MANIFEST_DELETED"); //TODO perhaps something more appropriate?
    }

    @Extension
    public static class DescriptorImpl extends EventTypeDescriptor {
        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.EventType_ManifestDeleted_DisplayName();
        }
    }
}
