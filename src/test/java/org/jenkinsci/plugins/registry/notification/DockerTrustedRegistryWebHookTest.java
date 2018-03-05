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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.events.EventType;
import org.jenkinsci.plugins.registry.notification.events.impl.ManifestDeleted;
import org.jenkinsci.plugins.registry.notification.events.impl.ManifestPushed;
import org.jenkinsci.plugins.registry.notification.events.impl.SecurityScanCompleted;
import org.jenkinsci.plugins.registry.notification.events.impl.TagDeleted;
import org.jenkinsci.plugins.registry.notification.events.impl.TagPushed;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.webhook.Http;
import org.jenkinsci.plugins.registry.notification.webhook.WebHookCause;
import org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.DockerTrustedRegistryPushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.DockerTrustedRegistryWebHook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Testing Docker Trusted Registry webhook.
 */
@RunWith(JUnitParamsRunner.class)
public class DockerTrustedRegistryWebHookTest {
    private static final Logger logger = Logger.getLogger(DockerTrustedRegistryWebHookTest.class.getName());

    private static final Set<String> REPOS = new HashSet<String>() {{
        add("dtr.unit.test.com/foo/bar");
    }};

    private static final Map<EventType, String> jsonPayload = new HashMap<EventType,String>() {{
        put(new TagPushed(), "/docker-trusted-registry-payload-tag-push.json");
        put(new TagDeleted(), "/docker-trusted-registry-payload-tag-delete.json");
        put(new SecurityScanCompleted(), "/docker-trusted-registry-payload-scan-completed.json");
        put(new ManifestPushed(), "/docker-trusted-registry-payload-manifest-push.json");
        put(new ManifestDeleted(), "/docker-trusted-registry-payload-manifest-delete.json");
    }};
    private Set<EventType> getEventTypes(){ return jsonPayload.keySet(); }

    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Test
    @Parameters(method = "getEventTypes")
    public void testThatOnlyTagPushIsTriggeredWithDefaultEventOptions(EventType testType) throws Exception {
        List<FreeStyleProject> projects = createProjectsTriggeredByRepository(REPOS);
        simulatePushNotification(testType.equals(new TagPushed()) ? 1 : 0, jsonPayload.get(testType), REPOS, testType);
        delete(projects);
    }

    @Test
    @Parameters(method = "getEventTypes")
    public void testThatEachTypeTriggersWheneverItIsConfigured(EventType testType) throws Exception {
        for(Set<EventType> configuredTypes: allEventCombinationsWith(testType)) {
            logger.info("Testing "+testType.getType()+" against project configured with: "+ configuredTypes.toString());
            List<FreeStyleProject> projects = createProjectsTriggeredByRepository(REPOS, configuredTypes);
            simulatePushNotification(1, jsonPayload.get(testType), REPOS, testType);
            delete(projects);
        }
    }

    private List<Set<EventType>> allEventCombinationsWith(final EventType firstType){
        final Set<EventType> types = new HashSet<EventType>(){{ add(firstType); }};
        List<Set<EventType>> typeList = new ArrayList<Set<EventType>>(){{ add(new HashSet<EventType>(){{addAll(types);}}); }};

        Set<EventType> otherTypes = new HashSet<EventType>();
        for ( EventType t : jsonPayload.keySet() ) {
            if(!t.equals(firstType)){
                otherTypes.add(t);
            }

        }
        for( EventType otherType: otherTypes){
            types.add(otherType);
            typeList.add(new HashSet<EventType>(){{addAll(types);}});
        }

        return typeList;
    }

    private void delete(List<FreeStyleProject> projects) throws IOException, InterruptedException {
        for(FreeStyleProject p : projects){
            p.delete();
        }
    }

    private List<FreeStyleProject> createProjectsTriggeredByRepository(Set<String> repositories) throws Exception {
       return createProjectsTriggeredByRepository(repositories, new HashSet<EventType>());
    }

    private List <FreeStyleProject> createProjectsTriggeredByRepository(Set<String> repositories, final Set<EventType> eventTypes) throws Exception {
        List <FreeStyleProject> projects = new ArrayList<FreeStyleProject>();
        for (final String repository : repositories) {
            FreeStyleProject project = j.createFreeStyleProject();
            DockerHubTrigger dht = new DockerHubTrigger(new TriggerOnSpecifiedImageNames(new ArrayList<String>() {{
                add(repository);
            }}));
            dht.setEventTypes(new ArrayList<EventType>(){{ addAll(eventTypes); }});
            project.addTrigger(dht);
            project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
            projects.add(project);
        }
        return projects;
    }

    private void simulatePushNotification(int expectedHits, String payloadResource, Set<String> repositories, EventType eventType) throws Exception {
        int expectedRepoSize = repositories.size() - expectedHits;
        ExtensionList<PushNotificationRunListener> extensionList = j.jenkins.getExtensionList(PushNotificationRunListener.class);
        PushNotificationRunListener pushNotificationRunListener = extensionList.get(0);
        pushNotificationRunListener.setHitCounter(0);
        pushNotificationRunListener.setExpectedRepos(repositories);
        pushNotificationRunListener.setExpectedEventType(eventType);
        assertThat(pushNotificationRunListener.getExpectedRepos(), hasSize(repositories.size()));
        JSONObject json = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream(payloadResource)));
        String url = j.getURL() + DockerTrustedRegistryWebHook.URL_NAME + "/notify";
        assertEquals(200, Http.post(url, json));
        j.waitUntilNoActivity();
        String msg = "Simulated event: " + eventType.getType() + " against: " + payloadResource;
        assertThat(msg,pushNotificationRunListener.getExpectedRepos(), hasSize(expectedRepoSize));
        assertEquals(msg,expectedHits, pushNotificationRunListener.getHitCounter());
    }


    @TestExtension
    public static class PushNotificationRunListener extends RunListener<Run<?,?>> {
        private Set<String> expectedRepos;
        private EventType expectedEventType;
        private int hitCounter;

        @Override
        public void onFinalized(Run<?, ?> run) {
            super.onFinalized(run);
            Cause cause = run.getCauses().get(0);
            if (cause instanceof WebHookCause) {
                DockerTrustedRegistryPushNotification pn = (DockerTrustedRegistryPushNotification) ((WebHookCause) cause).getPushNotification();
                String eventType = pn.getDtrEventJSONType();
                if( expectedEventType.accepts(eventType) ) {
                    hitCounter++;
                    String repoName = ((WebHookCause) cause).getPushNotification().getRepoName();
                    expectedRepos.remove(repoName);
                return;
                }
            }
            throw new RuntimeException(new IllegalAccessException("Unexpected cause: " + cause.getShortDescription()));
        }

        public Set<String> getExpectedRepos() {
            return expectedRepos;
        }

        public void setExpectedRepos(final Set<String> expectedRepos) {
            this.expectedRepos = new HashSet<String>(){{ addAll(expectedRepos); }};
        }

        public void setExpectedEventType(EventType expectedEventType) {
            this.expectedEventType = expectedEventType;
        }

        public int getHitCounter() {
            return hitCounter;
        }

        public void setHitCounter(Integer hitCounter) {
            this.hitCounter = hitCounter;
        }
    }
}
