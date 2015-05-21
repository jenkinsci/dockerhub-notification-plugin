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
package org.jenkinsci.plugins.dockerhub.notification.webhook;

import org.jenkinsci.plugins.dockerhub.notification.DockerHubWebHook;
import org.jenkinsci.plugins.dockerhub.notification.TriggerStore;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data to send as callback to a Docker Hub web hook.
 * See <a href="http://docs.docker.io/docker-hub/repos/#callback-json-data">Reference</a>
 */
public class CallbackPayload implements Serializable {
    private static final Logger logger = Logger.getLogger(CallbackPayload.class.getName());
    private static final long serialVersionUID = 7820793734732971287L;
    private final States state;
    private final String description;
    private final String context;
    private final String targetUrl;

    public CallbackPayload(States state, String description, String context, String targetUrl) {
        this.state = state;
        this.description = description;
        this.context = context;
        this.targetUrl = targetUrl;
    }

    public static CallbackPayload from(@Nonnull TriggerStore.TriggerEntry from) {
        Result finalRes = null;
        for (TriggerStore.TriggerEntry.RunEntry entry : from.getEntries()) {
            Run<?, ?> run = entry.getRun();
            if (run != null) {
                Result res = run.getResult();
                if (finalRes == null) {
                    finalRes = res;
                } else if (res.isWorseThan(finalRes)) {
                    finalRes = res;
                }
            }
        }
        if (finalRes != null) {
            return new CallbackPayload(
                    States.from(finalRes),
                    "Build result " + finalRes.toString(),
                    "Jenkins",
                    constructUrl(from.getEntries(), from.getPayload()));
        } else {
            return null;
        }
    }

    private static String constructUrl(@Nonnull List<TriggerStore.TriggerEntry.RunEntry> entries, @Nonnull WebHookPayload payload) {
        Jenkins jenkins = Jenkins.getInstance();
        StringBuilder str = new StringBuilder();
        if (jenkins != null) {
            String rootUrl = jenkins.getRootUrl();
            if (rootUrl != null) {
                str.append(rootUrl);
            } else {
                logger.warning("Jenkins root URL is not configured!");
            }
        }
        return str.append(DockerHubWebHook.URL_NAME).append("/details/").append(payload.sha()).toString();
    }

    public JSONObject toJSON() {
        JSONObject j = new JSONObject();
        j.put("state", state.name());
        j.put("description", description);
        j.put("context", context);
        j.put("target_url", targetUrl);
        return j;
    }

    public States getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public String getContext() {
        return context;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    public String toString() {
        return "CallbackPayload{" +
                "state=" + state +
                ", description='" + description + '\'' +
                ", context='" + context + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                '}';
    }

    public static enum States {
        success, failure, error;

        public static States from(@Nonnull Result result) {
            if (result == Result.SUCCESS) {
                return success;
            }
            if (result == Result.FAILURE) {
                return error;
            }
            if (result == Result.UNSTABLE) {
                return failure;
            }
            return error;
        }
    }
}
