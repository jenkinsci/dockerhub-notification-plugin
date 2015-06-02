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
package org.jenkinsci.plugins.dockerhub.notification;


import org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.dockerhub.notification.webhook.Http;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.UnprotectedRootAction;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests scenarios involving {@link Coordinator}.
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class CoordinatorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    private static final Response resp = new Response();

    @Test
    public void testTwoTriggered() throws Exception {
        j.jenkins.setCrumbIssuer(null);
        FreeStyleProject one = j.createFreeStyleProject();
        one.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(Arrays.asList("csanchez/jenkins-swarm-slave"))));
        one.getBuildersList().add(new MockBuilder(Result.SUCCESS));

        FreeStyleProject two = j.createFreeStyleProject();
        two.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(Arrays.asList("csanchez/jenkins-swarm-slave"))));
        two.getBuildersList().add(new MockBuilder(Result.SUCCESS));

        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/own-repository-payload.json")));
        json.put("callback_url", j.getURL() + "fake-dockerhub/respond");

        String url = j.getURL() + DockerHubWebHook.URL_NAME + "/notify";
        assertEquals(302, Http.post(url, json));
        synchronized (resp) {
            resp.wait();
        }
        JSONObject callback = resp.json;
        assertNotNull(callback);
        //{"state":"success","description":"Build result SUCCESS","context":"Jenkins","target_url":"http://localhost:49951/jenkins/dockerhub-webhook/details/b23e9f77fb91cb92a873488eb18e5b1001cfe4d0"}
        assertEquals("success", callback.getString("state"));
        String target_url = callback.getString("target_url");
        assertThat(target_url, containsString("/dockerhub-webhook/details/"));
        String sha = target_url.substring(target_url.lastIndexOf('/')+1);
        TriggerStore.TriggerEntry entry = TriggerStore.getInstance().getEntry(sha);
        assertNotNull(entry);
        assertThat(entry.getEntries(), allOf(
                hasSize(2),
                containsInAnyOrder(
                        allOf(
                                hasProperty("job", sameInstance(one)),
                                hasProperty("run", sameInstance(one.getLastBuild()))
                        ),
                        allOf(
                                hasProperty("job", sameInstance(two)),
                                hasProperty("run", sameInstance(two.getLastBuild()))
                        )
                )
        ));
    }

    @Test
    public void testOneTriggered() throws Exception {
        j.jenkins.setCrumbIssuer(null);
        FreeStyleProject one = j.createFreeStyleProject();
        one.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(Arrays.asList("csanchez/jenkins-swarm-slave"))));
        one.getBuildersList().add(new MockBuilder(Result.SUCCESS));

        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/own-repository-payload.json")));
        json.put("callback_url", j.getURL() + "fake-dockerhub/respond");

        String url = j.getURL() + DockerHubWebHook.URL_NAME + "/notify";
        assertEquals(302, Http.post(url, json));
        synchronized (resp) {
            resp.wait();
        }
        JSONObject callback = resp.json;
        assertNotNull(callback);
        assertEquals("success", callback.getString("state"));
        String target_url = callback.getString("target_url");
        assertNotNull(target_url);
        assertThat(target_url, containsString("/dockerhub-webhook/details/"));
        String sha = target_url.substring(target_url.lastIndexOf('/')+1);
        TriggerStore.TriggerEntry entry = TriggerStore.getInstance().getEntry(sha);
        assertNotNull(entry);
        assertThat(entry.getEntries(), allOf(
                hasSize(1),
                contains(
                        allOf(
                                hasProperty("job", sameInstance(one)),
                                hasProperty("run", sameInstance(one.getLastBuild()))
                        )
                )
        ));
    }

    @TestExtension
    public static class CallbackEndpoint implements UnprotectedRootAction {

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Fake DockerHub";
        }

        @Override
        public String getUrlName() {
            return "fake-dockerhub";
        }

        public void doRespond(StaplerRequest req, StaplerResponse response) throws IOException {
            String body = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            JSONObject json = JSONObject.fromObject(body);
            synchronized (resp) {
                resp.json = json;
                resp.notifyAll();
            }
        }
    }

    static class Response {
        JSONObject json;
    }
}