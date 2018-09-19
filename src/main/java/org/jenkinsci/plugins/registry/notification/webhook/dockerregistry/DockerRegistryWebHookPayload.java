/**
 * The MIT License
 *
 * Copyright (c) 2015, HolidayCheck AG.
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
package org.jenkinsci.plugins.registry.notification.webhook.dockerregistry;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DockerRegistryWebHookPayload extends WebHookPayload {

    private static final Logger logger = Logger.getLogger(DockerRegistryPushNotification.class.getName());
    
    /**
     * Creates the object from the json payload
     *
     * @param data the json payload
     * @throws net.sf.json.JSONException if the key {@code repository.repo_name} doesn't exist.
     */
    public DockerRegistryWebHookPayload(@Nonnull JSONObject data) {
        super();
        setData(data);
        setJson(data.toString());
        JSONArray events = data.getJSONArray("events");

        for (int i = 0, size = events.size(); i < size; i++) {
            JSONObject event = events.getJSONObject(i);
            String separator = "/";

            if ( event.optString("action").equals("push") ) {
                final String[] urlSegments = event.getJSONObject("target").optString("url").split(separator);
                StringBuffer sb = new StringBuffer();
                sb.append(urlSegments[2]);
                sb.append(separator);
                sb.append(event.getJSONObject("target").optString("repository"));
                String repository = sb.toString();
                if (urlSegments[urlSegments.length - 2 ].equals("manifests")) {
                    pushNotifications.add(createPushNotification(repository, event));
                } else {
                    logger.log(Level.FINER, "Skipping Layer Push notifications");

                }
            } else {
                logger.log(Level.FINER, "Skipping pull notification" + event.getJSONObject("target").optString("repository"));
            }
        }

    }

    private DockerRegistryPushNotification createPushNotification(@Nonnull final String repoName, @Nonnull JSONObject data) {
        final String timestamp = data.optString("timestamp");
        final String host = data.getJSONObject("request").optString("host");
        return new DockerRegistryPushNotification(this, repoName){{
            DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
            setPushedAt(parser.parseDateTime(timestamp).toDate());
            setRegistryHost(host);
        }};
    }
}
