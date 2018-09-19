package org.jenkinsci.plugins.registry.notification.events.impl;

import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.EventTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

public class TagPushed extends EventType {

    public static final String JSON_TYPE = "TAG_PUSH";
    @DataBoundConstructor
    public TagPushed() {}

    @Override
    public String getType() { return JSON_TYPE; }

    @Override
    public String getTimeStamp(JSONObject contents) { return contents.optString("pushedAt"); }

    @Extension
    public static class DescriptorImpl extends EventTypeDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.EventType_TagPushed_DisplayName();
        }

        @CheckForNull
        public static DescriptorImpl getInstance() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return jenkins.getDescriptorByType(DescriptorImpl.class);
            }
            return null;
        }
    }
}
