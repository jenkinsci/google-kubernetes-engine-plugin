package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertNotNull;
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
  private static final String TEST_INVALID_CREDENTIALS_ID = "test-invalid-credentials-id";

  @ClassRule public static JenkinsRule r = new JenkinsRule();

  public static Jenkins jenkins;

  @BeforeClass
  public static void init() throws IOException {
    jenkins = r.jenkins;

    CredentialsStore store = new SystemCredentialsProvider.ProviderImpl().getStore(jenkins);
    GoogleRobotCredentials credentials = Mockito.mock(GoogleRobotCredentials.class);
    credentials.getId();
    Mockito.when(credentials.getId()).thenReturn(TEST_CREDENTIALS_ID);
    store.addCredentials(Domain.global(), credentials);
  }

  @Test
  public void testGetRobotCredentialsReturnsFirstCredential() throws IOException {
    assertNotNull(
        CredentialsUtil.getRobotCredentials(
            jenkins.get(), ImmutableList.<DomainRequirement>of(), TEST_CREDENTIALS_ID));
  }

  @Test(expected = AbortException.class)
  public void testGetRobotCredentialsInvalidCredentialsIdAbortException() throws AbortException {
    CredentialsUtil.getRobotCredentials(
        jenkins.get(), ImmutableList.<DomainRequirement>of(), TEST_INVALID_CREDENTIALS_ID);
  }

  @Test(expected = AbortException.class)
  public void testGetGoogleCredentialAbortException()
      throws GeneralSecurityException, AbortException {

    GoogleRobotCredentials robotCreds = Mockito.mock(GoogleRobotCredentials.class);
    Mockito.when(robotCreds.getGoogleCredential(any(ContainerScopeRequirement.class)))
        .thenThrow(new GeneralSecurityException());
    CredentialsUtil.getGoogleCredential(robotCreds);
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

  @Test(expected = IOException.class)
  public void testGetAccessTokenIOException() throws IOException {
    GoogleCredential googleCredential = Mockito.mock(GoogleCredential.class);
    Mockito.when(googleCredential.refreshToken()).thenThrow(IOException.class);
    CredentialsUtil.getAccessToken(googleCredential);
  }

  @Test
  public void testGetAccessTokenReturnsToken() throws IOException {
    GoogleCredential googleCredential = Mockito.mock(GoogleCredential.class);
    Mockito.when(googleCredential.refreshToken()).thenReturn(true);
    Mockito.when(googleCredential.getAccessToken()).thenReturn("access token");
    assertNotNull(CredentialsUtil.getAccessToken(googleCredential));
  }

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

  @Test(expected = NullPointerException.class)
  public void testGetAccessTokenWithNullItemGroup() throws IOException {
    CredentialsUtil.getAccessToken(null, TEST_CREDENTIALS_ID);
  }

  @Test(expected = NullPointerException.class)
  public void testGetAccessTokenWithNullGoogleCredential() throws IOException {
    GoogleCredential googleCredential = null;
    CredentialsUtil.getAccessToken(googleCredential);
  }
}
