package org.jenkinsci.plugins.registry.notification.events;

import hudson.EnvVars;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.registry.notification.events.impl.*;
import org.jenkinsci.plugins.registry.notification.webhook.PushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.DockerTrustedRegistryPushNotification;
import org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.DockerTrustedRegistryWebHookPayload;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(JUnitParamsRunner.class)
public class EventTypeTest {
    private Map<EventType, DockerTrustedRegistryPushNotification> pushTypes;

    public EventTypeTest() throws IOException {
        pushTypes = new HashMap<EventType, DockerTrustedRegistryPushNotification>();
        pushTypes.putAll(getWebHookPayload(new TagPushed(), "/docker-trusted-registry-payload-tag-push.json"));
        pushTypes.putAll(getWebHookPayload(new TagDeleted(),"/docker-trusted-registry-payload-tag-delete.json"));
        pushTypes.putAll(getWebHookPayload(new ManifestPushed(), "/docker-trusted-registry-payload-manifest-push.json"));
        pushTypes.putAll(getWebHookPayload(new ManifestDeleted(),"/docker-trusted-registry-payload-manifest-delete.json"));
        pushTypes.putAll(getWebHookPayload(new SecurityScanCompleted(),"/docker-trusted-registry-payload-scan-completed.json"));
    }

    private Set<Map.Entry<EventType, DockerTrustedRegistryPushNotification>> getPushTypes(){ return pushTypes.entrySet(); }

    private Map<EventType, DockerTrustedRegistryPushNotification> getWebHookPayload(final EventType et, String s) throws IOException {
        final JSONObject json = JSONObject.fromObject(IOUtils.toString(this.getClass().getResourceAsStream(s)));
        return new HashMap<EventType, DockerTrustedRegistryPushNotification>(){{ put(et, new DockerTrustedRegistryPushNotification( new DockerTrustedRegistryWebHookPayload(json), "junit/reponame", et.getType(), json.getJSONObject("contents").getString("imageName").split("/")[0])); }};
    }

    @Test
    @Parameters(method = "getPushTypes")
    public void testBuildRuntimeForEachType(Map.Entry<EventType,PushNotification> pushType) throws Exception {
        checkBuildRuntime(pushType.getKey(),pushType.getValue());
    }

    private void checkBuildRuntime(EventType et, PushNotification push) {
        EnvVars ev = new EnvVars();
        et.buildEnvironment(ev, push);
        assertEquals(et.getType(), ev.get(EventTypeDescriptor.ENVIRONMENT_KEY));

        JSONObject contents = push.getWebHookPayload().getData().getJSONObject("contents");

        String payloadTimeStamp = et.getTimeStamp(contents);
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
        Date expectedTime = parser.parseDateTime(payloadTimeStamp).toDate();
        assertEquals(expectedTime, push.getPushedAt());

        if( et.hasDigest() ) {
            String expectedDigest = contents.getString("digest");
            assertEquals(expectedDigest, ev.get(EventType.KEY_DOCKER_IMAGE_DIGEST));
        } else {
            assertNull(ev.get(EventType.KEY_DOCKER_IMAGE_DIGEST));
        }
    }

    @Test
    @Parameters(method = "getPushTypes")
    public void testEachEventTypeShouldAcceptItsOwnPayload(Map.Entry<EventType,DockerTrustedRegistryPushNotification> pushType) throws Exception {
        assertTrue(pushType.getKey() + " should have accepted it's own payload but didn't!", pushType.getKey().accepts(pushType.getValue().getDtrEventJSONType()));
    }

    @Test
    @Parameters(method = "getPushTypes")
    public void testEachEventTypeShouldNotAcceptOtherPayloads(Map.Entry<EventType,DockerTrustedRegistryPushNotification> pushType) throws Exception {
        for (Map.Entry<EventType, DockerTrustedRegistryPushNotification> other : getOtherPushNotifications(pushType.getKey()).entrySet()) {
            EventType otherType = other.getKey();
            DockerTrustedRegistryPushNotification otherPayload = other.getValue();
            assertFalse(pushType.getKey() + " accepted payload for " + otherType.getType() + " but should not have!", pushType.getKey().accepts(otherPayload.getDtrEventJSONType()));
        }
    }

    private Map<EventType, DockerTrustedRegistryPushNotification> getOtherPushNotifications(EventType eventType) {
        Map<EventType, DockerTrustedRegistryPushNotification> others = new HashMap<EventType, DockerTrustedRegistryPushNotification>();
        for (Map.Entry<EventType, DockerTrustedRegistryPushNotification> pt : pushTypes.entrySet()) {
            if (!pt.getKey().equals(eventType)) {
                others.put(pt.getKey(), pt.getValue());
            }
        }
        return others;
    }

}
