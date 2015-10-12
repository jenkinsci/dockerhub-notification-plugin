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

import net.sf.json.JSONObject;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class WebHookPayload implements Serializable {

    public static final String PREFIX = "DOCKER_TRIGGER_";

    @CheckForNull
    private transient JSONObject data;
    @CheckForNull
    private String json;

    protected final long received;

    protected List<PushNotification> pushNotifications = new ArrayList<PushNotification>();

    public WebHookPayload() {
        this.received = System.currentTimeMillis();
    }

    /**
     * {@link System#currentTimeMillis()} when this object's constructor was called.
     *
     * @return the object's creation time/when the payload was received.
     */
    public long getReceived() {
        return received;
    }

    public List<PushNotification> getPushNotifications() {
        return Collections.unmodifiableList(pushNotifications);
    }

    @CheckForNull
    public JSONObject getData() {
        return data;
    }

    protected void setData(JSONObject data) {
        this.data = data;
    }

    protected void setJson(String json) {
        this.json = json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebHookPayload that = (WebHookPayload) o;

        if (received != that.received) return false;
        return !(data != null ? !data.equals(that.data) : that.data != null);

    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (int) (received ^ (received >>> 32));
        return result;
    }

    private Object readResolve() {
        if (this.data == null && this.json != null) {
            this.data = JSONObject.fromObject(this.json);
        }
        return this;
    }
}
