package org.jenkinsci.plugins.registry.notification.events;

import net.sf.json.JSONObject;

public interface EventTypePayloadFields {
    public String getType(JSONObject contents);
    public String getTimeStamp(JSONObject contents);
    public String getHost(JSONObject contents);
    public String getTag(JSONObject contents);
    public String getDigest(JSONObject contents);
}
