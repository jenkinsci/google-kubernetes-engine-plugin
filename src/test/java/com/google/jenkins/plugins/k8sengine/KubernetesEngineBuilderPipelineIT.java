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
import static com.google.jenkins.plugins.k8sengine.ITUtil.dumpLog;
import static com.google.jenkins.plugins.k8sengine.ITUtil.formatRandomName;
import static com.google.jenkins.plugins.k8sengine.ITUtil.getLocation;
import static com.google.jenkins.plugins.k8sengine.ITUtil.getServiceAccountConfig;
import static com.google.jenkins.plugins.k8sengine.ITUtil.loadResource;
import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.google.api.services.container.model.Cluster;
import com.google.cloud.graphite.platforms.plugin.client.ContainerClient;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import com.google.jenkins.plugins.k8sengine.client.ClientUtil;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/** Tests the {@link KubernetesEngineBuilder} for use-cases involving the Jenkins Pipeline DSL. */
public class KubernetesEngineBuilderPipelineIT {
  private static final Logger LOGGER =
      Logger.getLogger(KubernetesEngineBuilderPipelineIT.class.getName());
  private static final String TEST_DEPLOYMENT_MANIFEST = "testDeployment.yml";
  @ClassRule public static JenkinsRule jenkinsRule = new JenkinsRule();
  private static EnvVars envVars;
  private static String clusterName;
  private static String projectId;
  private static String testLocation;
  private static String credentialsId;
  private static ContainerClient client;

  @BeforeClass
  public static void init() throws Exception {
    LOGGER.info("Initializing KubernetesEngineBuilderPipelineIT");

    projectId = System.getenv("GOOGLE_PROJECT_ID");
    assertNotNull("GOOGLE_PROJECT_ID env var must be set", projectId);

    testLocation = getLocation();

    clusterName = System.getenv("GOOGLE_GKE_CLUSTER");
    assertNotNull("GOOGLE_GKE_CLUSTER env var must be set", clusterName);

    LOGGER.info("Creating credentials");
    ServiceAccountConfig sac = getServiceAccountConfig();
    credentialsId = projectId;
    Credentials c = (Credentials) new GoogleRobotPrivateKeyCredentials(credentialsId, sac, null);
    CredentialsStore store =
        new SystemCredentialsProvider.ProviderImpl().getStore(jenkinsRule.jenkins);
    store.addCredentials(Domain.global(), c);

    client = ClientUtil.getClientFactory(jenkinsRule.jenkins, credentialsId).containerClient();

    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    envVars = prop.getEnvVars();
    envVars.put("PROJECT_ID", projectId);
    envVars.put("CLUSTER_NAME", clusterName);
    envVars.put("CREDENTIALS_ID", credentialsId);
    envVars.put("LOCATION", testLocation);
    jenkinsRule.jenkins.getGlobalNodeProperties().add(prop);
  }

  @Test
  public void testWorkspaceDeclarativePipelineDeploysProperly() throws Exception {
    envVars.put("MANIFEST_PATTERN", TEST_DEPLOYMENT_MANIFEST);
    envVars.put("NAMESPACE", "default");
    WorkflowJob testProject =
        jenkinsRule.createProject(WorkflowJob.class, formatRandomName("test"));
    testProject.setDefinition(
        new CpsFlowDefinition(
            loadResource(getClass(), "workspaceDeclarativePipeline.groovy"), true));
    copyTestFileToDir(
        getClass(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject).getRemote(),
        TEST_DEPLOYMENT_MANIFEST);

    WorkflowRun run = testProject.scheduleBuild2(0).waitForStart();
    assertNotNull(run);
    jenkinsRule.assertBuildStatusSuccess(jenkinsRule.waitForCompletion(run));
    dumpLog(LOGGER, run);

    kubectlDelete(
        jenkinsRule.createLocalLauncher(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject),
        TEST_DEPLOYMENT_MANIFEST,
        "deployment",
        "nginx-deployment",
        "default");
  }

