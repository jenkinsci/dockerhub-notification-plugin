/**
 * The MIT License
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.registry.notification.webhook;

import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.Run;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubPushNotification;

import javax.annotation.CheckForNull;
import java.util.Date;
import java.util.Set;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerHubPushNotification)) return false;

        DockerHubPushNotification that = (DockerHubPushNotification)o;

        if (!getRepoName().equals(that.repoName)) return false;
        return getWebHookPayload().equals(that.getWebHookPayload());
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
