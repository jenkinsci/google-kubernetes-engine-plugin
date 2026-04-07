package com.google.jenkins.plugins.k8sengine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import hudson.AbortException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CredentialsUtilTest {

    private static final String TEST_CREDENTIALS_ID = "test-credentials-id";
    private static final String TEST_INVALID_CREDENTIALS_ID = "test-invalid-credentials-id";

    private static JenkinsRule r;

    @BeforeAll
    static void init(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void testGetRobotCredentialsInvalidCredentialsIdAbortException() {
        assertThrows(
                AbortException.class,
                () -> CredentialsUtil.getRobotCredentials(r.jenkins, ImmutableList.of(), TEST_INVALID_CREDENTIALS_ID));
    }

    @Test
    void testGetGoogleCredentialAbortException() throws Exception {
        SecretBytes bytes =
                SecretBytes.fromBytes("{\"client_email\": \"example@example.com\"}".getBytes(StandardCharsets.UTF_8));
        JsonServiceAccountConfig serviceAccountConfig = new JsonServiceAccountConfig();
        serviceAccountConfig.setSecretJsonKey(bytes);
        assertNotNull(serviceAccountConfig.getAccountId());
        GoogleRobotCredentials robotCreds =
                new GoogleRobotPrivateKeyCredentials(TEST_INVALID_CREDENTIALS_ID, serviceAccountConfig, null);
        CredentialsStore store = new SystemCredentialsProvider.ProviderImpl().getStore(r.jenkins);
        store.addCredentials(Domain.global(), robotCreds);
        assertThrows(
                GoogleRobotPrivateKeyCredentials.PrivateKeyNotSetException.class,
                () -> CredentialsUtil.getGoogleCredential(robotCreds));
    }

    @Test
    void testGetRobotCredentialsWithEmptyItemGroup() {
        assertThrows(
                NullPointerException.class,
                () -> CredentialsUtil.getRobotCredentials(null, ImmutableList.of(), TEST_CREDENTIALS_ID));
    }

    @Test
    void testGetRobotCredentialsWithEmptyDomainRequirements() {
        assertThrows(
                NullPointerException.class,
                () -> CredentialsUtil.getRobotCredentials(r.jenkins, null, TEST_CREDENTIALS_ID));
    }

    @Test
    void testGetRobotCredentialsWithNullCredentialsId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CredentialsUtil.getRobotCredentials(r.jenkins, ImmutableList.of(), null));
    }

    @Test
    void testGetRobotCredentialsWithEmptyCredentialsId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CredentialsUtil.getRobotCredentials(r.jenkins, ImmutableList.of(), ""));
    }

    @Test
    void testGetAccessTokenWithEmptyCredentialsId() {
        assertThrows(IllegalArgumentException.class, () -> CredentialsUtil.getAccessToken(""));
    }

    @Test
    void testGetAccessTokenWithNullItemGroup() {
        assertThrows(NullPointerException.class, () -> CredentialsUtil.getAccessToken(null, TEST_CREDENTIALS_ID));
    }

    @Test
    void testGetAccessTokenWithNullGoogleCredential() {
        Credential googleCredential = null;
        assertThrows(NullPointerException.class, () -> CredentialsUtil.getAccessToken(googleCredential));
    }
}
