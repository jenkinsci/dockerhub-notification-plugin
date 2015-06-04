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

import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Lists the images that a job triggers on.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class TriggerListViewColumn extends ListViewColumn {

    private int showMax;

    @DataBoundConstructor
    public TriggerListViewColumn() {
        showMax = 0;
    }

    public int getShowMax() {
        return showMax;
    }

    @DataBoundSetter
    public void setShowMax(int showMax) {
        this.showMax = showMax;
    }

    @Override
    public String getColumnCaption() {
        return Messages.TriggerListViewColumn_ColumnCaption();
    }

    public Collection<String> getImageNames(TopLevelItem item) {
        if (item instanceof ParameterizedJobMixIn.ParameterizedJob) {
            DockerHubTrigger trigger = DockerHubTrigger.getTrigger((ParameterizedJobMixIn.ParameterizedJob)item);
            if (trigger != null) {
                Set<String> names = trigger.getAllRepoNames();
                if (showMax <= 0 || names.size() <= showMax) {
                    return names;
                } else {
                    String[] array = names.toArray(new String[names.size()]);
                    return Arrays.asList(Arrays.copyOf(array, showMax));
                }
            }
        }
        return Collections.emptySet();
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.TriggerListViewColumn_DisplayName();
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }
    }
}
