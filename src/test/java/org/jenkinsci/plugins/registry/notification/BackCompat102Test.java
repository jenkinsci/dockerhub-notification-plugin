package org.jenkinsci.plugins.registry.notification;

import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.RunList;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerForAllUsedInJob;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubCallbackPayload;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHookCause;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing that stored data from version 1.0.2 of the plugin can be loaded from disk in newer versions.
 */
@WithJenkins
class BackCompat102Test {

    @Test
    @LocalData
    void testTriggerConfig(JenkinsRule j) {
        FreeStyleProject job = j.jenkins.getItemByFullName("OneConfigured", FreeStyleProject.class);
        assertNotNull(job, "The job should be loaded");
        DockerHubTrigger trigger = DockerHubTrigger.getTrigger(job);
        assertNotNull(trigger, "The trigger config of the job was not loaded");
        assertThat(trigger.getAllRepoNames(), hasItem(equalTo("rsandell/test")));
        assertThat(trigger.getOptions(), containsInAnyOrder(
                allOf(
                        instanceOf(TriggerOnSpecifiedImageNames.class),
                        hasProperty("repoNames", hasItem(equalTo("rsandell/test")))
                     ),
                instanceOf(TriggerForAllUsedInJob.class)));
    }

    @Test
    @LocalData
    void testPullImageBuilder(JenkinsRule j) {
        FreeStyleProject job = j.jenkins.getItemByFullName("OneConfigured", FreeStyleProject.class);
        assertNotNull(job, "The job should be loaded");
        DockerPullImageBuilder builder = job.getBuildersList().get(DockerPullImageBuilder.class);
        assertNotNull(builder, "Builder should be loaded");
        assertEquals("rsandell/test", builder.getImage());
        DockerRegistryEndpoint registry = builder.getRegistry();
        assertNotNull(registry, "Registru should be loaded");
        assertEquals("http://hub.rsandell.com", registry.getUrl());
    }

    @Test
    @LocalData
    void testBuilds(JenkinsRule j) {
        FreeStyleProject job = j.jenkins.getItemByFullName("JenkinsSlaveTrigger", FreeStyleProject.class);
        assertNotNull(job, "The job should be loaded");

        RunList<FreeStyleBuild> builds = job.getBuilds();
        FreeStyleBuild two = builds.getLastBuild();
        assertNotNull(two);
        FreeStyleBuild one = two.getPreviousBuild();
        assertNotNull(one);

        assertSame(Result.FAILURE, one.getResult(), "First build should be failure");
        assertSame(Result.SUCCESS, two.getResult(), "Second build should be success");

        DockerHubWebHookCause cause = two.getCause(DockerHubWebHookCause.class);
        assertNotNull(cause, "The cause should be loaded");
        PushNotification notification = cause.getPushNotification();
        assertNotNull(notification, "The cause should have a notification");
        assertEquals("csanchez/jenkins-swarm-slave", notification.getRepoName());
        assertEquals("registry.hub.example.com", notification.getRegistryHost());
    }

    @Test
    @LocalData
    void testFingerprintDb(JenkinsRule j) throws IOException, InterruptedException {
        TriggerStore.TriggerEntry entry = TriggerStore.getInstance().getEntry("e9d6eb6cd6a7bfcd2bd622765a87893f");
        assertNotNull(entry, "TriggerEntry should be loaded in the fingerprint db");
        assertTrue(entry.areAllDone());
        List<TriggerStore.TriggerEntry.RunEntry> runEntries = entry.getEntries();
        assertNotNull(runEntries);
        assertEquals(1, runEntries.size());
        TriggerStore.TriggerEntry.RunEntry run = runEntries.get(0);
        assertEquals("JenkinsSlaveTrigger", run.getJobName());
        assertNotNull(run.getRun(), "The run should be retrievable");
        PushNotification notification = entry.getPushNotification();
        assertNotNull(notification, "The entry should have a notification");
        assertEquals("csanchez/jenkins-swarm-slave", notification.getRepoName());
        assertEquals("registry.hub.example.com", notification.getRegistryHost());

        DockerHubCallbackPayload data = entry.getCallbackData();
        assertNotNull(data, "There should be stored callback data");
        assertSame(DockerHubCallbackPayload.States.success, data.getState());
        assertEquals("dockerhub-webhook/details/e9d6eb6cd6a7bfcd2bd622765a87893f", data.getTargetUrl());
    }

    @Test
    @LocalData
    void testFingerprintUi(JenkinsRule j) throws IOException, SAXException {
        JenkinsRule.WebClient web = j.createWebClient();
        HtmlPage page = web.goTo("dockerhub-webhook/details/e9d6eb6cd6a7bfcd2bd622765a87893f");
        j.assertStringContains(page.asNormalizedText(), "Build results for push of csanchez/jenkins-swarm-slave");
    }
}
