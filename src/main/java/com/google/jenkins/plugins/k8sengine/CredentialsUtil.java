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
   * @param itemGroup A handle to the Jenkins instance. Must be non-null.
   * @param domainRequirements A list of domain requirements. Must be non-null.
   * @param credentialsId The ID of the GoogleRobotCredentials to be retrieved from Jenkins and
   *     utilized for authorization. Must be non-empty or non-null and exist in credentials store.
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
      throw new AbortException(Messages.ClientFactory_FailedToRetrieveCredentials(credentialsId));
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
   * Given a credentialsId and Jenkins context, returns the access token.
   *
   * @param itemGroup A handle to the Jenkins instance. Must be non-null.
   * @param credentialsId The service account credential's id. Must be non-null.
   * @return Access token from OAuth to allow kubectl to interact with the cluster.
   * @throws IOException If an error occurred fetching the access token.
   */
  static String getAccessToken(ItemGroup itemGroup, String credentialsId) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(credentialsId));
    Preconditions.checkNotNull(itemGroup);
    GoogleRobotCredentials robotCreds =
        getRobotCredentials(itemGroup, ImmutableList.of(), credentialsId);

    Credential googleCredential = getGoogleCredential(robotCreds);
    return getAccessToken(googleCredential);
  }
  /**
   * Wrapper to get access token for service account with this credentialsId. Uses Jenkins.get() as
   * context.
   *
   * @param credentialsId The service account credential's id. Must be non-null.
   * @return Access token from OAuth to allow kubectl to interact with the cluster.
   * @throws IOException If an error occurred fetching the access token.
   */
  static String getAccessToken(String credentialsId) throws IOException {
    return getAccessToken(Jenkins.get(), credentialsId);
  }

  /**
   * Given the Google Credential, retrieve the access token.
   *
   * @param googleCredential Google Credential to get an access token. Must be non-null.
   * @return Access token from OAuth to allow kubectl to interact with the cluster.
   * @throws IOException If an error occured fetching the access token.
   */
  static String getAccessToken(Credential googleCredential) throws IOException {
    Preconditions.checkNotNull(googleCredential);

    googleCredential.refreshToken();
    return googleCredential.getAccessToken();
  }

  /**
   * Given a credentialsId and Jenkins context, return the default project ID for the service
   * account credentials specified.
   *
   * @param itemGroup A handle to the Jenkins instance. Must be non-null.
   * @param credentialsId The service account credential's ID. Must be non-null.
   * @return The project ID specified when creating the credential in the Jenkins credential store.
   * @throws AbortException If an error occurred fetching the credential.
   */
  static String getDefaultProjectId(ItemGroup itemGroup, String credentialsId)
      throws AbortException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(credentialsId));
    Preconditions.checkNotNull(itemGroup);
    GoogleRobotCredentials robotCreds =
        getRobotCredentials(itemGroup, ImmutableList.of(), credentialsId);
    return Strings.isNullOrEmpty(robotCreds.getProjectId()) ? "" : robotCreds.getProjectId();
  }

  /**
   * Wrapper to get the default project ID for a credential using Jenkins.get() as the context.
   *
   * @param credentialsId The service account credential's ID. Must be non-null.
   * @return The project ID specified when creating the credential in the Jenkins credential store.
   * @throws AbortException If an error occurred fetching the credential.
   */
  static String getDefaultProjectId(String credentialsId) throws AbortException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(credentialsId));
    return getDefaultProjectId(Jenkins.get(), credentialsId);
  }
}
