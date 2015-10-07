package org.jenkinsci.plugins.registry.notification.webhook;

import net.sf.json.JSONObject;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lguminski on 06/10/15.
 */
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
