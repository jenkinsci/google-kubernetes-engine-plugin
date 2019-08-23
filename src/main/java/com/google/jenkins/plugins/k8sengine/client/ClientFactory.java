/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.jenkins.plugins.k8sengine.client;

import static com.google.jenkins.plugins.k8sengine.CredentialsUtil.getGoogleCredential;
import static com.google.jenkins.plugins.k8sengine.CredentialsUtil.getRobotCredentials;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.graphite.platforms.plugin.client.CloudResourceManagerClient;
import com.google.graphite.platforms.plugin.client.ContainerClient;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.AbortException;
import hudson.model.ItemGroup;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

/** Creates clients for communicating with Google APIs. */
public class ClientFactory {
  public static final String APPLICATION_NAME = "jenkins-google-gke-plugin";

  private final String credentialsId;
  private final String defaultProjectId;
  private final com.google.graphite.platforms.plugin.client.ClientFactory clientFactory;

  /**
   * Creates a {@link ClientFactory} instance.
   *
   * @param itemGroup A handle to the Jenkins instance.
   * @param domainRequirements A list of domain requirements.
   * @param credentialsId The ID of the GoogleRobotCredentials to be retrieved from Jenkins and
   *     utilized for authorization.
   * @param httpTransport If specified, the HTTP transport this factory will utilize for clients it
   *     creates.
   * @throws AbortException If failed to create a new client factory.
   */
  public ClientFactory(
      ItemGroup itemGroup,
      ImmutableList<DomainRequirement> domainRequirements,
      String credentialsId,
      Optional<HttpTransport> httpTransport)
      throws AbortException {
    GoogleRobotCredentials robotCreds =
        getRobotCredentials(itemGroup, domainRequirements, credentialsId);
    Credential credential = getGoogleCredential(robotCreds);
    this.defaultProjectId =
        Strings.isNullOrEmpty(robotCreds.getProjectId()) ? "" : robotCreds.getProjectId();
    this.credentialsId = credentialsId;

    HttpTransport transport;
    try {
      transport = httpTransport.orElse(GoogleNetHttpTransport.newTrustedTransport());
    } catch (GeneralSecurityException | IOException e) {
      throw new AbortException(
          Messages.ClientFactory_FailedToInitializeHTTPTransport(e.getMessage()));
    }

    try {
      this.clientFactory =
          new com.google.graphite.platforms.plugin.client.ClientFactory(
              Optional.ofNullable(transport), credential, APPLICATION_NAME);
    } catch (GeneralSecurityException | IOException e) {
      throw new AbortException(
          Messages.ClientFactory_FailedToInitializeHTTPTransport(e.getMessage()));
    }
  }

  /**
   * Creates a {@link ClientFactory} instance without specifying domainRequirements or
   * httpTransport.
   *
   * @param itemGroup A handle to the Jenkins instance.
   * @param credentialsId The ID of the GoogleRobotCredentials to be retrieved from Jenkins and
   *     utilized for authorization.
   * @throws AbortException If failed to create a new client factory.
   */
  public ClientFactory(ItemGroup itemGroup, String credentialsId) throws AbortException {
    this(itemGroup, ImmutableList.of(), credentialsId, Optional.empty());
  }

  /**
   * Creates a new {@link ContainerClient}.
   *
   * @return A new {@link ContainerClient} instance.
   */
  public ContainerClient containerClient() {
    return this.clientFactory.containerClient();
  }

  /**
   * Creates a new {@link CloudResourceManagerClient}.
   *
   * @return A new {@link CloudResourceManagerClient} instance.
   */
  public CloudResourceManagerClient cloudResourceManagerClient() {
    return this.clientFactory.cloudResourceManagerClient();
  }

  /** @return The default Project ID associated with this ClientFactory's credentials. */
  public String getDefaultProjectId() {
    return this.defaultProjectId;
  }

  /** @return The Credentials ID for this ClientFactory. */
  public String getCredentialsId() {
    return this.credentialsId;
  }
}
