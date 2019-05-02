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

import static com.google.jenkins.plugins.k8sengine.ITUtil.copyTestFileToDir;
import static com.google.jenkins.plugins.k8sengine.ITUtil.createTestWorkspace;
import static com.google.jenkins.plugins.k8sengine.ITUtil.dumpLog;
import static com.google.jenkins.plugins.k8sengine.ITUtil.formatRandomName;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.http.HttpTransport;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.ContainerClient;
import com.jayway.jsonpath.JsonPath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/** Tests {@link KubernetesEngineBuilder}. */
public class KubernetesEngineBuilderIT {
  private static final Logger LOGGER = Logger.getLogger(KubernetesEngineBuilderIT.class.getName());
  private static final String TEST_DEPLOYMENT_MANIFEST = "testDeployment.yml";
  private static final String TEST_DEPLOYMENT_MALFORMED_MANIFEST = "testMalformedDeployment.yml";
  private static final String TEST_DEPLOYMENT_UNVERIFIABLE_MANIFEST =
      "testUnverifiableDeployment.yml";

  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();

  private static String clusterName;
  private static String projectId;
  private static String testZone;
  private static String credentialsId;
  private static ContainerClient client;

  @BeforeClass
  public static void init() throws Exception {
    LOGGER.info("Initializing KubernetesEngineBuilderIT");

    projectId = System.getenv("GOOGLE_PROJECT_ID");
    assertNotNull("GOOGLE_PROJECT_ID env var must be set", projectId);

    testZone = System.getenv("GOOGLE_PROJECT_ZONE");
    assertNotNull("GOOGLE_PROJECT_ZONE env var must be set", testZone);

    clusterName = System.getenv("GOOGLE_GKE_CLUSTER");
    assertNotNull("GOOGLE_GKE_CLUSTER env var must be set", clusterName);

    LOGGER.info("Creating credentials");
    String serviceAccountKeyJson = System.getenv("GOOGLE_CREDENTIALS");
    assertNotNull("GOOGLE_CREDENTIALS env var must be set", serviceAccountKeyJson);
    credentialsId = projectId;
    ServiceAccountConfig sac = new StringJsonServiceAccountConfig(serviceAccountKeyJson);
    Credentials c = (Credentials) new GoogleRobotPrivateKeyCredentials(credentialsId, sac, null);
    CredentialsStore store =
        new SystemCredentialsProvider.ProviderImpl().getStore(jenkinsRule.jenkins);
    store.addCredentials(Domain.global(), c);

    LOGGER.info("Creating container client");
    client =
        new ClientFactory(
                jenkinsRule.jenkins,
                ImmutableList.<DomainRequirement>of(),
                credentialsId,
                Optional.<HttpTransport>empty())
            .containerClient();
  }

