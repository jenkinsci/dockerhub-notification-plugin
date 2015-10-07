package org.jenkinsci.plugins.registry.notification.webhook.dockerhub;

import hudson.model.Run;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.registry.notification.TriggerStore;
import org.jenkinsci.plugins.registry.notification.webhook.CallbackHandler;
import org.jenkinsci.plugins.registry.notification.webhook.Http;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by lguminski on 07/10/15.
 */
public class DockerHubWebHookPayload extends WebHookPayload {
    private static final Logger logger = Logger.getLogger(DockerHubWebHookPayload.class.getName());

    private Date pushedAt = null;

    public DockerHubWebHookPayload(@Nonnull String repoName, final @CheckForNull JSONObject data) {
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
    public DockerHubWebHookPayload(@Nonnull JSONObject data) {
        super();
        setData(data);
        if (data != null) {
            setJson(data.toString());
        }
        JSONObject repository = data.getJSONObject("repository");
        this.pushNotifications.add(createPushNotification(repository.getString("repo_name"), data));
    }

    private DockerHubPushNotification createPushNotification(@Nonnull final String repoName, @CheckForNull final JSONObject data) {
        final DockerHubPushNotification dockerHubPushNotification = new DockerHubPushNotification(this, repoName);
        if(data != null) {
            dockerHubPushNotification.setCallbackUrl(data.optString("callback_url"));
            JSONObject push_data = data.optJSONObject("push_data");
            if (push_data != null) {
                try {
                    long pushed_at = push_data.optLong("pushed_at");
                    if (pushed_at > 0) {
                        dockerHubPushNotification.setPushedAt(new Date(pushed_at * 1000));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            dockerHubPushNotification.setCallbackHandler(new CallbackHandler() {
                @Override
                public void notify(PushNotification pushNotification, Run<?, ?> run) throws InterruptedException, ExecutionException, IOException {
                    final String callbackUrl = dockerHubPushNotification.getCallbackUrl();
                    TriggerStore.TriggerEntry entry = TriggerStore.getInstance().finalized(dockerHubPushNotification, run);
                    DockerHubCallbackPayload callback = DockerHubCallbackPayload.from(entry);
                    if (!StringUtils.isBlank(callbackUrl)) {
                        logger.log(Level.FINE, "Sending callback to Docker Hub");
                        logger.log(Level.FINER, "Callback: {0}", callback);
                        int response = Http.post(callbackUrl, callback.toJSON());
                        logger.log(Level.FINE, "Docker Hub returned {0}", response);
                    } else {
                        logger.log(Level.WARNING, "No callback URL specified in {0}", pushNotification);
                    }
                }
            });
        }
        return dockerHubPushNotification;
    }

    public DockerHubWebHookPayload(@Nonnull String repoName) {
        this(repoName, null);
    }

}
