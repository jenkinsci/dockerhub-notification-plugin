/**
 * The MIT License
 *
 * Copyright (c) 2020, CloudBees, Inc.
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

import hudson.model.ListView;
import hudson.model.View;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that {@link TriggerViewFilter} and {@link TriggerListViewColumn} can be configured with configuration as code.
 *
 * Testing both with a configuration before symbols where added and with the new symbols.
 *
 * @see <a href="https://plugins.jenkins.io/configuration-as-code/">Configuration as Code Plugin</a>
 */
@RunWith(Parameterized.class)
public class JcasCTest extends RoundTripAbstractTest {

    private String resource;

    public JcasCTest(final String resource) {
        this.resource = resource;
    }

    @Override
    protected void assertConfiguredAsExpected(final RestartableJenkinsRule j, final String s) {
        final View view = j.j.jenkins.getView("Docker");
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

    @Override
    protected String stringInLogExpected() {
        return "Setting org.jenkinsci.plugins.registry.notification.TriggerListViewColumn";
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] params() {
        return new Object[][]{{"/jcasc_bare.yaml"}, {"/jcasc_symbols.yaml"}};
    }

    @Override
    protected String configResource() {
        return resource;
    }
}
