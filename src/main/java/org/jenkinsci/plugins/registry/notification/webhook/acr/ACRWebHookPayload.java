package org.jenkinsci.plugins.registry.notification.webhook.acr;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Partially implements https://docs.microsoft.com/en-us/azure/container-registry/container-registry-webhook-reference.
 */
public class ACRWebHookPayload extends WebHookPayload {
    private static final Logger logger = Logger.getLogger(ACRWebHookPayload.class.getName());

    public enum Action {
        PUSH("push");
        private String name;

        private Action(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public ACRWebHookPayload(@Nonnull final JSONObject data) {
        setData(data);
        if (data != null) {
            setJson(data.toString());
        }

        if (Action.PUSH.getName().equals(data.optString("action"))) {
            final JSONObject event = data;
            final String host = event.getJSONObject("request").getString("host");
            final String url = String.format("%s/%s",
                    host,
                    event.getJSONObject("target").getString("repository"));
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Creating push notification for " + url);
            }
            pushNotifications.add(new ACRPushNotification(this, url){{
                DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
                setPushedAt(parser.parseDateTime(event.getString("timestamp")).toDate());
                setRegistryHost(host);
            }});
        } else {
            logger.log(Level.FINER, "Unsupported event received: " + data.toString());
        }
    }
}
