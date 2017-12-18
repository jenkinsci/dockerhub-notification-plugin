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
package org.jenkinsci.plugins.registry.notification;


import hudson.ExtensionList;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.webhook.Http;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookCause;
import org.jenkinsci.plugins.registry.notification.webhook.dockerregistry.DockerRegistryWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.dockerregistry.DockerTrustedRegistryWebHook;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Testing Docker Trusted Registry webhook.
 */
public class DockerTrustedRegistryWebHookTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testTagPush() throws Exception {
        HashSet<String> repositories = new HashSet<String>() {{
            add("dtr.unit.test.com/foo/bar");
        }};
        createProjectsTriggeredByRepository(repositories);
        simulatePushNotification(1, "/docker-trusted-registry-payload-tag-push.json", repositories);
    }

    @Ignore
    @Test
    public void testPullIgnore() throws Exception {
        HashSet<String> repositories = new HashSet<String>() {{
            add("registry:5000/jplock/zookeeper");
        }};
        createProjectsTriggeredByRepository(repositories);
        simulatePushNotification(1, "/private-registry-payload-pull-1-repository.json", repositories);
    }

    @Ignore
    @Test
    public void testBlobIgnore() throws Exception {
        HashSet<String> repositories = new HashSet<String>() {{
                add("registry:5000/jplock/zookeeper");
            }};
        createProjectsTriggeredByRepository(repositories);
        simulatePushNotification(1, "/private-registry-payload-blob-1-repository.json", repositories);
    }


    private void createProjectsTriggeredByRepository(Set<String> repositories) throws Exception {
        for (final String repository : repositories) {
            FreeStyleProject project = j.createFreeStyleProject();
            project.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(new ArrayList<String>() {{
                add(repository);
            }})));
            project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        }
    }

    private void simulatePushNotification(Integer expectedHits, String payloadResource, Set<String> repositories) throws Exception {
        ExtensionList<PushNotificationRunListener> extensionList = j.jenkins.getExtensionList(PushNotificationRunListener.class);
        PushNotificationRunListener pushNotificationRunListener = extensionList.get(0);
        pushNotificationRunListener.setHitCounter(0);
        pushNotificationRunListener.setExpectedCauses(repositories);
        assertThat(pushNotificationRunListener.getExpectedCauses(), hasSize(repositories.size()));
        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream(payloadResource)));
        String url = j.getURL() + DockerTrustedRegistryWebHook.URL_NAME + "/notify";
        assertEquals(200, Http.post(url, json));
        j.waitUntilNoActivity();
        assertThat(pushNotificationRunListener.getExpectedCauses(), hasSize(0));
        assertEquals(expectedHits, pushNotificationRunListener.getHitCounter());
    }


    @TestExtension
    public static class PushNotificationRunListener extends RunListener<Run<?,?>> {
        private Set<String> expectedCauses;
        private Integer hitCounter;

        @Override
        public void onFinalized(Run<?, ?> run) {
            super.onFinalized(run);
            hitCounter++;
            Cause cause = run.getCauses().get(0);
            if (cause instanceof WebHookCause) {
                String repoName = ((WebHookCause) cause).getPushNotification().getRepoName();
                expectedCauses.remove(repoName);
                return;
            }
            throw new RuntimeException(new IllegalAccessException("Unexpected cause: " + cause.getShortDescription()));
        }

        public Set<String> getExpectedCauses() {
            return expectedCauses;
        }

        public void setExpectedCauses(Set<String> expectedCauses) {
            this.expectedCauses = expectedCauses;
        }

        public Integer getHitCounter() {
            return hitCounter;
        }

        public void setHitCounter(Integer hitCounter) {
            this.hitCounter = hitCounter;
        }
    }
}
