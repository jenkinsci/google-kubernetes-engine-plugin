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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.http.HttpTransport;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.ContainerClient;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

/** Tests {@link KubernetesEngineBuilder}. */
public class KubernetesEngineBuilderIT {
  private static final Logger LOGGER = Logger.getLogger(KubernetesEngineBuilderIT.class.getName());
  private static final String TEST_DEPLOYMENT_MANIFEST = "testDeployment.yml";
  private static final String TEST_DEPLOYMENT_MALFORMED_MANIFEST = "testMalformedDeployment.yml";

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  private static String clusterName;
  private static String projectId;
  private static String testZone;
  private static String credentialsId;
  private static ContainerClient client;

  private FreeStyleProject testJenkinsProject;
  private TemporaryFolder testWorkspace;

  @BeforeClass
  public static void init() throws Exception {
    LOGGER.info("Initializing KubernetesEngineBuilderIT");

    // setup test project ID
    projectId = System.getenv("GOOGLE_PROJECT_ID");
    assertNotNull("GOOGLE_PROJECT_ID env var must be set", projectId);

    // setup test zone
    testZone = System.getenv("GOOGLE_PROJECT_ZONE");
    assertNotNull("GOOGLE_PROJECT_ZONE env var must be set", testZone);

    // setup test cluster
    clusterName = System.getenv("GOOGLE_GKE_CLUSTER");
    assertNotNull("GOOGLE_GKE_CLUSTER env var must be set", clusterName);

    // setup test credentials
    LOGGER.info("Creating credentials");
    String serviceAccountKeyJson = System.getenv("GOOGLE_CREDENTIALS");
    assertNotNull("GOOGLE_CREDENTIALS env var must be set", serviceAccountKeyJson);
    credentialsId = projectId;
    ServiceAccountConfig sac = new StringJsonServiceAccountConfig(serviceAccountKeyJson);
    Credentials c = (Credentials) new GoogleRobotPrivateKeyCredentials(credentialsId, sac, null);
    CredentialsStore store =
        new SystemCredentialsProvider.ProviderImpl().getStore(jenkinsRule.jenkins);
    store.addCredentials(Domain.global(), c);

    // create the container client
    LOGGER.info("Creating container client");
    client =
        new ClientFactory(
                jenkinsRule.jenkins,
                ImmutableList.<DomainRequirement>of(),
                credentialsId,
                Optional.<HttpTransport>empty())
            .containerClient();
  }

  @Before
  public void createTestProject() throws Exception {
    String testProjectName = formatRandomName("test-jenkins");
    LOGGER.log(Level.INFO, "Creating test jenkins project: {0}", testProjectName);
    testJenkinsProject = jenkinsRule.createFreeStyleProject(testProjectName);
    testWorkspace = new TemporaryFolder();
    testWorkspace.create();
    testJenkinsProject.setCustomWorkspace(testWorkspace.getRoot().toString());
  }

  @After
  public void cleanupTestProject() throws Exception {
    LOGGER.log(Level.INFO, "Deleting test jenkins project: {0}", testJenkinsProject.getName());
    testJenkinsProject.delete();
    testWorkspace.delete();
  }

