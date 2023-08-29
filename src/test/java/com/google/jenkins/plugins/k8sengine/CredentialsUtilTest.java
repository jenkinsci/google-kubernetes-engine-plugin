package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import hudson.AbortException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CredentialsUtilTest {
  private static final String TEST_CREDENTIALS_ID = "test-credentials-id";
  private static final String TEST_INVALID_CREDENTIALS_ID = "test-invalid-credentials-id";
  private static final String TEST_ACCESS_TOKEN = "test-access-token";
  @ClassRule public static JenkinsRule r = new JenkinsRule();

  @Test(expected = AbortException.class)
  public void testGetRobotCredentialsInvalidCredentialsIdAbortException() throws AbortException {
    CredentialsUtil.getRobotCredentials(
        r.jenkins, ImmutableList.<DomainRequirement>of(), TEST_INVALID_CREDENTIALS_ID);
  }

  @Test(expected = GoogleRobotPrivateKeyCredentials.PrivateKeyNotSetException.class)
  public void testGetGoogleCredentialAbortException() throws Exception {
    SecretBytes bytes =
        SecretBytes.fromBytes(
            "{\"client_email\": \"example@example.com\"}".getBytes(StandardCharsets.UTF_8));
    JsonServiceAccountConfig serviceAccountConfig = new JsonServiceAccountConfig();
    serviceAccountConfig.setSecretJsonKey(bytes);
    assertNotNull(serviceAccountConfig.getAccountId());
    GoogleRobotCredentials robotCreds =
        new GoogleRobotPrivateKeyCredentials(
            TEST_INVALID_CREDENTIALS_ID, serviceAccountConfig, null);
    CredentialsStore store = new SystemCredentialsProvider.ProviderImpl().getStore(r.jenkins);
    store.addCredentials(Domain.global(), robotCreds);
    CredentialsUtil.getGoogleCredential(robotCreds);
  }

  @Test(expected = NullPointerException.class)
  public void testGetRobotCredentialsWithEmptyItemGroup() throws AbortException {
    CredentialsUtil.getRobotCredentials(
        null, ImmutableList.<DomainRequirement>of(), TEST_CREDENTIALS_ID);
  }

  @Test(expected = NullPointerException.class)
  public void testGetRobotCredentialsWithEmptyDomainRequirements() throws AbortException {
    CredentialsUtil.getRobotCredentials(r.jenkins, null, TEST_CREDENTIALS_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRobotCredentialsWithNullCredentialsId() throws AbortException {
    CredentialsUtil.getRobotCredentials(r.jenkins, ImmutableList.<DomainRequirement>of(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRobotCredentialsWithEmptyCredentialsId() throws AbortException {
    CredentialsUtil.getRobotCredentials(r.jenkins, ImmutableList.<DomainRequirement>of(), "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetAccessTokenWithEmptyCredentialsId() throws IOException {
    CredentialsUtil.getAccessToken("");
  }

  @Test(expected = NullPointerException.class)
  public void testGetAccessTokenWithNullItemGroup() throws IOException {
    CredentialsUtil.getAccessToken(null, TEST_CREDENTIALS_ID);
  }

  @Test(expected = NullPointerException.class)
  public void testGetAccessTokenWithNullGoogleCredential() throws IOException {
    Credential googleCredential = null;
    CredentialsUtil.getAccessToken(googleCredential);
  }
}
