package org.jenkinsci.plugins.registry.notification.events.impl;

import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.EventTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

public class ManifestDeleted extends EventType {

    public static final String JSON_TYPE = "MANIFEST_DELETE";

    @DataBoundConstructor
    public ManifestDeleted() {}

    @Override
    public String getType() { return JSON_TYPE; }

    @Override
    public String getTimeStamp(JSONObject contents) { return contents.optString("deletedAt"); }

    @Extension
    public static class DescriptorImpl extends EventTypeDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.EventType_ManifestDeleted_DisplayName();
        }

        @CheckForNull
        public static ManifestDeleted.DescriptorImpl getInstance() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return jenkins.getDescriptorByType(ManifestDeleted.DescriptorImpl.class);
            }
            return null;
        }

    }
}
