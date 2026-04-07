/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jenkins.plugins.k8sengine.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import hudson.AbortException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/** Test suite for {@link ClientUtil}. */
@WithJenkins
class ClientUtilTest {

    private static final String TEST_CREDENTIALS_ID = "test-project";

    private static JenkinsRule jenkinsRule;

    @BeforeAll
    static void init(JenkinsRule rule) {
        jenkinsRule = rule;
    }

    @Test
    void testGetClientFactoryNullJenkins() {
        assertThrows(
                NullPointerException.class,
                () -> ClientUtil.getClientFactory(null, ImmutableList.of(), TEST_CREDENTIALS_ID, Optional.empty()));
    }

    @Test
    void testGetClientFactoryShortNullJenkins() {
        assertThrows(NullPointerException.class, () -> ClientUtil.getClientFactory(null, TEST_CREDENTIALS_ID));
    }

    @Test
    void testGetClientFactoryNullDomainRequirements() {
        assertThrows(
                NullPointerException.class,
                () -> ClientUtil.getClientFactory(jenkinsRule.jenkins, null, TEST_CREDENTIALS_ID, Optional.empty()));
    }

    @Test
    void testGetClientFactoryNullCredentialsId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), null, Optional.empty()));
    }

    @Test
    void testGetClientFactoryShortNullCredentialsId() {
        assertThrows(IllegalArgumentException.class, () -> ClientUtil.getClientFactory(jenkinsRule.jenkins, null));
    }

    @Test
    void testGetClientFactoryEmptyCredentialsId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), "", Optional.empty()));
    }

    @Test
    void testGetClientFactoryShortEmptyCredentialsId() {
        assertThrows(IllegalArgumentException.class, () -> ClientUtil.getClientFactory(jenkinsRule.jenkins, ""));
    }

    @Test
    void testGetClientFactoryFailsWithInvalidCredentialsId() {
        assertThrows(NullPointerException.class, () -> {
            try {
                ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), "fake", Optional.empty());
            } catch (AbortException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void testGetClientFactoryShortFailsWithInvalidCredentialsId() {
        assertThrows(NullPointerException.class, () -> {
            try {
                ClientUtil.getClientFactory(jenkinsRule.jenkins, "fake");
            } catch (AbortException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    void testGetClientFactoryTransportNull() {
        assertThrows(
                NullPointerException.class,
                () -> ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), TEST_CREDENTIALS_ID, null));
    }
}
