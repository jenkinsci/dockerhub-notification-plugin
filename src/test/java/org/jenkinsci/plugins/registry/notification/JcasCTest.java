/**
 * The MIT License
 * <p>
 * Copyright (c) 2020, CloudBees, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.registry.notification;

import hudson.model.ListView;
import hudson.model.View;
import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import org.junit.jupiter.api.Nested;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that {@link TriggerViewFilter} and {@link TriggerListViewColumn} can be configured with configuration as code.
 * <p>
 * Testing both with a configuration before symbols where added and with the new symbols.
 *
 * @see <a href="https://plugins.jenkins.io/configuration-as-code/">Configuration as Code Plugin</a>
 */
class JcasCTest {

    private static final String LOG_MESSAGE = "Setting org.jenkinsci.plugins.registry.notification.TriggerListViewColumn";

    private static void assertConfiguredAsExpected(final JenkinsRule j) {
        final View view = j.jenkins.getView("Docker");
        assertNotNull(view);
        ListView dv = (ListView) view;
        assertThat(dv.getColumns(), hasItem(
                allOf(
                        instanceOf(TriggerListViewColumn.class),
                        hasProperty("showMax", equalTo(3))
                )
        ));
        assertThat(dv.getJobFilters(), hasItem(
                allOf(
                        instanceOf(TriggerViewFilter.class),
                        hasProperty("patterns", containsInAnyOrder(
                                equalTo(".*"),
                                equalTo("jenkins"))
                        )
                )
        ));
    }

    @Nested
    @WithJenkins
    class BareTest extends AbstractRoundTripTest {

        @Override
        protected void assertConfiguredAsExpected(final JenkinsRule j, final String s) {
            JcasCTest.assertConfiguredAsExpected(j);
        }

        @Override
        protected String stringInLogExpected() {
            return LOG_MESSAGE;
        }

        @Override
        protected String configResource() {
            return "/jcasc_bare.yaml";
        }
    }

    @Nested
    @WithJenkins
    class SymbolsTest extends AbstractRoundTripTest {

        @Override
        protected void assertConfiguredAsExpected(final JenkinsRule j, final String s) {
            JcasCTest.assertConfiguredAsExpected(j);
        }

        @Override
        protected String stringInLogExpected() {
            return LOG_MESSAGE;
        }

        @Override
        protected String configResource() {
            return "/jcasc_symbols.yaml";
        }
    }
}
