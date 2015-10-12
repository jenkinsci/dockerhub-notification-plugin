package org.jenkinsci.plugins.registry.notification.webhook.dockerregistry;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lguminski on 07/10/15.
 */
public class DockerRegistryWebHookPayload extends WebHookPayload {

    public DockerRegistryWebHookPayload(@Nonnull String repoName, final @CheckForNull JSONObject data) {
        super();
        setData(data);
        if (data != null) {
            setJson(data.toString());
        }
        this.pushNotifications.add(createPushNotification(repoName, data));
    }

    /**
     * Creates the object from the json payload
     *
     * @param data the json payload
     * @throws net.sf.json.JSONException if the key {@code repository.repo_name} doesn't exist.
     */
    public DockerRegistryWebHookPayload(@Nonnull JSONObject data) {
        super();
        setData(data);
        if (data != null) {
            setJson(data.toString());
        }
        JSONArray events = data.getJSONArray("events");

        for (int i = 0, size = events.size(); i < size; i++) {
            JSONObject event = events.getJSONObject(i);
            String separator = "/";
            final String[] urlSegments = event.getJSONObject("target").optString("url").split(separator);
            StringBuffer sb = new StringBuffer();
            sb.append(urlSegments[2]);
            sb.append(separator);
            sb.append(event.getJSONObject("target").optString("repository"));
            String repository = sb.toString();
            pushNotifications.add(createPushNotification(repository, event));
        }

    }

    private DockerRegistryPushNotification createPushNotification(@Nonnull final String repoName, @CheckForNull final JSONObject data) {
        return new DockerRegistryPushNotification(this, repoName){{
            DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
            String timestamp = data.optString("timestamp");
            setPushedAt(parser.parseDateTime(timestamp).toDate());
            setRegistryHost(data.getJSONObject("request").optString("host"));
        }};
    }

    public DockerRegistryWebHookPayload(@Nonnull String repoName) {
        this(repoName, null);
    }


}
