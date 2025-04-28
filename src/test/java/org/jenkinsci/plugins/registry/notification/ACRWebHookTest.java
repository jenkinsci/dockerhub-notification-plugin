package org.jenkinsci.plugins.registry.notification;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.token.ApiTokens;
import org.jenkinsci.plugins.registry.notification.webhook.Http;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRPushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRWebHookCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing ACR webhook.
 */
@WithJenkins
class ACRWebHookTest {

    private JenkinsRule j;

    private String token;

    private String getWebHookURL() throws IOException {
        return this.j.getURL() + ACRWebHook.URL_NAME + "/" + token + "/notify";
    }

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        final JSONObject test = ApiTokens.get().generateApiToken("test");
        token = test.getString("value");
    }

    @Test
    void testValidRepoUpdate() throws Exception {
        PushNotificationRunListener listener = j.jenkins.getExtensionList(PushNotificationRunListener.class).get(0);
        listener.reset();
        createProjectWithTrigger(new TriggerOnSpecifiedImageNames(Collections.singletonList("myregistry.azurecr.io/hello-world")));
        JSONObject data = JSONObject.fromObject(IOUtils.toString(getClass().getResourceAsStream("/acr-payload-valid.json"), StandardCharsets.UTF_8));
        assertEquals(200, Http.post(getWebHookURL(), data));
        j.waitUntilNoActivity();
        ACRPushNotification notification = listener.getPushNotification();
        assertNotNull(notification);
        assertEquals("v1", notification.getTag());
        assertEquals("myregistry.azurecr.io", notification.getRegistryHost());

    }

    private void createProjectWithTrigger(TriggerOption... options) throws Exception {
        FreeStyleProject project = this.j.createFreeStyleProject();
        project.addTrigger(new DockerHubTrigger(options));
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
    }

    @TestExtension
    public static class PushNotificationRunListener extends RunListener<Run<?,?>> {
        private final List<Run> hits;

        public PushNotificationRunListener() {
            this.hits = new ArrayList<>();
        }

        @Override
        public void onFinalized(Run<?, ?> run) {
            super.onFinalized(run);
            this.hits.add(run);
        }

        public void reset() {
            this.hits.clear();
        }

        public ACRPushNotification getPushNotification() {
            for (Run hit : this.hits) {
                ACRWebHookCause cause = (ACRWebHookCause) hit.getCause(ACRWebHookCause.class);
                return (ACRPushNotification) cause.getPushNotification();
            }
            return null;
        }
    }
}

