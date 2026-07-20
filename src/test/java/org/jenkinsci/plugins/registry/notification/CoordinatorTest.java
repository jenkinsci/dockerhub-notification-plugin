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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests scenarios involving {@link Coordinator}.
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@WithJenkins
class CoordinatorTest {

    private static final Response resp = new Response();

    private JenkinsRule j;

    private String token;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        final JSONObject test = ApiTokens.get().generateApiToken("test");
        token = test.getString("value");
    }

    @Test
    void testTwoTriggered() throws Exception {
        FreeStyleProject one = j.createFreeStyleProject();
        one.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(List.of("csanchez/jenkins-swarm-slave"))));
        one.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        one.setQuietPeriod(0);

        FreeStyleProject two = j.createFreeStyleProject();
        two.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(List.of("csanchez/jenkins-swarm-slave"))));
        two.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        two.setQuietPeriod(0);

        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/own-repository-payload.json"), StandardCharsets.UTF_8));
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
    void testOneTriggered() throws Exception {
        FreeStyleProject one = j.createFreeStyleProject();
        one.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(List.of("csanchez/jenkins-swarm-slave"))));
        one.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        one.setQuietPeriod(0);

        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/own-repository-payload.json"), StandardCharsets.UTF_8));
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

        public void doRespond(StaplerRequest2 req, StaplerResponse2 response) throws IOException {
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