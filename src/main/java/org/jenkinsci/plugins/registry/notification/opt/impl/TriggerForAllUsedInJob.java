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
package org.jenkinsci.plugins.registry.notification.opt.impl;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.DockerImageExtractor;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOptionDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collection;
import java.util.Collections;

/**
 * {@link TriggerOption} to trigger on all images reported by all {@link DockerImageExtractor}s for the job.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class TriggerForAllUsedInJob extends TriggerOption {

    @DataBoundConstructor
    public TriggerForAllUsedInJob() {
    }

    @Override
    public Collection<String> getRepoNames(Job<?, ?> job) {
        if (job == null) {
            // DockerImageExtractor.getDockerImagesUsedByJobFromAll expects a non-null job argument
            // Return an empty list if the job argument is null
            return Collections.emptyList();
        }
        return DockerImageExtractor.getDockerImagesUsedByJobFromAll(job);
    }

    @Extension
    public static class DescriptorImpl extends TriggerOptionDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.TriggerOption_TriggerForAllUsedInJob_DisplayName();
        }

        @CheckForNull
        public static DescriptorImpl getInstance() {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return jenkins.getDescriptorByType(DescriptorImpl.class);
            }
            return null;
        }
    }
}
