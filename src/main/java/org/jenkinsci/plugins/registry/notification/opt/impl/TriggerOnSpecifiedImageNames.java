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

import org.jenkinsci.plugins.registry.notification.opt.TriggerOptionDescriptor;
import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.opt.TriggerOption;
import hudson.Extension;
import hudson.model.Job;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link TriggerOption} with manually specified names.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class TriggerOnSpecifiedImageNames extends TriggerOption {
    private Set<String> repoNames;

    @DataBoundConstructor
    public TriggerOnSpecifiedImageNames() {
        this.repoNames = Collections.emptySet();
    }

    public TriggerOnSpecifiedImageNames(Collection<String> repoNames) {
        this.repoNames = new HashSet<String>();
        if (repoNames != null) {
            this.repoNames.addAll(repoNames);
        }
    }

    public TriggerOnSpecifiedImageNames(String... repoNames) {
        this(Arrays.asList(repoNames));
    }

    public Set<String> getRepoNames() {
        return repoNames;
    }

    @DataBoundSetter
    public void setRepoNames(Set<String> repoNames) {
        this.repoNames = repoNames;
    }

    @Override
    public Collection<String> getRepoNames(Job<?, ?> job) {
        return getRepoNames();
    }

    @Extension
    public static class DescriptorImpl extends TriggerOptionDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.TriggerOption_TriggerOnSpecifiedImageNames_DisplayName();
        }

        @Override
        public TriggerOption newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // TODO JENKINS-27901: need a standard control for this
            if (formData.has("repoNames") && !StringUtils.isBlank(formData.optString("repoNames"))) {
                JSONArray array = new JSONArray();
                array.addAll(Arrays.asList(StringUtils.split(formData.getString("repoNames"))));
                formData.put("repoNames", array);
            } else {
                formData.put("repoNames", new JSONArray());
            }
            return super.newInstance(req, formData);
        }
    }
}
