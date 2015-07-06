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
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.DateUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Received payload of data from the Docker Hub web hook.
 * See <a href="http://docs.docker.io/docker-hub/repos/#webhooks">Reference</a>
*/
public class WebHookPayload implements Serializable {
    private static final long serialVersionUID = 207798312860576090L;
    private static final Logger logger = Logger.getLogger(WebHookPayload.class.getName());

    private String repoName;
    @CheckForNull
    private transient JSONObject data;
    @CheckForNull
    private String json;
    private final long received;


    public WebHookPayload(@Nonnull String repoName, @CheckForNull JSONObject data) {
        this.repoName = repoName;
        this.data = data;
        if (this.data != null) {
            this.json = this.data.toString();
        }
        this.received = System.currentTimeMillis();
    }

    /**
     * Creates the object from the json payload
     *
     * @param data the json payload
     * @throws net.sf.json.JSONException if the key {@code repository.repo_name} doesn't exist.
     */
    public WebHookPayload(@Nonnull JSONObject data) {
        this.data = data;
        JSONObject repository = data.getJSONObject("repository");
        this.repoName = repository.getString("repo_name");
        this.json = data.toString();
        this.received = System.currentTimeMillis();
    }

    public WebHookPayload(@Nonnull String repoName) {
        this(repoName, null);
    }

    /**
     * String like "username/reponame"
     */
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

    /**
     * {@link System#currentTimeMillis()} when this object's constructor was called.
     *
     * @return the object's creation time/when the payload was received.
     */
    public long getReceived() {
        return received;
    }

    public String sha() {
        return Util.getDigestOf("dockerHubNotification:"+repoName+Long.toBinaryString(received));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebHookPayload)) return false;

        WebHookPayload that = (WebHookPayload)o;

        if (received != that.received) return false;
        if (!repoName.equals(that.repoName)) return false;
        return !(data != null ? !data.equals(that.data) : that.data != null);

    }

    @Override
    public int hashCode() {
        int result = repoName.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (int)(received ^ (received >>> 32));
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
                ", received=" + received +
                '}';
    }
}
