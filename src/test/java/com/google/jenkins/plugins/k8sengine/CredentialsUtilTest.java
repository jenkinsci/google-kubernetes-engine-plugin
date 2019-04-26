package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;

import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.k8sengine.client.ContainerScopeRequirement;
import com.google.jenkins.plugins.k8sengine.client.Messages;
import hudson.AbortException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import jenkins.model.Jenkins;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CredentialsUtilTest {
  private static final String TEST_CREDENTIALS_ID = "test-credentials-id";
  private static final String TEST_EXCEPTION_MESSAGE = "TEST EXCEPTION MESSAGE";

  @ClassRule public static JenkinsRule r = new JenkinsRule();

  public static Jenkins jenkins;

  @BeforeClass
  public static void init() {
    jenkins = r.jenkins;
  }

  // todo: COMBINE error msg with null
  @Test(expected = AbortException.class)
  public void testGetRobotCredentialsReturnsNull() throws AbortException {
    GoogleRobotCredentials robotCreds =
        CredentialsUtil.getRobotCredentials(
            jenkins.get(), ImmutableList.<DomainRequirement>of(), TEST_CREDENTIALS_ID);
    assertNull(robotCreds);
  }

  @Test
  public void testGetRobotCredentialsReturnsFirstCredential() throws IOException {
    // can I just create a credential and then look for it? but then ill have to remove it
    CredentialsStore store = new SystemCredentialsProvider.ProviderImpl().getStore(r.jenkins);
    GoogleRobotCredentials credentials = Mockito.mock(GoogleRobotCredentials.class);
    credentials.getId();
    Mockito.when(credentials.getId()).thenReturn(TEST_CREDENTIALS_ID);
    store.addCredentials(Domain.global(), credentials);

    assertNotNull(
        CredentialsUtil.getRobotCredentials(
            r.jenkins.get(), ImmutableList.<DomainRequirement>of(), TEST_CREDENTIALS_ID));
    store.removeCredentials(Domain.global(), credentials);
  }

  @Test
  public void testGetRobotCredentialsErrorMessageWithAbortException() {
    try {
      CredentialsUtil.getRobotCredentials(
          jenkins.get(), ImmutableList.<DomainRequirement>of(), TEST_CREDENTIALS_ID);
    } catch (AbortException e) {
      assertEquals(
          Messages.ClientFactory_FailedToRetrieveCredentials(TEST_CREDENTIALS_ID), e.getMessage());
    }
  }

  @Test
  public void testGetGoogleCredentialThrowsAbortException() throws GeneralSecurityException {
    try {
      GoogleRobotCredentials robotCreds = Mockito.mock(GoogleRobotCredentials.class);
      Mockito.when(robotCreds.getGoogleCredential(any(ContainerScopeRequirement.class)))
          .thenThrow(new GeneralSecurityException(TEST_EXCEPTION_MESSAGE));
      CredentialsUtil.getGoogleCredential(robotCreds);
    } catch (AbortException e) {
      assertEquals(
          Messages.ClientFactory_FailedToInitializeHTTPTransport(TEST_EXCEPTION_MESSAGE),
          e.getMessage());
    }
  }

  @Test
  public void testGetGoogleCredentialReturnsCredential()
      throws GeneralSecurityException, AbortException {
    GoogleRobotCredentials robotCreds = Mockito.mock(GoogleRobotCredentials.class);
    Credential credential = Mockito.mock(Credential.class);
    Mockito.when(robotCreds.getGoogleCredential(any(ContainerScopeRequirement.class)))
        .thenReturn(credential);
    assertNotNull(CredentialsUtil.getGoogleCredential(robotCreds));
  }

  // TODO: how to test getAccesstoken
  // TODO: move the credentials out
  @Test(expected = IOException.class)
  public void testGetAccessTokenIOException() throws IOException {
    GoogleCredential googleCredential = Mockito.mock(GoogleCredential.class);
    Mockito.when(googleCredential.refreshToken()).thenThrow(IOException.class);
    CredentialsUtil.getAccessToken(googleCredential);
  }

  @Test
  public void testGetAccessTokenReturnToken() throws IOException {
    GoogleCredential googleCredential = Mockito.mock(GoogleCredential.class);
    Mockito.when(googleCredential.refreshToken()).thenReturn(true);
    Mockito.when(googleCredential.getAccessToken()).thenReturn("access token");
    assertNotNull(CredentialsUtil.getAccessToken(googleCredential));
  }

  // TODO: do I need these tests testing preconditions
  @Test(expected = NullPointerException.class)
  public void testGetRobotCredentialsWithEmptyItemGroup() throws AbortException {
    CredentialsUtil.getRobotCredentials(
        null, ImmutableList.<DomainRequirement>of(), TEST_CREDENTIALS_ID);
  }

  @Test(expected = NullPointerException.class)
  public void testGetRobotCredentialsWithEmptyDomainRequirements() throws AbortException {
    CredentialsUtil.getRobotCredentials(jenkins.get(), null, TEST_CREDENTIALS_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRobotCredentialsWithNullCredentialsId() throws AbortException {
    CredentialsUtil.getRobotCredentials(jenkins.get(), ImmutableList.<DomainRequirement>of(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetRobotCredentialsWithEmptyCredentialsId() throws AbortException {
    CredentialsUtil.getRobotCredentials(jenkins.get(), ImmutableList.<DomainRequirement>of(), "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetAccessTokenWithEmptyCredentialsId() throws IOException {
    CredentialsUtil.getAccessToken("");
  }
}
