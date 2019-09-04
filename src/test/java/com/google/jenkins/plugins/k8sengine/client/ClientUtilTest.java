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

/** Test suite for {@link ClientUtil}. */
public class ClientUtilTest {
  private static Logger LOGGER = Logger.getLogger(ClientUtilTest.class.getName());

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  private static String projectId;
  private static String credentialsId;

  @BeforeClass
  public static void init() throws Exception {
    LOGGER.info("Starting ClientUtil test.");
    projectId = System.getenv("GOOGLE_PROJECT_ID");
    assertNotNull("GOOGLE_PROJECT_ID env var must be set", projectId);
    ServiceAccountConfig sac = getServiceAccountConfig();
    credentialsId = projectId;
    Credentials c = new GoogleRobotPrivateKeyCredentials(credentialsId, sac, null);
    CredentialsStore store =
        new SystemCredentialsProvider.ProviderImpl().getStore(jenkinsRule.jenkins);
    store.addCredentials(Domain.global(), c);
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryNullJenkins() throws AbortException {
    ClientUtil.getClientFactory(null, ImmutableList.of(), credentialsId, Optional.empty());
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryShortNullJenkins() throws AbortException {
    ClientUtil.getClientFactory(null, credentialsId);
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryNullDomainRequirements() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, null, credentialsId, Optional.empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryNullCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), null, Optional.empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryShortNullCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryEmptyCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), "", Optional.empty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClientFactoryShortEmptyCredentialsId() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, "");
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryTransportNull() throws AbortException {
    ClientUtil.getClientFactory(jenkinsRule.jenkins, ImmutableList.of(), credentialsId, null);
  }

  @Test
  public void testGetClientFactoryValidCreds() throws AbortException {
    ClientFactory factory =
        ClientUtil.getClientFactory(
            jenkinsRule.jenkins, ImmutableList.of(), credentialsId, Optional.empty());
    assertNotNull(factory);
    assertNotNull(factory.computeClient());
  }

  @Test
  public void testGetClientFactoryShortValidCreds() throws AbortException {
    ClientFactory factory = ClientUtil.getClientFactory(jenkinsRule.jenkins, credentialsId);
    assertNotNull(factory);
    assertNotNull(factory.computeClient());
  }

  @Test(expected = NullPointerException.class)
  public void testGetClientFactoryFailsWithInvalidCredentialsId() throws Throwable {
    try {
      ClientUtil.getClientFactory(
          jenkinsRule.jenkins, ImmutableList.of(), credentialsId + "fake", Optional.empty());
    } catch (AbortException e) {
      throw e.getCause();
    }
  }
}
