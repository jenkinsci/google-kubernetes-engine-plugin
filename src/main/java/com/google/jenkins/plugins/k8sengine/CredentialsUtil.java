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

package com.google.jenkins.plugins.k8sengine;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.k8sengine.client.ContainerScopeRequirement;
import com.google.jenkins.plugins.k8sengine.client.Messages;
import hudson.AbortException;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import java.io.IOException;
import java.security.GeneralSecurityException;
import jenkins.model.Jenkins;

/** Provides a library of utility functions for credentials-related work. */
public class CredentialsUtil {
  /**
   * Get the Google Robot Credentials for the given credentialsId.
   *
   * @param itemGroup A handle to the Jenkins instance.
   * @param domainRequirements A list of domain requirements.
   * @param credentialsId The ID of the GoogleRobotCredentials to be retrieved from Jenkins and
   *     utilized for authorization.
   * @return Google Robot Credential for the given credentialsId.
   * @throws AbortException If there was an issue retrieving the Google Robot Credentials.
   */
  public static GoogleRobotCredentials getRobotCredentials(
      ItemGroup itemGroup,
      ImmutableList<DomainRequirement> domainRequirements,
      String credentialsId)
      throws AbortException {
    Preconditions.checkNotNull(itemGroup);
    Preconditions.checkNotNull(domainRequirements);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(credentialsId));

    GoogleRobotCredentials robotCreds =
        CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(
                GoogleRobotCredentials.class, itemGroup, ACL.SYSTEM, domainRequirements),
            CredentialsMatchers.withId(credentialsId));

    if (robotCreds == null) {
      // TODO: Change the  message names since not in clientfactory anymore
      throw new AbortException(
          com.google.jenkins.plugins.k8sengine.client.Messages
              .ClientFactory_FailedToRetrieveCredentials(credentialsId));
    }

    return robotCreds;
  }

  /**
   * Get the Credential from the Google robot credentials for GKE access.
   *
   * @param robotCreds Google Robot Credential for desired service account.
   * @return Google Credential for the service account.
   * @throws AbortException if there was an error initializing HTTP transport.
   */
  public static Credential getGoogleCredential(GoogleRobotCredentials robotCreds)
      throws AbortException {
    Credential credential;
    try {
      credential = robotCreds.getGoogleCredential(new ContainerScopeRequirement());
    } catch (GeneralSecurityException gse) {
      throw new AbortException(
          Messages.ClientFactory_FailedToInitializeHTTPTransport(gse.getMessage()));
    }

    return credential;
  }

  /**
   * Get access token for service account with this credentialsId.
   *
   * @param credentialsId The service account credential's id.
   * @return Access token from OAuth to allow kubectl to interact with the cluster.
   * @throws java.io.IOException If an error occurred fetching the access token.
   */
  static String getAccessToken(String credentialsId) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(credentialsId));
    GoogleRobotCredentials robotCreds =
        getRobotCredentials(Jenkins.get(), ImmutableList.<DomainRequirement>of(), credentialsId);

    GoogleCredential googleCredentials = (GoogleCredential) getGoogleCredential(robotCreds);
    googleCredentials.refreshToken();
    return googleCredentials.getAccessToken();
  }
}
