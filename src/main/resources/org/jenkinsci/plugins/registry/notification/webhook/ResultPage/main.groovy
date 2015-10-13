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
package org.jenkinsci.plugins.registry.notification.webhook.ResultPage

import org.jenkinsci.plugins.registry.notification.TriggerStore
import hudson.model.Item
import hudson.model.Run

import java.text.DateFormat

def l = namespace(lib.LayoutTagLib)

TriggerStore.TriggerEntry data = triggerData

link(rel: "stylesheet", href: "${rootURL}/${res(my, "style.css")}", type: "text/css")

h1(_("heading", repoName, data.pushNotification.pushedAt))

data.entries.each { entry ->
    Run run = entry.run
    if (run != null && (run.hasPermission(Item.READ) || run.hasPermission(Item.DISCOVER))) {
        div(class: "build-entry result-${run.result.toString()} ${run.result.color.iconClassName}") {
            a(href: "${rootURL}/${run.url}", class: "result-icon") {
                l.icon(class: "${run.result.color.iconClassName} icon-lg", alt: run.result.color.description,
                        tooltip: run.result.color.description)
            }
            div(class: "build-link") {
                a(href: "${rootURL}/${run.url}", class: "model-link", run.getFullDisplayName())
            }
            if (run.hasPermission(Item.READ)) {
                ul(class: "build-details") {
                    li(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, request.getLocale()).format(run.time))
                    li(run.durationString)
                    li(run.result.toString())
                }
                ul(class: "causes") {
                    run.causes.each { cause ->
                        li(cause.getShortDescription())
                    }
                }
                div(class: "buttons") {
                    a(class: "console", href: "${rootURL}/${run.url}console", _("Console"))
                    run.getArtifactsUpTo(2).each { artifact ->
                        a(class: "artifact", href: "${rootURL}/${run.url}artifact/${artifact.href}", artifact.displayPath)
                    }
                }
            }
        }
    }
}

