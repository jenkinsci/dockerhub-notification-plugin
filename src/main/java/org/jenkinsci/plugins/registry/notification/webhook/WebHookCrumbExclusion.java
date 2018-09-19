/**
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
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

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import org.jenkinsci.plugins.registry.notification.webhook.acr.ACRWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.dockerhub.DockerHubWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.dockerregistry.DockerRegistryWebHook;
import org.jenkinsci.plugins.registry.notification.webhook.dockertrustedregistry.DockerTrustedRegistryWebHook;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Excludes {@link DockerHubWebHook}, {@link DockerRegistryWebHook} and {@link DockerTrustedRegistryWebHook} from having a CSRF protection filter.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
@Extension
public class WebHookCrumbExclusion extends CrumbExclusion {
    private static final String TRUSTED_REGISTRY_BASE = "/" + DockerTrustedRegistryWebHook.URL_NAME + "/";
    private static final String REGISTRY_BASE = "/" + DockerRegistryWebHook.URL_NAME + "/";
    private static final String HUB_BASE = "/" + DockerHubWebHook.URL_NAME + "/";
    private static final String ACR_BASE = "/" + ACRWebHook.URL_NAME + "/";

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && (pathInfo.startsWith(REGISTRY_BASE) || pathInfo.startsWith(HUB_BASE) || pathInfo.startsWith(ACR_BASE) || pathInfo.startsWith(TRUSTED_REGISTRY_BASE))) {
            chain.doFilter(request, response);
            return true;
        }
        return false;
    }
}
