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
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Fingerprint;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.Run;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.DescribableList;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOptionDescriptor;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerForAllUsedInJob;
import org.jenkinsci.plugins.registry.notification.opt.impl.TriggerOnSpecifiedImageNames;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHookCause;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    @Initializer(before = InitMilestone.JOB_LOADED)
    @Restricted(NoExternalUse.class)
    public static void packageRenameConverting() {
        for(XStream2 xs : Arrays.asList(Items.XSTREAM2, Run.XSTREAM2, Jenkins.XSTREAM2, getFingerprintXStream())) {
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.DockerHubTrigger",
                                     DockerHubTrigger.class);
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.DockerHubWebHookCause",
                                     DockerHubWebHookCause.class);
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.DockerPullImageBuilder",
                                     DockerPullImageBuilder.class);
            //TODO no back-compat tests for the column and filter
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.TriggerListViewColumn",
                                     TriggerListViewColumn.class);
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.TriggerViewFilter",
                                     TriggerViewFilter.class);
            //The TriggerOption extension point has also changed package name and will not be backwards compatible API
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerForAllUsedInJob",
                                     TriggerForAllUsedInJob.class);
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerOnSpecifiedImageNames",
                                     TriggerOnSpecifiedImageNames.class);
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.TriggerStore$TriggerEntry",
                                     TriggerStore.TriggerEntry.class);
            xs.addCompatibilityAlias("org.jenkinsci.plugins.dockerhub.notification.TriggerStore$TriggerEntry$RunEntry",
                                     TriggerStore.TriggerEntry.RunEntry.class);
        }
    }

    /**
     * Hack to get around the fact that {@link Fingerprint#XSTREAM} has private access.
     * If any issues arise when trying to access the field a new XStream object is returned to avoid null values.
     * @return {@link Fingerprint}'s XStream2 static field.
     */
    @Nonnull
    private static XStream2 getFingerprintXStream() {
        try {
            Field field = Fingerprint.class.getDeclaredField("XSTREAM");
            field.setAccessible(true);
            XStream2 xStream2 = (XStream2)field.get(null);
            if (xStream2 == null) {
                xStream2 = new XStream2();
            }
            return xStream2;
        } catch (NoSuchFieldException e) {
            Logger.getLogger(DockerHubTrigger.class.getName()).log(Level.WARNING, "Fingerprint XStream instance gone? " +
                    "Old data conversion of stored callback reports can't be performed. Risk of data loss.", e);
        } catch (IllegalAccessException e) {
            Logger.getLogger(DockerHubTrigger.class.getName()).log(Level.WARNING,
                    "Fingerprint XStream instance inaccessible due to installed security manager. " +
                    "Old data conversion of stored callback reports can't be performed. Risk of data loss.", e);
        }
        return new XStream2();
    }
}
