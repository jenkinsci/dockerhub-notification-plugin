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
package org.jenkinsci.plugins.registry.notification;

import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;

public class DockerHubWebHookTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test(timeout = 60000)
    public void testDoIndex() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        final String repoName = "cb/jenkins";
        project.addTrigger(new DockerHubTrigger(new TriggerOnSpecifiedImageNames(repoName)));
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));
        j.createWebClient().goTo("dockerhub-webhook/debug?image=" + repoName);

        while(project.getBuilds().isEmpty()) {
            Thread.sleep(500);
        }

        j.assertLogContains(repoName, project.getLastBuild());
    }
}