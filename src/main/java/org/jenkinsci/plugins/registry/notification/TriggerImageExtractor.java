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
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import hudson.Extension;
import hudson.model.Job;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.docker.commons.DockerImageExtractor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

/**
 * Extracts the explicitly stated images used by {@link DockerHubTrigger}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 * @see DockerHubTrigger#getAllRepoNames()
 * @see TriggerOnSpecifiedImageNames
 */
@Extension
public class TriggerImageExtractor extends DockerImageExtractor {
    @Nonnull
    @Override
    public Collection<String> getDockerImagesUsedByJob(@Nonnull Job<?,?> job) {
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            DockerHubTrigger trigger = DockerHubTrigger.getTrigger((ParameterizedJobMixIn.ParameterizedJob)job);
            if (trigger != null) {
                for (TriggerOption option : trigger.getOptions()) {
                    if (option instanceof TriggerOnSpecifiedImageNames) {
                        return ((TriggerOnSpecifiedImageNames)option).getRepoNames();
                    }
                }
            }
        }
        return Collections.emptySet();
    }
}
