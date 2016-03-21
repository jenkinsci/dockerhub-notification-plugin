package org.jenkinsci.plugins.dockerhub.notification;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.RunList;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerForAllUsedInJob;
import org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.dockerhub.notification.webhook.CallbackPayload;
import org.jenkinsci.plugins.dockerhub.notification.webhook.WebHookPayload;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testing that stored data from version 1.0.2 of the plugin can be loaded from disk in newer versions.
 */
public class BackCompat102Tests {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void testTriggerConfig() {
        FreeStyleProject job = j.jenkins.getItemByFullName("OneConfigured", FreeStyleProject.class);
        assertNotNull("The job should be loaded", job);
        DockerHubTrigger trigger = DockerHubTrigger.getTrigger(job);
        assertNotNull("The trigger config of the job was not loaded", trigger);
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
    public void testPullImageBuilder() {
        FreeStyleProject job = j.jenkins.getItemByFullName("OneConfigured", FreeStyleProject.class);
        assertNotNull("The job should be loaded", job);
        DockerPullImageBuilder builder = job.getBuildersList().get(DockerPullImageBuilder.class);
        assertNotNull("Builder should be loaded", builder);
        assertEquals("rsandell/test", builder.getImage());
        DockerRegistryEndpoint registry = builder.getRegistry();
        assertNotNull("Registru should be loaded", registry);
        assertEquals("http://hub.rsandell.com", registry.getUrl());
    }

    @Test
    @LocalData
    public void testBuilds() {
        FreeStyleProject job = j.jenkins.getItemByFullName("JenkinsSlaveTrigger", FreeStyleProject.class);
        assertNotNull("The job should be loaded", job);

        RunList<FreeStyleBuild> builds = job.getBuilds();
        FreeStyleBuild two = builds.getLastBuild();
        assertNotNull(two);
        FreeStyleBuild one = two.getPreviousBuild();
        assertNotNull(one);

        assertSame("First build should be failure", Result.FAILURE, one.getResult());
        assertSame("Second build should be success", Result.SUCCESS, two.getResult());

        DockerHubWebHookCause cause = two.getCause(DockerHubWebHookCause.class);
        assertNotNull("The cause should be loaded", cause);
        WebHookPayload payload = cause.getPayload();
        assertNotNull("The cause should have a payload", payload);
        assertEquals("csanchez/jenkins-swarm-slave", payload.getRepoName());
        assertEquals("registry.hub.example.com", payload.getCallbackHost());
    }

    @Test
    @LocalData
    public void testFingerprintDb() throws IOException, InterruptedException {
        TriggerStore.TriggerEntry entry = TriggerStore.getInstance().getEntry("e9d6eb6cd6a7bfcd2bd622765a87893f");
        assertNotNull("TriggerEntry should be loaded in the fingerprint db", entry);
        assertTrue(entry.areAllDone());
        List<TriggerStore.TriggerEntry.RunEntry> runEntries = entry.getEntries();
        assertNotNull(runEntries);
        assertEquals(1, runEntries.size());
        TriggerStore.TriggerEntry.RunEntry run = runEntries.get(0);
        assertEquals("JenkinsSlaveTrigger", run.getJobName());
        assertNotNull("The run should be retrievable", run.getRun());

        WebHookPayload payload = entry.getPayload();
        assertNotNull("The entry should have a payload", payload);
        assertEquals("csanchez/jenkins-swarm-slave", payload.getRepoName());
        assertEquals("registry.hub.example.com", payload.getCallbackHost());

        CallbackPayload data = entry.getCallbackData();
        assertNotNull("There should be stored callback data", data);
        assertSame(CallbackPayload.States.success, data.getState());
        assertEquals("dockerhub-webhook/details/e9d6eb6cd6a7bfcd2bd622765a87893f", data.getTargetUrl());
    }

    @Test
    @LocalData
    public void testFingerprintUi() throws IOException, InterruptedException, SAXException {
        JenkinsRule.WebClient web = j.createWebClient();
        HtmlPage page = web.goTo("dockerhub-webhook/details/e9d6eb6cd6a7bfcd2bd622765a87893f");
        j.assertStringContains(page.asText(), "Build results for push of csanchez/jenkins-swarm-slave");
    }
}
