package org.jenkinsci.plugins.registry.notification.events;

import hudson.EnvVars;
import hudson.model.AbstractDescribableImpl;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public abstract class EventType extends AbstractDescribableImpl<EventType> {
    private static final Logger logger = Logger.getLogger(EventType.class.getName());
    public static final String KEY_DOCKER_IMAGE_DIGEST = WebHookPayload.PREFIX + "DOCKER_IMAGE_DIGEST";

    public boolean accepts(@Nonnull String jsonTypeCode ){
        return jsonTypeCode.equals(getType());
    }

    public void buildEnvironment(@Nonnull EnvVars envs, @Nonnull PushNotification push) {
        // Each event type can have a unique time stamp field name
        WebHookPayload payload = push.getWebHookPayload();
        if( payload != null ){
            JSONObject data = payload.getData();
            if ( data != null) {
                JSONObject contents = data.getJSONObject("contents");
                if (contents != null) {
                    DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
                    push.setPushedAt(parser.parseDateTime(getTimeStamp(contents)).toDate());

                    envs.put(EventTypeDescriptor.ENVIRONMENT_KEY, getType());
                    if (hasDigest()) {
                        envs.put(KEY_DOCKER_IMAGE_DIGEST, contents.getString("digest"));
                    }
                }
            }
        }
    }


    public abstract String getType();
    public abstract String getTimeStamp(JSONObject contents);

    public boolean hasDigest() { return true; }

    @Override
    public int hashCode() {
        int result = getType() != null ? getType().hashCode() : 0;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EventType && this.getType().equals(((EventType)obj).getType());
    }

    @Override
    public String toString() {
        return getType();
    }
}

