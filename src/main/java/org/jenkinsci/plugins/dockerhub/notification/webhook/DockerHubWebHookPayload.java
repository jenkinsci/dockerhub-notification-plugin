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

import hudson.Util;
import hudson.model.Cause;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.dockerhub.notification.DockerHubWebHookCause;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Received payload of data from the Docker Hub web hook.
 * See <a href="http://docs.docker.io/docker-hub/repos/#webhooks">Reference</a>
*/
public class DockerHubWebHookPayload extends WebHookPayload {
    private static final long serialVersionUID = 207798312860576090L;
    public static final String KEY_REPO_NAME = PREFIX + "REPO_NAME";
    public static final String KEY_DOCKER_HUB_HOST = PREFIX + "DOCKER_HUB_HOST";
    private static final Logger logger = Logger.getLogger(DockerHubWebHookPayload.class.getName());

    private String repoName;
    @CheckForNull
    private transient JSONObject data;
    @CheckForNull
    private String json;


    public DockerHubWebHookPayload(@Nonnull String repoName, @CheckForNull JSONObject data) {
        super();
        this.repoName = repoName;
        this.data = data;
        if (this.data != null) {
            this.json = this.data.toString();
        }
    }

    /**
     * Creates the object from the json payload
     *
     * @param data the json payload
     * @throws net.sf.json.JSONException if the key {@code repository.repo_name} doesn't exist.
     */
    public DockerHubWebHookPayload(@Nonnull JSONObject data) {
        super();
        this.data = data;
        JSONObject repository = data.getJSONObject("repository");
        this.repoName = repository.getString("repo_name");
        this.json = data.toString();
    }

    public DockerHubWebHookPayload(@Nonnull String repoName) {
        this(repoName, null);
    }

    /**
     * String like "username/reponame"
     */
    @Override
    public String getRepoName() {
        return repoName;
    }

    @CheckForNull
    public JSONObject getData() {
        return data;
    }

    @CheckForNull
    public String getCallbackUrl() {
        return data != null ? data.optString("callback_url") : null;
    }

    @CheckForNull
    public String getCallbackHost() {
        String urlS = getCallbackUrl();
        if (urlS != null) {
            try {
                URL url = new URL(urlS);
                return url.getHost();
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "DockerHub is sending malformed data. ", e);
            }
        }
        return null;
    }

    @CheckForNull
    public Date getPushedAt() {
        JSONObject data = getData();
        if (data != null) {
            JSONObject push_data = data.optJSONObject("push_data");
            if (push_data != null) {
                try {
                    long pushed_at = push_data.optLong("pushed_at");
                    if (pushed_at > 0) {
                        return new Date(pushed_at * 1000);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public String sha() {
        return Util.getDigestOf("dockerHubNotification:"+repoName+Long.toBinaryString(getReceived()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DockerHubWebHookPayload)) return false;

        DockerHubWebHookPayload that = (DockerHubWebHookPayload)o;

        if (getReceived() != that.getReceived()) return false;
        if (!repoName.equals(that.repoName)) return false;
        return !(data != null ? !data.equals(that.data) : that.data != null);

    }

    @Override
    public int hashCode() {
        int result = repoName.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (int)(getReceived() ^ (getReceived() >>> 32));
        return result;
    }

    private Object readResolve() {
        if (this.data == null && this.json != null) {
            this.data = JSONObject.fromObject(this.json);
        }
        return this;
    }

    @Override
    public String toString() {
        return "WebHookPayload{" +
                "repoName='" + repoName + '\'' +
                ", data=" + (data == null ? "<null>" : "<json>") +
                ", received=" + getReceived() +
                '}';
    }

    @Override
    public Cause getCause() {
        return new DockerHubWebHookCause(this);
    }

    @Override
    public Set<ParameterValue> getJobParamerers() {
        Set<ParameterValue> parameters = new HashSet<ParameterValue>();
        parameters.add(new StringParameterValue(KEY_REPO_NAME, getRepoName()));
        String host = getCallbackHost();
        if (!StringUtils.isBlank(host)) {
            parameters.add(new StringParameterValue(KEY_DOCKER_HUB_HOST, host));
        }
        return parameters;
    }

    @Override
    public String getCauseMessage() {
        return "Docker image " + getRepoName() + " has been rebuilt by DockerHub@" + getCallbackHost();
    }
}
