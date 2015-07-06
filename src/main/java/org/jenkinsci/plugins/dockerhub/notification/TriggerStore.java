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

import org.jenkinsci.plugins.dockerhub.notification.webhook.CallbackPayload;
import org.jenkinsci.plugins.dockerhub.notification.webhook.WebHookPayload;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Fingerprint;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.security.ACL;
import jenkins.model.FingerprintFacet;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Store of all triggered builds.
 */
@Extension
public final class TriggerStore extends Descriptor<TriggerStore>
        implements Describable<TriggerStore> {

    @Inject
    Jenkins jenkins;
    
    public TriggerStore() {
        super(TriggerStore.class);
    }

    public synchronized void triggered(@Nonnull final WebHookPayload payload, Job<?, ?> job) {
        try {
            TriggerEntry entry = getOrCreateEntry(payload);
            entry.addEntry(job);
            save(entry);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update triggered info for " + job.getFullDisplayName(), e);
        }
    }

    public synchronized void started(@Nonnull final WebHookPayload payload, Run<?, ?> run) {
        try {
            TriggerEntry entry = getOrCreateEntry(payload);
            entry.updateEntry(run);
            save(entry);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update started info for " + run.getFullDisplayName(), e);
        }
    }

    @CheckForNull
    public synchronized TriggerEntry finalized(@Nonnull final WebHookPayload payload, Run<?, ?> run) {
        try {
            TriggerEntry entry = getOrCreateEntry(payload);
            entry.updateEntry(run);
            save(entry);
            return entry;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update finalized info for " + run.getFullDisplayName(), e);
            return null;
        }
    }

    /**
     * When a build has been removed from jenkins it should also be removed from this store.
     *
     * @param run the build.
     */
    public synchronized void removed(@Nonnull final WebHookPayload payload, Run<?, ?> run) {
        try {
            TriggerEntry entry = getEntry(payload.sha());
            if (entry != null) {
                entry.removeEntry(run);
                if (entry.getEntries().isEmpty()) {
                    // TODO: FingerprintFacet should have isAlive() method to let it report its liveness.
                }
                save(entry);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to remove info for build " + run.getFullDisplayName(), e);
        }
    }

    @Nonnull
    private synchronized TriggerEntry getOrCreateEntry(@Nonnull final WebHookPayload payload) throws IOException, InterruptedException {
        Fingerprint fingerprint = jenkins.getFingerprintMap().getOrCreate(null, payload.getRepoName(), payload.sha());
        TriggerEntry entry = fingerprint.getFacet(TriggerEntry.class);
        if (entry==null)    fingerprint.getFacets().add(entry=new TriggerEntry(fingerprint,payload));
        return entry;
    }

    /**
     * Gets an existing {@link TriggerEntry}, or null if no such thing exists.
     *
     * @param sha the {@link WebHookPayload#sha()}.
     * @return the entry if found.
     * @throws IOException          if so
     * @throws InterruptedException if so
     */
    @CheckForNull
    public synchronized TriggerEntry getEntry(String sha) throws IOException, InterruptedException {
        Fingerprint fingerprint = jenkins.getFingerprintMap().get(sha);
        if (fingerprint==null)  return null;
        return fingerprint.getFacet(TriggerEntry.class);
    }

    private synchronized void onLocationChanged(@Nonnull Job<?,?> item, @Nonnull String oldFullName, @Nonnull String newFullName) {
        // no efficient way to do this in fingerprint. But hey, cool job names do not change http://www.w3.org/Provider/Style/URI.html
//        try {
//            //This could be quite many, but I have no better ideas
//            List<String> shas = getAllStoredShas();
//            for (String sha : shas) {
//                TriggerEntry entry = getEntry(sha);
//                if (entry != null) {
//                    boolean updated = false;
//                    for (TriggerEntry.RunEntry run : entry.getEntries()) {
//                        if (oldFullName.equals(run.getJobName())) {
//                            run.setJobName(newFullName);
//                            updated = true;
//                        }
//                    }
//                    if (updated) {
//                        save(entry);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "Failed to update a potentially stored job reference \'"+oldFullName+"\' to \'"+newFullName+"\'", e);
//            e.printStackTrace();
//        }
    }

    public synchronized void save(@Nonnull final TriggerEntry entry) throws IOException, InterruptedException {
        entry.getFingerprint().save();
    }

    /**
     * Gets the effective singleton instance.
     *
     * @return the effective singleton instance.
     * @throws AssertionError if the singleton is missing, i.e. not running on a Jenkins master.
     */
    @Nonnull
    public static TriggerStore getInstance() {
        Jenkins instance = Jenkins.getInstance();
        TriggerStore d;
        if (instance == null) {
            d = null;
        } else if (instance.getInitLevel().compareTo(InitMilestone.JOB_LOADED) < 0) {
            throw new AssertionError(TriggerStore.class.getName() + " is not available until after all jobs are loaded");
        } else {
            d = instance.getDescriptorByType(TriggerStore.class);
        }
        if (d == null) {
            throw new AssertionError(TriggerStore.class.getName() + " is missing");
        }
        return d;
    }

    @Override
    public Descriptor<TriggerStore> getDescriptor() {
        return this;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    public static class TriggerEntry extends FingerprintFacet {
        @Nonnull
        private final WebHookPayload payload;
        @Nonnull
        private final List<RunEntry> entries;
        @CheckForNull
        private CallbackPayload callbackData;

        public TriggerEntry(Fingerprint fingerprint, @Nonnull WebHookPayload payload) {
            super(fingerprint, payload.getReceived());
            this.payload = payload;
            entries = new LinkedList<RunEntry>();
        }

        @Nonnull
        public RunEntry addEntry(Job<?, ?> job) {
            RunEntry entry = getEntry(job.getFullName());
            if (entry == null) {
                entry = new RunEntry(job.getFullName());
            }
            return entry;
        }

        public RunEntry getEntry(@Nonnull Job<?, ?> job) {
            return getEntry(job.getFullName());
        }

        public RunEntry getEntry(@Nonnull String jobName) {
            for (RunEntry entry : entries) {
                if (entry.getJobName().equals(jobName)) {
                    return entry;
                }
            }
            return null;
        }

        public RunEntry updateEntry(Run<?, ?> run) {
            RunEntry entry = getEntry(run.getParent());
            if (entry == null) {
                entry = new RunEntry(run.getParent().getFullName(), run.getId());
                entries.add(entry);
            } else {
                entry.setRun(run);
            }
            entry.setDone(!run.isBuilding());
            return entry;
        }

        @Nonnull
        public WebHookPayload getPayload() {
            return payload;
        }

        @Nonnull
        public List<RunEntry> getEntries() {
            return entries;
        }

        @CheckForNull
        public CallbackPayload getCallbackData() {
            return callbackData;
        }

        public void setCallbackData(@CheckForNull CallbackPayload callbackData) {
            this.callbackData = callbackData;
        }

        public void removeEntry(@Nonnull Run<?, ?> run) {
            RunEntry entry = getEntry(run.getParent());
            if (entry != null) {
                entries.remove(entry);
            }
        }

        public boolean areAllDone() {
            for (RunEntry entry : entries) {
                if (!entry.isDone()) {
                    return false;
                }
            }
            return true;
        }

        public static class RunEntry implements Serializable {
            private static final long serialVersionUID = -4889803337604416914L;
            private String jobName;
            private String buildId;
            private boolean done;

            public RunEntry(@Nonnull String jobName) {
                this.jobName = jobName;
            }

            public RunEntry(@Nonnull String jobName, String buildId) {
                this.jobName = jobName;
                this.buildId = buildId;
            }

            @Nonnull
            public String getJobName() {
                return jobName;
            }

            public void setJobName(@Nonnull String jobName) {
                this.jobName = jobName;
            }

            @CheckForNull
            public String getBuildId() {
                return buildId;
            }

            public boolean isDone() {
                return done;
            }

            public void setDone(boolean done) {
                this.done = done;
            }

            @CheckForNull
            public Job<?, ?> getJob() {
                final Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    SecurityContext old = ACL.impersonate(ACL.SYSTEM);
                    try {
                        return jenkins.getItemByFullName(jobName, Job.class);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to retrieve job " + jobName, e);
                    } finally {
                        SecurityContextHolder.setContext(old);
                    }
                }
                return null;
            }

            public void setRun(@CheckForNull Run<?, ?> build) {
                if (build == null) {
                    this.buildId = null;
                } else {
                    this.buildId = build.getId();
                }
            }

            @CheckForNull
            public Run<?, ?> getRun() {
                if (StringUtils.isBlank(buildId)) {
                    return null;
                }
                final Job<?, ?> job = getJob();
                if (job != null) {
                    SecurityContext old = ACL.impersonate(ACL.SYSTEM);
                    try {
                        return job.getBuild(buildId);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to retrieve run " + jobName + ":" + buildId, e);
                    } finally {
                        SecurityContextHolder.setContext(old);
                    }
                }
                return null;
            }
        }
    }

    @Extension
    public static class ItemListener extends hudson.model.listeners.ItemListener {
        @Override
        public void onLocationChanged(Item item, String oldFullName, String newFullName) {
            if (item instanceof Job) {
                TriggerStore.getInstance().onLocationChanged((Job<?, ?>)item, oldFullName, newFullName);
            }
        }
    }

    private static final Logger logger = Logger.getLogger(TriggerStore.class.getName());
}
