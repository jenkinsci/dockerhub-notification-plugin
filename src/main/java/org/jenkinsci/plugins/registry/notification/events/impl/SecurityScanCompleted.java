package org.jenkinsci.plugins.registry.notification.events.impl;

import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.EventTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

public class SecurityScanCompleted extends EventType {

    public static final String JSON_TYPE = "SCAN_COMPLETED";

    @DataBoundConstructor
    public SecurityScanCompleted() {}

    @Override
    public String getType() { return JSON_TYPE; }

    @Override
    public String getTimeStamp(JSONObject contents) { return contents.getJSONObject("scanSummary").optString("check_completed_at"); }

    @Override
    public boolean hasDigest() { return false; }

    @Extension
    public static class DescriptorImpl extends EventTypeDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.EventType_SecurityScanCompleted_DisplayName();
        }

        @CheckForNull
        public static SecurityScanCompleted.DescriptorImpl getInstance() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return jenkins.getDescriptorByType(SecurityScanCompleted.DescriptorImpl.class);
            }
            return null;
        }

    }
}
