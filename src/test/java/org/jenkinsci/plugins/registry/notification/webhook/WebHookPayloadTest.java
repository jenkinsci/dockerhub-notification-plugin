/**
 * The MIT License
 * <p>
 * Copyright (c) 2015, CloudBees, Inc.
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
package org.jenkinsci.plugins.registry.notification.webhook;

import hudson.util.XStream2;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHookPayload;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

class WebHookPayloadTest {

    @Test
    void testSerialization() {
        JSONObject json = new JSONObject();
        JSONObject repository = new JSONObject();
        repository.put("repo_name", "cb/jenkins");
        json.put("repository", repository);
        WebHookPayload obj = new DockerHubWebHookPayload(json);

        XStream2 xs = new XStream2();
        xs.allowTypes(new Class[] {DockerHubWebHookPayload.class});
        String xml = xs.toXML(obj);
        assertThat(xml, not(containsString("<data>")));

        DockerHubWebHookPayload nObj = (DockerHubWebHookPayload)xs.fromXML(xml);
        assertNotNull(nObj.getData());
        assertNotNull(nObj.getData().get("repository"));
    }
}