  @Test
  public void testServiceDeploymentSucceeds() throws Exception {
    LOGGER.info("Testing service deployment succeeds");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      copyTestFileToDir(
          getClass(), testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);
      gkeBuilder.pushAfterBuildStep(
          (kubeConfig, run, workspace, launcher, listener) -> {
            KubectlWrapper kubectl =
                new KubectlWrapper.Builder()
                    .workspace(workspace)
                    .launcher(launcher)
                    .kubeConfig(kubeConfig)
                    .namespace("default")
                    .build();
            Object json = kubectl.getObject("deployment", "nginx-deployment");
            Map<String, Object> labels = JsonPath.read(json, "metadata.labels");
            assertNotNull(labels);
            assertNotNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
            String labelValues = (String) labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY);
            assertTrue(
                Arrays.asList(labelValues.split(","))
                    .contains(KubernetesEngineBuilder.METRICS_LABEL_VALUE));
          });

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatusSuccess(jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  @Test
  public void testWellFormedFailedDeploymentNotVerified() throws Exception {
    LOGGER.info("Testing well-formed unverifiable deployment fails verification");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      gkeBuilder.setManifestPattern(TEST_DEPLOYMENT_UNVERIFIABLE_MANIFEST);
      copyTestFileToDir(
          getClass(),
          testJenkinsProject.getCustomWorkspace(),
          TEST_DEPLOYMENT_UNVERIFIABLE_MANIFEST);

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  @Test
  public void testServiceDeploymentFailsBadCluster() throws Exception {
    LOGGER.info("Testing service deployment fails bad cluster");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      gkeBuilder.setClusterName(formatRandomName("bad-cluster"));
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      copyTestFileToDir(
          getClass(), testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  @Test
  public void testServiceDeploymentFailsBadProjectId() throws Exception {
    LOGGER.info("Testing service deployment fails bad project id");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      gkeBuilder.setProjectId(formatRandomName("bad-project"));
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      copyTestFileToDir(
          getClass(), testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  @Test
  public void testServiceDeploymentFailsBadCredentialId() throws Exception {
    LOGGER.info("Testing service deployment fails bad credential id");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      gkeBuilder.setCredentialsId(formatRandomName("bad-credential"));
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      copyTestFileToDir(
          getClass(), testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  @Test
  public void testServiceDeploymentFailsBadManifestPattern() throws Exception {
    LOGGER.info("Testing service deployment fails bad manifest pattern");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      gkeBuilder.setManifestPattern(formatRandomName("bad-manifest-pattern.yml"));
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      copyTestFileToDir(
          getClass(), testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MANIFEST);

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  @Test
  public void testServiceDeploymentFailsMalformedManifest() throws Exception {
    LOGGER.info("Testing service fails malformed manifest");
    FreeStyleProject testJenkinsProject =
        jenkinsRule.createFreeStyleProject(formatRandomName("test"));
    createTestWorkspace(testJenkinsProject);
    try {
      KubernetesEngineBuilder gkeBuilder = getDefaultGKEBuilder();
      gkeBuilder.setManifestPattern(TEST_DEPLOYMENT_MALFORMED_MANIFEST);
      testJenkinsProject.getBuildersList().add(gkeBuilder);
      copyTestFileToDir(
          getClass(), testJenkinsProject.getCustomWorkspace(), TEST_DEPLOYMENT_MALFORMED_MANIFEST);

      FreeStyleBuild build = testJenkinsProject.scheduleBuild2(0).get();
      assertNotNull(build);
      jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(build));
      dumpLog(LOGGER, build);
    } finally {
      testJenkinsProject.delete();
    }
  }

  private static KubernetesEngineBuilder getDefaultGKEBuilder() {
    KubernetesEngineBuilder gkeBuilder = new KubernetesEngineBuilder();
    gkeBuilder.setProjectId(projectId);
    gkeBuilder.setClusterName(clusterName);
    gkeBuilder.setCredentialsId(credentialsId);
    gkeBuilder.setZone(testZone);
    gkeBuilder.setNamespace("*");
    gkeBuilder.setManifestPattern(TEST_DEPLOYMENT_MANIFEST);
    gkeBuilder.setVerifyDeployments(true);
    gkeBuilder.setVerifyTimeoutInMinutes(1);
    gkeBuilder.pushAfterBuildStep(
        (kubeConfig, run, workspace, launcher, listener) -> {
          KubectlWrapper kubectl =
              new KubectlWrapper.Builder()
                  .workspace(workspace)
                  .kubeConfig(kubeConfig)
                  .launcher(launcher)
                  .namespace("default")
                  .build();
          Set<String> objectKinds = new HashSet<>();
          Manifests manifests =
              Manifests.fromFile(workspace.child(gkeBuilder.getManifestPattern()));
          manifests.getObjectManifests().stream().forEach(mo -> objectKinds.add(mo.getKind()));
          for (String kind : objectKinds) {
            kubectl.runKubectlCommand(
                "delete",
                new ImmutableList.Builder<String>()
                    .add(kind.toLowerCase())
                    .addAll(
                        manifests.getObjectManifests().stream()
                            .filter(mo -> mo.getName().isPresent())
                            .map(mo -> mo.getName().get())
                            .collect(Collectors.toList()))
                    .build());
          }
        });
    return gkeBuilder;
  }
}