  @Test
  public void testGitDeclarativePipelineDeploysProperly() throws Exception {
    envVars.put("GIT_URL", "https://github.com/jenkinsci/google-kubernetes-engine-plugin.git");
    envVars.put("MANIFEST_PATTERN", "docs/resources/manifest.yaml");
    envVars.put("NAMESPACE", "default");
    WorkflowJob testProject =
        jenkinsRule.createProject(WorkflowJob.class, formatRandomName("test"));
    testProject.setDefinition(
        new CpsFlowDefinition(loadResource(getClass(), "gitDeclarativePipeline.groovy"), true));

    WorkflowRun run = testProject.scheduleBuild2(0).waitForStart();
    assertNotNull(run);
    jenkinsRule.assertBuildStatusSuccess(jenkinsRule.waitForCompletion(run));
    dumpLog(LOGGER, run);

    kubectlDelete(
        jenkinsRule.createLocalLauncher(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject),
        "docs/resources/manifest.yaml",
        "deployment",
        "nginx-deployment",
        "default");
  }

  @Test
  public void testMalformedDeclarativePipelineFails() throws Exception {
    envVars.put("MANIFEST_PATTERN", TEST_DEPLOYMENT_MANIFEST);
    envVars.put("NAMESPACE", "default");
    WorkflowJob testProject =
        jenkinsRule.createProject(WorkflowJob.class, formatRandomName("test"));
    testProject.setDefinition(
        new CpsFlowDefinition(
            loadResource(getClass(), "malformedDeclarativePipeline.groovy"), true));
    copyTestFileToDir(
        getClass(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject).getRemote(),
        TEST_DEPLOYMENT_MANIFEST);

    WorkflowRun run = testProject.scheduleBuild2(0).waitForStart();
    assertNotNull(run);
    jenkinsRule.assertBuildStatus(Result.FAILURE, jenkinsRule.waitForCompletion(run));
    dumpLog(LOGGER, run);
  }

  @Test
  public void testNoNamespaceDeclarativePipelineDeploysProperly() throws Exception {
    envVars.put("MANIFEST_PATTERN", TEST_DEPLOYMENT_MANIFEST);
    WorkflowJob testProject =
        jenkinsRule.createProject(WorkflowJob.class, formatRandomName("test"));
    testProject.setDefinition(
        new CpsFlowDefinition(
            loadResource(getClass(), "noNamespaceDeclarativePipeline.groovy"), true));
    copyTestFileToDir(
        getClass(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject).getRemote(),
        TEST_DEPLOYMENT_MANIFEST);

    WorkflowRun run = testProject.scheduleBuild2(0).waitForStart();
    assertNotNull(run);
    jenkinsRule.assertBuildStatus(Result.SUCCESS, jenkinsRule.waitForCompletion(run));
    dumpLog(LOGGER, run);

    kubectlDelete(
        jenkinsRule.createLocalLauncher(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject),
        TEST_DEPLOYMENT_MANIFEST,
        "deployment",
        "nginx-deployment",
        "");
  }

  @Test
  public void testCustomNamespaceDeclarativePipelineDeploysProperly() throws Exception {
    envVars.put("MANIFEST_PATTERN", TEST_DEPLOYMENT_MANIFEST);
    envVars.put("NAMESPACE", "test");
    WorkflowJob testProject =
        jenkinsRule.createProject(WorkflowJob.class, formatRandomName("test"));
    testProject.setDefinition(
        new CpsFlowDefinition(
            loadResource(getClass(), "workspaceDeclarativePipeline.groovy"), true));
    copyTestFileToDir(
        getClass(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject).getRemote(),
        TEST_DEPLOYMENT_MANIFEST);

    WorkflowRun run = testProject.scheduleBuild2(0).waitForStart();
    assertNotNull(run);
    jenkinsRule.assertBuildStatus(Result.SUCCESS, jenkinsRule.waitForCompletion(run));
    dumpLog(LOGGER, run);

    kubectlDelete(
        jenkinsRule.createLocalLauncher(),
        jenkinsRule.jenkins.getWorkspaceFor(testProject),
        TEST_DEPLOYMENT_MANIFEST,
        "deployment",
        "nginx-deployment",
        "test");
  }

  private static void kubectlDelete(
      Launcher launcher,
      FilePath workspace,
      String manifestPattern,
      String kind,
      String name,
      String namespace)
      throws Exception {
    Cluster cluster = client.getCluster(projectId, testLocation, clusterName);
    KubeConfig kubeConfig =
        KubeConfig.fromCluster(projectId, cluster, CredentialsUtil.getAccessToken(credentialsId));
    KubectlWrapper kubectl =
        new KubectlWrapper.Builder()
            .workspace(workspace)
            .launcher(launcher)
            .kubeConfig(kubeConfig)
            .namespace(namespace)
            .build();
    FilePath manifestFile = workspace.child(manifestPattern);
    kubectl.runKubectlCommand("delete", ImmutableList.<String>of(kind, name));
  }
}
