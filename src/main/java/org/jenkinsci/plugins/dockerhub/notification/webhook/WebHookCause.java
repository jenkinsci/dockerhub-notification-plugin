package org.jenkinsci.plugins.dockerhub.notification.webhook;

/**
 * Created by lguminski on 07/10/15.
 */
public interface WebHookCause {
    DockerHubWebHookPayload getPayload();
}
