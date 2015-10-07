package org.jenkinsci.plugins.registry.notification.webhook;

import hudson.model.Run;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by lguminski on 07/10/15.
 */
public interface CallbackHandler {
    void notify(PushNotification pushNotification, Run<?, ?> run) throws InterruptedException, ExecutionException, IOException;
}
