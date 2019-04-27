package org.jenkinsci.plugins.registry.notification;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.webhook.Http;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRPushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRWebHookCause;
import org.jenkinsci.plugins.registry.notification.webhook.nexus.NexusDockerRegistryPushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.nexus.NexusDockerRegistryWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.nexus.NexusDockerRegistryWebHookCause;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing ACR webhook.
 */
public class NexusWebHookTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private String getWebHookURL() throws IOException {
        return this.j.getURL() + NexusDockerRegistryWebHook.URL_NAME + "/notify?host=myregistry.azurecr.io";
    }

    @Test
    public void testValidRepoCreate() throws Exception {
        PushNotificationRunListener listener = j.jenkins.getExtensionList(PushNotificationRunListener.class).get(0);
        listener.reset();
        createProjectWithTrigger(new TriggerOnSpecifiedImageNames(Collections.singletonList("myregistry.azurecr.io/hello-world")));
        JSONObject data = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/nexus-create-payload.json")));
        assertEquals(200, Http.post(getWebHookURL(), data));
        j.waitUntilNoActivity();
        NexusDockerRegistryPushNotification notification = listener.getPushNotification();
        assertNotNull(notification);
        assertEquals("v1", notification.getTag());
        assertEquals("myregistry.azurecr.io/hello-world", notification.getRepoName());
        assertEquals("myregistry.azurecr.io", notification.getRegistryHost());
    }

    @Test
    public void testValidRepoUpdate() throws Exception {
        PushNotificationRunListener listener = j.jenkins.getExtensionList(PushNotificationRunListener.class).get(0);
        listener.reset();
        createProjectWithTrigger(new TriggerOnSpecifiedImageNames(Collections.singletonList("myregistry.azurecr.io/hello-world")));
        JSONObject data = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/nexus-update-payload.json")));
        assertEquals(200, Http.post(getWebHookURL(), data));
        j.waitUntilNoActivity();
        NexusDockerRegistryPushNotification notification = listener.getPushNotification();
        assertNotNull(notification);
        assertEquals("v1", notification.getTag());
        assertEquals("myregistry.azurecr.io/hello-world", notification.getRepoName());
        assertEquals("myregistry.azurecr.io", notification.getRegistryHost());
    }

    private void createProjectWithTrigger(TriggerOption... options) throws Exception {
        FreeStyleProject project = this.j.createFreeStyleProject();
        project.addTrigger(new DockerHubTrigger(options));
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
    }

    @TestExtension
    public static class PushNotificationRunListener extends RunListener<Run<?,?>> {
        private List<Run> hits;

        public PushNotificationRunListener() {
            this.hits = new ArrayList<Run>();
        }

        @Override
        public void onFinalized(Run<?, ?> run) {
            super.onFinalized(run);
            this.hits.add(run);
        }

        public void reset() {
            this.hits.clear();
        }

        public NexusDockerRegistryPushNotification getPushNotification() {
            for (Run hit : this.hits) {
                NexusDockerRegistryWebHookCause cause = (NexusDockerRegistryWebHookCause) hit.getCause(NexusDockerRegistryWebHookCause.class);
                return (NexusDockerRegistryPushNotification) cause.getPushNotification();
            }
            return null;
        }
    }
}

