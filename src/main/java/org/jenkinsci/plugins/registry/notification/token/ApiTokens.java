/**
 * The MIT License
 *
 * Copyright (c) 2022, CloudBees, Inc.
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
package org.jenkinsci.plugins.registry.notification.token;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.PersistentDescriptor;
import hudson.util.HttpResponses;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import net.jcip.annotations.GuardedBy;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.verb.POST;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
@Restricted(NoExternalUse.class)
@Symbol("dockerHubApiTokens")
public class ApiTokens extends GlobalConfiguration implements PersistentDescriptor {

    private static final Logger LOGGER = Logger.getLogger(ApiTokens.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HASH_ALGORITHM = "SHA-256";

    @GuardedBy("this")
    private final List<HashedApiToken> apiTokens;

    public ApiTokens() {
        this.apiTokens = new ArrayList<>();
    }

    @NonNull
    @Override
    public GlobalConfigurationCategory getCategory() {
        return GlobalConfigurationCategory.get(GlobalConfigurationCategory.Security.class);
    }

    public static ApiTokens get() {
        return GlobalConfiguration.all().get(ApiTokens.class);
    }

    @POST
    public HttpResponse doGenerate(StaplerRequest2 req) {
        // Require admin privileges to change the API tokens
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        String apiTokenName = req.getParameter("apiTokenName");
        JSONObject json = this.generateApiToken(apiTokenName);
        save();

        return HttpResponses.okJSON(json);
    }

    public JSONObject generateApiToken(@NonNull String name) {
        byte[] random = new byte[16];
        RANDOM.nextBytes(random);

        String plainTextApiToken = Util.toHexString(random);
        assert plainTextApiToken.length() == 32;

        String apiTokenValueHashed = Util.toHexString(hashedBytes(plainTextApiToken.getBytes(StandardCharsets.US_ASCII)));
        HashedApiToken apiToken = new HashedApiToken(name, apiTokenValueHashed);

        synchronized (this) {
            this.apiTokens.add(apiToken);
        }

        JSONObject json = new JSONObject();
        json.put("uuid", apiToken.getUuid());
        json.put("name", apiToken.getName());
        json.put("value", plainTextApiToken);

        return json;
    }

    @NonNull
    private static byte[] hashedBytes(byte[] tokenBytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("There is no " + HASH_ALGORITHM + " available in this system", e);
        }
        return digest.digest(tokenBytes);
    }

    @POST
    public HttpResponse doRevoke(StaplerRequest2 req) {
        // Require admin privileges to change the API tokens
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        String apiTokenUuid = req.getParameter("apiTokenUuid");
        if (StringUtils.isBlank(apiTokenUuid)) {
            return HttpResponses.errorWithoutStack(400, "API token UUID cannot be empty");
        }

        synchronized (this) {
            this.apiTokens.removeIf(apiToken -> apiToken.getUuid().equals(apiTokenUuid));
        }
        save();

        return HttpResponses.ok();
    }

    public synchronized Collection<HashedApiToken> getApiTokens() {
        return Collections.unmodifiableList(new ArrayList<>(this.apiTokens));
    }

    public boolean isValidApiToken(String plainApiToken) {
        if (StringUtils.isBlank(plainApiToken)) {
            return false;
        }

        return this.hasMatchingApiToken(plainApiToken);
    }

    public synchronized boolean hasMatchingApiToken(@NonNull String plainApiToken) {
        byte[] hash = hashedBytes(plainApiToken.getBytes(StandardCharsets.US_ASCII));
        return this.apiTokens.stream().anyMatch(apiToken -> apiToken.match(hash));
    }

    public static class HashedApiToken implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String uuid;
        private final String name;
        private final String hash;
        private final Date created;

        private HashedApiToken(String name, String hash) {
            this.uuid = UUID.randomUUID().toString();
            this.name = name;
            this.hash = hash;
            this.created = new Date();
        }

        private HashedApiToken(String uuid, String name, String hash, final Date created) {
            this.uuid = uuid;
            this.name = name;
            this.hash = hash;
            this.created = created;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getHash() {
            return hash;
        }

        public Date getCreated() {
            return new Date(created.getTime());
        }

        private boolean match(byte[] hashedBytes) {
            byte[] hashFromHex;
            try {
                hashFromHex = Util.fromHexString(hash);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "The API token with name=[{0}] is not in hex-format and so cannot be used", name);
                return false;
            }

            return MessageDigest.isEqual(hashFromHex, hashedBytes);
        }
    }
}
