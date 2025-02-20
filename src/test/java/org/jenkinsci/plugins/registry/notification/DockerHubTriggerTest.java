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

import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerForAllUsedInJob;

import hudson.model.Item;
import hudson.model.FreeStyleProject;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Tests for {@link DockerHubTrigger}.
 */
@WithJenkins
class DockerHubTriggerTest {

    @Test
    void testConfigRoundTrip(JenkinsRule j) throws Exception {
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
    void testConfigRoundTripEmptyNames(JenkinsRule j) throws Exception {
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
