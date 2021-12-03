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

import hudson.AbortException;
import java.util.Collections;
import java.util.Optional;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/** Test suite for {@link ClientUtil}. */
public class ClientUtilTest {

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  private static final String TEST_CREDENTIALS_ID = "test-project";

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryNullJenkins() throws AbortException {
    ClientUtil.getClientFactory(
        null, Collections.emptyList(), TEST_CREDENTIALS_ID, Optional.empty());
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryShortNullJenkins() throws AbortException {
    ClientUtil.getClientFactory(null, TEST_CREDENTIALS_ID);
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryNullDomainRequirements() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, null, TEST_CREDENTIALS_ID, Optional.empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryNullCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(
        jenkinsRule.jenkins, Collections.emptyList(), null, Optional.empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryShortNullCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryEmptyCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, Collections.emptyList(), "", Optional.empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryShortEmptyCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, "");
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryFailsWithInvalidCredentialsId() throws Throwable {
    try {
      ClientUtil.getClientFactory(
          jenkinsRule.jenkins, Collections.emptyList(), "fake", Optional.empty());
    } catch (AbortException e) {
      throw e.getCause();
    }
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryShortFailsWithInvalidCredentialsId() throws Throwable {
    try {
      ClientUtil.getClientFactory(jenkinsRule.jenkins, "fake");
    } catch (AbortException e) {
      throw e.getCause();
    }
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryTransportNull() throws AbortException {
    ClientUtil.getClientFactory(
        jenkinsRule.jenkins, Collections.emptyList(), TEST_CREDENTIALS_ID, null);
  }
}
