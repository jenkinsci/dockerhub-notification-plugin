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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.Run;

import java.util.Collections;
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
        if (o == null || getClass() != o.getClass()) return false;

        PushNotification that = (PushNotification) o;

        if (webHookPayload != null ? !webHookPayload.equals(that.webHookPayload) : that.webHookPayload != null)
            return false;
        if (repoName != null ? !repoName.equals(that.repoName) : that.repoName != null) return false;
        return !(pushedAt != null ? !pushedAt.equals(that.pushedAt) : that.pushedAt != null);

    }

    @Override
    public int hashCode() {
        int result = webHookPayload != null ? webHookPayload.hashCode() : 0;
        result = 31 * result + (repoName != null ? repoName.hashCode() : 0);
        result = 31 * result + (pushedAt != null ? pushedAt.hashCode() : 0);
        return result;
    }

    abstract public Cause getCause();

    /**
     * Provide parameters to be put into a build.
     * @return the parameters
     * @deprecated misspelled and wrong context naming. Use {@link #getRunParameters()}
     */
    @Deprecated
    public Set<ParameterValue> getJobParamerers() {
        if (Util.isOverridden(PushNotification.class, getClass(), "getRunParameters")) {
            return getRunParameters();
        }
        return Collections.emptySet();
    }

    public Set<ParameterValue> getRunParameters() {
        if (Util.isOverridden(PushNotification.class, getClass(), "getJobParamerers")) {
            return getJobParamerers();
        }
        return Collections.emptySet();
    }

    abstract public String getCauseMessage();

    /**
     * String like "username/reponame"
     * @return the name of the repo
     */
    public String getRepoName() {
        return repoName;
    }

    abstract public String sha();

    @CheckForNull
    public Date getPushedAt() {
        return new Date(this.pushedAt.getTime());
    }

    public void setPushedAt(Date pushedAt) {
        this.pushedAt = new Date(pushedAt.getTime());
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
