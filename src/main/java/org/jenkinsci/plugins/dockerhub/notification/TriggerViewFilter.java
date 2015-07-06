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
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.util.FormValidation;
import hudson.views.ViewJobFilter;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filters jobs that are triggered by docker hub.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class TriggerViewFilter extends ViewJobFilter {

    @Nonnull
    private List<String> patterns;
    private transient List<Pattern> compiled;

    @DataBoundConstructor
    public TriggerViewFilter(@Nonnull List<String> patterns) throws Descriptor.FormException {
        this.patterns = patterns;
        try {
            compilePatterns();
        } catch (PatternSyntaxException e) {
            throw new Descriptor.FormException(Messages.TriggerViewFilter_CompileError(), e, "patterns");
        }
    }

    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
        List<Pattern> patterns = getCompiled();
        List<TopLevelItem> workList = added.isEmpty() ? all : added;
        List<TopLevelItem> filtered = new LinkedList<TopLevelItem>();

        for (TopLevelItem item : workList) {
            if (item instanceof ParameterizedJobMixIn.ParameterizedJob) {
                DockerHubTrigger trigger = DockerHubTrigger.getTrigger((ParameterizedJobMixIn.ParameterizedJob)item);
                if (trigger != null) {
                    if (patterns.isEmpty()) {
                        filtered.add(item);
                    } else {
                        for (String name : trigger.getAllRepoNames()) {
                            if(matches(name)) {
                                filtered.add(item);
                            }
                        }
                    }
                }
            }
        }
        return filtered;
    }

    private boolean matches(String name) {
        List<Pattern> patterns = getCompiled();
        for (Pattern pattern : patterns) {
            if (pattern.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    synchronized List<Pattern> getCompiled() {
        compilePatterns();
        return compiled;
    }

    private synchronized void compilePatterns() {
        if (compiled == null) {
            compiled = compilePatterns(patterns);
        }
    }

    @Nonnull
    private static List<Pattern> compilePatterns(@Nonnull List<String> strings) {
        List<Pattern> comp = new LinkedList<Pattern>();
        for (String pattern : strings) {
            comp.add(Pattern.compile(pattern));
        }
        return comp;
    }

    @Nonnull
    public synchronized List<String> getPatterns() {
        return patterns;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
        @Override
        public String getDisplayName() {
            return Messages.TriggerViewFilter_DisplayName();
        }

        @Override
        public ViewJobFilter newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // TODO JENKINS-27901: need a standard control for this
            if (formData.has("patterns") && !StringUtils.isBlank(formData.optString("patterns"))) {
                JSONArray array = new JSONArray();
                String[] split = StringUtils.split(formData.getString("patterns"), '\n');
                if (split != null && split.length > 0) {
                    array.addAll(Arrays.asList(split));
                }
                formData.put("patterns", array);
            } else {
                formData.put("patterns", new JSONArray());
            }
            return super.newInstance(req, formData);
        }

        public FormValidation doCheckPatterns(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok();
            } else {
                String[] split = StringUtils.split(value, '\n');
                try {
                    compilePatterns(Arrays.asList(split));
                    return FormValidation.ok();
                } catch (PatternSyntaxException e) {
                    return FormValidation.error(e, Messages.TriggerViewFilter_CompileError());
                }
            }
        }
    }
}
