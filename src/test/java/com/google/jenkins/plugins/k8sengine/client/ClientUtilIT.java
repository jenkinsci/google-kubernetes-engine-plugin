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

import static com.google.jenkins.plugins.k8sengine.ITUtil.getServiceAccountConfig;
import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.google.cloud.graphite.platforms.plugin.client.ClientFactory;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import hudson.AbortException;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/** Integration tests for {@link ClientUtil}. * */
public class ClientUtilIT {

  private static Logger LOGGER = Logger.getLogger(ClientUtilIT.class.getName());

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  private static String credentialsId;

  @BeforeClass
  public static void init() throws Exception {
    LOGGER.info("Starting ClientUtil test.");
    String projectId = System.getenv("GOOGLE_PROJECT_ID");
    assertNotNull("GOOGLE_PROJECT_ID env var must be set", projectId);
    ServiceAccountConfig sac = getServiceAccountConfig();
    credentialsId = projectId;
    Credentials c = new GoogleRobotPrivateKeyCredentials(credentialsId, sac, null);
    CredentialsStore store =
        new SystemCredentialsProvider.ProviderImpl().getStore(jenkinsRule.jenkins);
    store.addCredentials(Domain.global(), c);
  }

  @Test
  public void testGetClientFactoryValidCreds() throws AbortException {
    ClientFactory factory =
        ClientUtil.getClientFactory(
            jenkinsRule.jenkins, ImmutableList.of(), credentialsId, Optional.empty());
    assertNotNull(factory);
    assertNotNull(factory.containerClient());
  }

  @Test
  public void testGetClientFactoryShortValidCreds() throws AbortException {
    ClientFactory factory = ClientUtil.getClientFactory(jenkinsRule.jenkins, credentialsId);
    assertNotNull(factory);
    assertNotNull(factory.containerClient());
  }
}
