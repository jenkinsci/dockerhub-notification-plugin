package org.jenkinsci.plugins.registry.notification.events.impl;

import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.EventTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

public class ManifestPushed extends EventType {

    public static final String JSON_TYPE = "MANIFEST_PUSH";

    @DataBoundConstructor
    public ManifestPushed() {}

    @Override
    public String getType() { return JSON_TYPE; }

    @Override
    public String getTimeStamp(JSONObject contents) { return contents.optString("pushedAt"); }

    @Extension
    public static class DescriptorImpl extends EventTypeDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.EventType_ManifestPushed_DisplayName();
        }

        @CheckForNull
        public static ManifestPushed.DescriptorImpl getInstance() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return jenkins.getDescriptorByType(ManifestPushed.DescriptorImpl.class);
            }
            return null;
        }
    }
}
