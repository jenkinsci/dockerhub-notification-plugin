/**
 * The MIT License
 * <p>
 * Copyright (c) 2015, HolidayCheck AG.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.registry.notification.webhook.nexus;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookPayload;
import org.jenkinsci.plugins.registry.notification.webhook.dockerregistry.DockerRegistryPushNotification;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NexusDockerRegistryWebHookPayload extends WebHookPayload {

    private static final Logger logger = Logger.getLogger(DockerRegistryPushNotification.class.getName());

    /**
     * Creates the object from the json payload
     *
     * @param data the json payload
     * @param host
     */
    public NexusDockerRegistryWebHookPayload(@Nonnull JSONObject data, final String host) {
        super();
        setData(data);
        setJson(data.toString());

        if (StringUtils.isEmpty(host)) {
            logger.log(Level.WARNING, "Dropping nexus docker notify as host param is missing, please add ?host=<yourdockerregistryhost> to the webhook config");
        } else {
            String action = data.getString("action");
            if ("CREATED".equals(action) || "UPDATED".equals(action)) {
                JSONObject asset = data.getJSONObject("asset");
                if ("docker".equals(asset.getString("format"))) {
                    String separator = "/";
                    final String[] urlSegments = asset.getString("name").split(separator);
                    if (urlSegments[urlSegments.length - 2].equals("manifests") && !urlSegments[urlSegments.length - 1].startsWith("sha")) {
                        StringBuilder sb = new StringBuilder(host);
                        for (int i = 1; i < urlSegments.length - 2; i++) {
                            sb.append(separator);
                            sb.append(urlSegments[i]);
                        }
                        String repository = sb.toString();
                        logger.log(Level.FINER, "Notify for " + repository);
                        final String timestamp = data.optString("timestamp");
                        pushNotifications.add(new NexusDockerRegistryPushNotification(this, repository, urlSegments[urlSegments.length - 1]) {{
                            DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
                            setPushedAt(parser.parseDateTime(timestamp).toDate());
                            setRegistryHost(host);
                        }});
                    } else {
                        logger.log(Level.FINER, "Skipping Layer Push notifications");
                    }
                } else {
                    logger.log(Level.FINER, "Skipping non docker notify");
                }
            } else {
                logger.log(Level.FINER, "Skipping " + action + " action");
            }
        }
    }
}