  @Test
  public void testServiceDeploymentSucceeds() throws Exception {
    LOGGER.info("Testing service deployment succeeds");
    // setup GKE Builder
    KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
    testJenkinsProject.getBuildersList().add(gkeBuilder);

    // copy test deployment into project workspace
    copyTestFileToDir(testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

    // execute a build
    FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
    dumpLog(build);
    assertEquals(Result.SUCCESS, build.getResult());
    // TODO(craigbarber): Add service verification
  }

  @Test
  public void testServiceDeploymentFailsBadCluster() throws Exception {
    LOGGER.info("Testing service deployment fails bad cluster");
    // setup GKE Builder
    KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
    gkeBuilder.setClusterName(formatRandomName("bad-cluster"));
    testJenkinsProject.getBuildersList().add(gkeBuilder);

    // copy test deployment into project workspace
    copyTestFileToDir(testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

    // execute a build
    FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
    dumpLog(build);
    assertEquals(Result.FAILURE, build.getResult());
  }

  @Test
  public void testServiceDeploymentFailsBadProjectId() throws Exception {
    LOGGER.info("Testing service deployment fails bad project id");
    // setup GKE Builder
    KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
    gkeBuilder.setProjectId(formatRandomName("bad-project"));
    testJenkinsProject.getBuildersList().add(gkeBuilder);

    // copy test deployment into project workspace
    copyTestFileToDir(testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

    // execute a build
    FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
    dumpLog(build);
    assertEquals(Result.FAILURE, build.getResult());
  }

  @Test
  public void testServiceDeploymentFailsBadCredentialId() throws Exception {
    LOGGER.info("Testing service deployment fails bad credential id");
    // setup GKE Builder
    KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
    gkeBuilder.setCredentialsId(formatRandomName("bad-credential"));
    testJenkinsProject.getBuildersList().add(gkeBuilder);

    // copy test deployment into project workspace
    copyTestFileToDir(testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

    // execute a build
    FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
    dumpLog(build);
    assertEquals(Result.FAILURE, build.getResult());
  }

  @Test
  public void testServiceDeploymentFailsBadManifestPattern() throws Exception {
    LOGGER.info("Testing service deployment fails bad manifest pattern");
    // setup GKE Builder
    KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
    gkeBuilder.setManifestPattern(formatRandomName("bad-manifest-pattern.yml"));
    testJenkinsProject.getBuildersList().add(gkeBuilder);

    // copy test deployment into project workspace
    copyTestFileToDir(testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

    // execute a build
    FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
    dumpLog(build);
    assertEquals(Result.FAILURE, build.getResult());
  }

  @Test
  public void testServiceDeploymentFailsMalformedManifest() throws Exception {
    LOGGER.info("Testing service fails malformed manifest");
    // setup GKE Builder
    KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
    gkeBuilder.setManifestPattern(TEST_DEPLOYMENT_MALFORMED_MANIFEST);
    testJenkinsProject.getBuildersList().add(gkeBuilder);

    // copy test deployment into project workspace
    copyTestFileToDir(testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MALFORMED_MANIFEST);

    // execute a build
    FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
    dumpLog(build);
    assertEquals(Result.FAILURE, build.getResult());
  }

  private static void copyTestFileToDir(String dir, String testFile)
      throws IOException, InterruptedException {
    FilePath dirPath = new FilePath(new File(dir));
    String testFileContents =
        Resources.toString(Resources.getResource(testFile), StandardCharsets.UTF_8);
    FilePath testWorkspaceFile = dirPath.child(testFile);
    testWorkspaceFile.write(testFileContents, StandardCharsets.UTF_8.toString());
  }

  private static String formatRandomName(String prefix) {
    return String.format("%s-%s", prefix, java.util.UUID.randomUUID().toString().replace("-", ""));
  }

  private static void dumpLog(Run<?, ?> run) throws IOException {
    BufferedReader reader = new BufferedReader(run.getLogReader());
    String line = null;
    while ((line = reader.readLine()) != null) {
      LOGGER.info(line);
    }
  }

  private static KubernetesEngineBuilder getDefaultGKEBuilder() {
    KubernetesEngineBuilder gkeBuilder = new KubernetesEngineBuilder();
    gkeBuilder.setProjectId(projectId);
    gkeBuilder.setClusterName(clusterName);
    gkeBuilder.setCredentialsId(credentialsId);
    gkeBuilder.setZone(testZone);
    gkeBuilder.setManifestPattern(TEST_DEPLOYMENT_MANIFEST);
    gkeBuilder.setAfterBuildStep(
        (kubeConfig, run, workspace, launcher, listener) ->
            KubectlWrapper.runKubectlCommand(
                new JenkinsRunContext.Builder()
                    .workspace(workspace)
                    .launcher(launcher)
                    .taskListener(listener)
                    .run(run)
                    .build(),
                kubeConfig,
                "delete",
                ImmutableList.<String>of(
                    "daemonsets,replicasets,services,deployments,pods,rc", "--all")));
    return gkeBuilder;
  }
}
