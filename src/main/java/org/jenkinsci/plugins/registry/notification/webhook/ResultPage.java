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
package org.jenkinsci.plugins.registry.notification.webhook;

import org.jenkinsci.plugins.registry.notification.Messages;
import org.jenkinsci.plugins.registry.notification.TriggerStore;
import hudson.model.ModelObject;

import javax.annotation.CheckForNull;

/**
 * Landing page from Docker Hub when multiple builds where triggered for the same web hook.
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 *
 * See <em>main.groovy</em>
 */
public class ResultPage implements ModelObject {
    public static final ResultPage NO_RESULT = new NoResultPage();

    private TriggerStore.TriggerEntry data;

    public ResultPage(TriggerStore.TriggerEntry data) {
        this.data = data;
    }

    @CheckForNull
    public TriggerStore.TriggerEntry getData() {
        return data;
    }

    @Override
    public String getDisplayName() {
        String repoName = "<unknown>";
        if (data != null) {
            repoName = data.getPushNotification().getRepoName();
        }
        return Messages.ResultPage_DisplayName(repoName);
    }

    public static class NoResultPage extends ResultPage {

        public NoResultPage() {
            super(null);
        }
    }
}
