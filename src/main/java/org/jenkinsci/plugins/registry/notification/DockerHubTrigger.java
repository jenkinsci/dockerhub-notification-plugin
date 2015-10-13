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

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOptionDescriptor;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHook;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.*;

/**
 * The trigger configuration. The actual trigger logic is in {@link DockerHubWebHook}.
 */
public class DockerHubTrigger extends Trigger<Job<?, ?>> {

    private List<TriggerOption> options;

    @DataBoundConstructor
    public DockerHubTrigger(List<TriggerOption> options) {
        this.options = options;
    }

    public DockerHubTrigger(TriggerOption... options) {
        this(Arrays.asList(options));
    }

    @Override
    public void start(Job job, boolean newInstance) {
        this.job = job;
        // TODO register jenkins instance to dockerhub hook
    }

    public List<TriggerOption> getOptions() {
        return options;
    }

    public DescribableList<TriggerOption, TriggerOptionDescriptor> getOptionsList() {
        return new DescribableList<TriggerOption, TriggerOptionDescriptor>(this.job,
                this.options != null ? this.options : Collections.<TriggerOption>emptyList());
    }

    @DataBoundSetter
    public void setOptions(List<TriggerOption> options) {
        this.options = options;
    }

    @Nonnull
    public Set<String> getAllRepoNames() {
        Set<String> all = new HashSet<String>();
        if (options != null) {
            for (TriggerOption option : options) {
                all.addAll(option.getRepoNames(this.job));
            }
        }
        return all;
    }

    @CheckForNull
    public static DockerHubTrigger getTrigger(ParameterizedJobMixIn.ParameterizedJob job) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            DockerHubTrigger.DescriptorImpl descriptor = jenkins.getDescriptorByType(DockerHubTrigger.DescriptorImpl.class);
            if (descriptor != null) {
                Map<TriggerDescriptor, Trigger<?>> triggers = job.getTriggers();
                return (DockerHubTrigger)triggers.get(descriptor);
            }
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.DockerHubTrigger_DisplayName();
        }

        @Override
        public Trigger<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            JSONObject data = formData.getJSONObject("options");
            List<TriggerOption> r = new Vector<TriggerOption>();
            for (TriggerOptionDescriptor d : TriggerOptionDescriptor.all()) {
                String safeName = d.getJsonSafeClassName();
                if (req.getParameter(safeName) != null) {
                    TriggerOption instance = d.newInstance(req, data.getJSONObject(safeName));
                    r.add(instance);
                }
            }
            return new DockerHubTrigger(r);
        }
    }
}
