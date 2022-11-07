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
package org.jenkinsci.plugins.registry.notification;


import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.token.ApiTokens;
import org.jenkinsci.plugins.registry.notification.webhook.Http;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHook;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests scenarios involving {@link Coordinator}.
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class CoordinatorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    private static final Response resp = new Response();

    private String token;

    @Before
    public void setUp() {
        final JSONObject test = ApiTokens.get().generateApiToken("test");
        token = test.getString("value");
    }

    @Test
    public void testTwoTriggered() throws Exception {
        FreeStyleProject one = j.createFreeStyleProject();
        one.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(Arrays.asList("csanchez/jenkins-swarm-slave"))));
        one.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        one.setQuietPeriod(0);

        FreeStyleProject two = j.createFreeStyleProject();
        two.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(Arrays.asList("csanchez/jenkins-swarm-slave"))));
        two.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        two.setQuietPeriod(0);

        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/own-repository-payload.json")));
        json.put("callback_url", j.getURL() + "fake-dockerhub/respond");

        String url = j.getURL() + DockerHubWebHook.URL_NAME + "/" + token + "/notify";
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

        // the TriggerStore store is updated when the job runs, at the moment all we have guaranteed is a trigger, so we need to wait.
        j.waitUntilNoActivity();

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
        FreeStyleProject one = j.createFreeStyleProject();
        one.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(Arrays.asList("csanchez/jenkins-swarm-slave"))));
        one.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        one.setQuietPeriod(0);

        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/own-repository-payload.json")));
        json.put("callback_url", j.getURL() + "fake-dockerhub/respond");

        String url = j.getURL() + DockerHubWebHook.URL_NAME + "/" + token + "/notify";
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

        // the TriggerStore store is provided when the job runs, at the moment all we have guaranteed is a trigger, so we need to wait.
        j.waitUntilNoActivity();

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

    @TestExtension
    public static class CallbackEndpointCrumbExclusion extends CrumbExclusion {

        @Override
        public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && (pathInfo.startsWith("/fake-dockerhub"))) {
                chain.doFilter(request, response);
                return true;
            }
            return false;
        }
    }
}