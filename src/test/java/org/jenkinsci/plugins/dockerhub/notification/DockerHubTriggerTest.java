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

import org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerForAllUsedInJob;
import org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerOnSpecifiedImageNames;

import hudson.model.Item;
import hudson.model.FreeStyleProject;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DockerHubTrigger}.
 */
public class DockerHubTriggerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testConfigRoundTrip() throws Exception {
        String[] expectedRepoNames = new String[] {"cb/jenkins", "cb/je"};
        FreeStyleProject project = j.createFreeStyleProject();
        project.addTrigger(new DockerHubTrigger(new TriggerForAllUsedInJob(), new TriggerOnSpecifiedImageNames(expectedRepoNames)));
        DockerHubTrigger trigger = DockerHubTrigger.getTrigger(project);
        assertNotNull(trigger);
        assertThat(trigger.getAllRepoNames(), contains(expectedRepoNames));
        project = (FreeStyleProject) j.configRoundtrip((Item)project);
        trigger = DockerHubTrigger.getTrigger(project);
        assertNotNull(trigger);
        assertThat(trigger.getAllRepoNames(), contains(expectedRepoNames));
        assertThat(trigger.getAllRepoNames(), contains(expectedRepoNames));
    }

    @Test
    public void testConfigRoundTripEmptyNames() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.addTrigger(new DockerHubTrigger(new TriggerForAllUsedInJob(), new TriggerOnSpecifiedImageNames()));
        DockerHubTrigger trigger = DockerHubTrigger.getTrigger(project);
        assertNotNull(trigger);
        assertThat(trigger.getAllRepoNames(), empty());
        project = (FreeStyleProject) j.configRoundtrip((Item)project);
        trigger = DockerHubTrigger.getTrigger(project);
        assertNotNull(trigger);
        assertThat(trigger.getAllRepoNames(), empty());
        assertThat(trigger.getAllRepoNames(), empty());
    }
}