package org.jenkinsci.plugins.registry.notification.webhook;

import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.Set;

/**
 * Created by lguminski on 07/10/15.
 */
public abstract class PushNotification {

    private final WebHookPayload webHookPayload;

    protected String repoName;
    private Date pushedAt;

    CallbackHandler callbackHandler = new CallbackHandler() {
        @Override
        public void notify(PushNotification pushNotification, Run<?, ?> run) {
        }
    };

    public PushNotification(WebHookPayload webHookPayload) {
        this.webHookPayload = webHookPayload;
    }

    abstract public Cause getCause();

    abstract public Set<ParameterValue> getJobParamerers();

    abstract public String getCauseMessage();

    /**
     * String like "username/reponame"
     */
    public String getRepoName() {
        return repoName;
    }

    abstract public String sha();

    @CheckForNull
    public Date getPushedAt() {
        return this.pushedAt;
    }

    public void setPushedAt(Date pushedAt) {
        this.pushedAt = pushedAt;
    }

    public WebHookPayload getWebHookPayload() {
        return webHookPayload;
    }

    public long getReceived() {
        return webHookPayload.getReceived();
    }

    abstract public String getShortDescription();

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    abstract public String getRegistryHost();
}
