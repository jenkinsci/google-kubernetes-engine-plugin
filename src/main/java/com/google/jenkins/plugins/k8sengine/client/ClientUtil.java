package com.google.jenkins.plugins.k8sengine.client;

import static com.google.jenkins.plugins.k8sengine.CredentialsUtil.getGoogleCredential;
import static com.google.jenkins.plugins.k8sengine.CredentialsUtil.getRobotCredentials;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.cloud.graphite.platforms.plugin.client.ClientFactory;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.AbortException;
import hudson.model.ItemGroup;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;

/** Utilities for using the gcp-plugin-core clients. */
public class ClientUtil {

  private static final String APPLICATION_NAME = "jenkins-google-gke-plugin";

  /**
   * Creates a {@link ClientFactory} for generating the GCP api clients.
   *
   * @param itemGroup The Jenkins context to use for retrieving the credentials.
   * @param domainRequirements A list of domain requirements. Must be non-null.
   * @param credentialsId The ID of the credentials to use for generating clients.
   * @param transport An {@link Optional} parameter that specifies the {@link HttpTransport} to use.
   *     A default will be used if unspecified.
   * @return A {@link ClientFactory} to get clients.
   * @throws AbortException If there was an error initializing the ClientFactory.
   */
  public static ClientFactory getClientFactory(
      ItemGroup itemGroup,
      List<DomainRequirement> domainRequirements,
      String credentialsId,
      Optional<HttpTransport> transport)
      throws AbortException {
    Objects.requireNonNull(itemGroup);
    Objects.requireNonNull(domainRequirements);
    if (StringUtils.isBlank(credentialsId)) {
      throw new IllegalArgumentException("credentialsId cannot be null");
    }
    Objects.requireNonNull(transport);

    ClientFactory clientFactory;
    try {
      GoogleRobotCredentials robotCreds =
          getRobotCredentials(itemGroup, domainRequirements, credentialsId);
      Credential googleCredential = getGoogleCredential(robotCreds);
      clientFactory =
          new ClientFactory(
              transport, new RetryHttpInitializerWrapper(googleCredential), APPLICATION_NAME);
    } catch (IOException | GeneralSecurityException ex) {
      throw new AbortException(Messages.ClientFactory_FailedToInitializeHTTPTransport(ex));
    }
    return clientFactory;
  }

  /**
   * Creates a {@link ClientFactory} for generating the GCP api clients.
   *
   * @param itemGroup The Jenkins context to use for retrieving the credentials.
   * @param credentialsId The ID of the credentials to use for generating clients.
   * @return A {@link ClientFactory} to get clients.
   * @throws AbortException If there was an error initializing the ClientFactory.
   */
  public static ClientFactory getClientFactory(ItemGroup itemGroup, String credentialsId)
      throws AbortException {
    return getClientFactory(itemGroup, Collections.emptyList(), credentialsId, Optional.empty());
  }
}
