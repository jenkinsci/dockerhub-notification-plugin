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
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jenkinsci.plugins.docker.commons.DockerImageExtractor;
import org.jenkinsci.plugins.docker.commons.DockerRegistryEndpoint;
import org.jenkinsci.plugins.docker.commons.KeyMaterial;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Pull and Run specified Docker image
 */
public class DockerPullImageBuilder extends Builder {

    private final String image;
    private final DockerRegistryEndpoint registry;

    @DataBoundConstructor
    public DockerPullImageBuilder(DockerRegistryEndpoint registry, String image) {
        this.image = image;
        this.registry = registry;
    }

    public String getImage() {
        return image;
    }

    public DockerRegistryEndpoint getRegistry() {
        return registry;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException {

        // TODO could maybe use Docker REST API, need first to check Java can talk with linux sockets
        // TODO maybe use DockerHost API
        int status = 0;
        KeyMaterial key = null;
        try {
            // get Docker registry credentials
            key = registry.newKeyMaterialFactory(build).materialize();

            status = launcher.launch()
                    .cmds("docker", "pull", registry.imageName(image)).envs(key.env())
                    .writeStdin().stdout(listener.getLogger()).stderr(listener.getLogger()).join();
            if (status != 0) {
                throw new RuntimeException("Failed to pull docker image");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to pull docker image", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to pull docker image", e);
        } finally {
            if (key != null) {
                key.close();
            }
        }

        listener.getLogger().println("docker pull " + image);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable image is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.DockerPullImageBuilder_DisplayName();
        }
    }

    @Extension
    public static final class ImageExtractor extends DockerImageExtractor {
        @Nonnull
        @Override
        public Collection<String> getDockerImagesUsedByJob(@Nonnull Job<?,?> job) {
            if (job instanceof Project) {
                Project<? extends Project, ? extends Build> project = (Project<?,? extends Build>)job;
                Set<String> images = new HashSet<String>();
                // check DockerHub build step for matching image ID
                for (Builder b : project.getBuilders()) {
                    if (b instanceof DockerPullImageBuilder) {
                        images.add(((DockerPullImageBuilder)b).getImage());
                    }
                }
                return images;
            } else {
                return Collections.emptySet();
            }
        }
    }
}

