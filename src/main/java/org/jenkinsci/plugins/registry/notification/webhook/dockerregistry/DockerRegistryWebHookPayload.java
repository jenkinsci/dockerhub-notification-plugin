package org.jenkinsci.plugins.registry.notification.webhook.dockerregistry;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubPushNotification;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Date;

/**
 * Created by lguminski on 07/10/15.
 */
public class DockerRegistryWebHookPayload extends WebHookPayload {

    private Date pushedAt = null;

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
        JSONObject repository = data.getJSONObject("repository");
        this.pushNotifications.add(createPushNotification(repository.getString("repo_name"), data));
    }

    private DockerRegistryPushNotification createPushNotification(@Nonnull final String repoName, @CheckForNull final JSONObject data) {
        return new DockerRegistryPushNotification(this, repoName){{
            if(data != null) {
                setCallbackUrl(data.optString("callback_url"));
                JSONObject push_data = data.optJSONObject("push_data");
                if (push_data != null) {
                    try {
                        long pushed_at = push_data.optLong("pushed_at");
                        if (pushed_at > 0) {
                            setPushedAt(new Date(pushed_at * 1000));
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }};
    }

    public DockerRegistryWebHookPayload(@Nonnull String repoName) {
        this(repoName, null);
    }

}
