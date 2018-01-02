package org.jenkinsci.plugins.registry.notification.events.impl;

import hudson.Extension;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.EventTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;

public class TagDeleted extends EventType {

    public static final String JSON_TYPE = "TAG_DELETE";

    @DataBoundConstructor
    public TagDeleted() {}

    @Override
    public String getType() { return JSON_TYPE; }

    @Override
    public String getTimeStamp(JSONObject contents) { return contents.optString("deletedAt"); }

    @Extension
    public static class DescriptorImpl extends EventTypeDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.EventType_TagDeleted_DisplayName();
        }

        @CheckForNull
        public static TagDeleted.DescriptorImpl getInstance() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return jenkins.getDescriptorByType(TagDeleted.DescriptorImpl.class);
            }
            return null;
        }
    }
}
