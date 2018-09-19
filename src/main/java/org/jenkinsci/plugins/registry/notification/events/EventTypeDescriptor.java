package org.jenkinsci.plugins.registry.notification.events;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

public abstract class EventTypeDescriptor extends Descriptor<EventType> {

    public static final String ENVIRONMENT_KEY = WebHookPayload.PREFIX + "EVENT";

    public EventTypeDescriptor(Class<? extends EventType> clazz) {
        super(clazz);
    }

    public EventTypeDescriptor() {
    }


    public static ExtensionList<EventTypeDescriptor> all() {
        return ExtensionList.lookup(EventTypeDescriptor.class);
    }
}
