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
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.container.model.Cluster;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2Credentials;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.CloudResourceManagerClient;
import com.google.jenkins.plugins.k8sengine.client.ContainerClient;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/** Provides a build step for publishing build artifacts to a Kubernetes cluster running on GKE. */
public class KubernetesEngineBuilder extends Builder implements SimpleBuildStep, Serializable {
  public static final long serialVersionUID = 333L;
  private static final Logger LOGGER = Logger.getLogger(KubernetesEngineBuilder.class.getName());
  static final String EMPTY_NAME = "- none -";
  static final String EMPTY_VALUE = "";
  static final int DEFAULT_VERIFY_TIMEOUT_MINUTES = 5;
  static final String METRICS_LABEL_KEY = "app.kubernetes.io/managed-by";
  static final String METRICS_LABEL_VALUE = "graphite-jenkins-gke";
  static final ImmutableSet<String> METRICS_TARGET_TYPES =
      ImmutableSet.<String>of("Deployment", "Service", "ReplicaSet");

  private String credentialsId;
  private String projectId;
  private String zone;
  private String clusterName;
  private String namespace;
  private String manifestPattern;
  private boolean verifyDeployments;
  private int verifyTimeoutInMinutes = DEFAULT_VERIFY_TIMEOUT_MINUTES;
  private boolean verifyServices;
  private boolean isTestCleanup;
  private LinkedList<KubeConfigAfterBuildStep> afterBuildStepStack;

  /** Constructs a new {@link KubernetesEngineBuilder}. */
  @DataBoundConstructor
  public KubernetesEngineBuilder() {}

  public String getCredentialsId() {
    return this.credentialsId;
  }

  @DataBoundSetter
  public void setCredentialsId(String credentialsId) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(credentialsId));
    this.credentialsId = credentialsId;
  }

  public String getProjectId() {
    return this.projectId;
  }

  @DataBoundSetter
  public void setProjectId(String projectId) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    this.projectId = projectId;
  }

  public String getZone() {
    return this.zone;
  }

  @DataBoundSetter
  public void setZone(String zone) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(zone));
    this.zone = zone;
  }

  public String getClusterName() {
    return this.clusterName;
  }

  @DataBoundSetter
  public void setClusterName(String clusterName) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(clusterName));
    this.clusterName = clusterName;
  }

  public String getCluster() {
    return ClusterUtil.toNameAndZone(this.clusterName, this.zone);
  }

  @DataBoundSetter
  public void setCluster(String cluster) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(cluster));
    String[] values = ClusterUtil.valuesFromNameAndZone(cluster);
    setClusterName(values[0]);
    setZone(values[1]);
  }

  public String getNamespace() {
    return this.namespace;
  }

  @DataBoundSetter
  public void setNamespace(String namespace) {
    this.namespace = namespace == null ? "" : namespace;
  }

  public String getManifestPattern() {
    return this.manifestPattern;
  }

  @DataBoundSetter
  public void setManifestPattern(String manifestPattern) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(manifestPattern));
    this.manifestPattern = manifestPattern;
  }

  @DataBoundSetter
  public void setVerifyDeployments(boolean verifyDeployments) {
    this.verifyDeployments = verifyDeployments;
  }

  public boolean isVerifyDeployments() {
    return this.verifyDeployments;
  }

  @DataBoundSetter
  public void setVerifyServices(boolean verifyServices) {
    this.verifyServices = verifyServices;
  }

  public boolean isVerifyServices() {
    return this.verifyServices;
  }

  public int getVerifyTimeoutInMinutes() {
    return verifyTimeoutInMinutes;
  }

  @DataBoundSetter
  public void setVerifyTimeoutInMinutes(int verifyTimeoutInMinutes) {
    this.verifyTimeoutInMinutes = verifyTimeoutInMinutes;
  }

  @VisibleForTesting
  void pushAfterBuildStep(KubeConfigAfterBuildStep afterBuildStep) {
    if (afterBuildStepStack == null) {
      afterBuildStepStack = new LinkedList<>();
    }

    afterBuildStepStack.push(afterBuildStep);
  }

  /** {@inheritDoc} */
  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath workspace,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener listener)
      throws InterruptedException, IOException {
    LOGGER.log(
        Level.INFO,
        String.format(
            "GKE Deploying, projectId: %s cluster: %s zone: %s", projectId, clusterName, zone));
    ContainerClient client = getContainerClient(credentialsId);
    Cluster cluster = client.getCluster(projectId, zone, clusterName);

    // generate a kubeconfig for the cluster
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
    addMetricsLabel(manifestFile);
    kubectl.runKubectlCommand("apply", ImmutableList.of("-f", manifestFile.getRemote()));
    try {
      if (verifyDeployments && !verify(kubectl, manifestPattern, workspace, listener.getLogger())) {
        throw new AbortException(Messages.KubernetesEngineBuilder_KubernetesObjectsNotVerified());
      }
    } finally {
      // run the after build step if it exists
      // NOTE(craigatgoogle): Due to the reflective way this class is created, initializers aren't
      // run, so we still have to check for null.
      if (afterBuildStepStack != null) {
        while (!afterBuildStepStack.isEmpty()) {
          afterBuildStepStack.pop().perform(kubeConfig, run, workspace, launcher, listener);
        }
      }
    }
  }

  /**
   * Adds a Kubernetes user label unique to this Jenkins plugin to the specified manifest,
   * (in-place) in order to enable Jenkins GKE/GCE non-identifying usage metrics. Behavior with
   * malformed manifests is undefined.
   *
   * @param manifestFile The manifest file to be modified.
   * @throws IOException If an error occurred while reading/writing the manifest file.
   * @throws InterruptedException If an error occurred while parsing/dumping YAML.
   */
  @VisibleForTesting
  static void addMetricsLabel(FilePath manifestFile) throws InterruptedException, IOException {
    Manifests manifests = Manifests.fromFile(manifestFile);
    for (Manifests.ManifestObject manifest :
        manifests.getObjectManifestsOfKinds(METRICS_TARGET_TYPES)) {
      manifest.addLabel(METRICS_LABEL_KEY, METRICS_LABEL_VALUE);
    }

    manifests.write();
  }

  /**
   * Verify the application of the supplied {@link Manifests.ManifestObject}'s to the Kubernetes
   * cluster.
   *
   * @param kubectl The {@link KubectlWrapper} for running the queries on the Kubernetes cluster.
   * @param manifestPattern The manifest pattern for the list of manifest files to use.
   * @param workspace The {@link FilePath} to the build workspace directory.
   * @param consoleLogger The {@link PrintStream} for Jenkins console output.
   * @return If the verification succeeded.
   * @throws InterruptedException If an error occurred during verification.
   * @throws IOException If an error occurred during verification.
   */
  private boolean verify(
      KubectlWrapper kubectl, String manifestPattern, FilePath workspace, PrintStream consoleLogger)
      throws InterruptedException, IOException {
    LOGGER.log(
        Level.INFO,
        String.format(
            "GKE verifying deployment to, projectId: %s cluster: %s zone: %s manifests: %s",
            projectId, clusterName, zone, workspace.child(manifestPattern)));

    consoleLogger.println(
        String.format("Verifying manifests: %s", workspace.child(manifestPattern)));

    Manifests manifests = Manifests.fromFile(workspace.child(manifestPattern));

    // Filter by the kinds of manifests being verified.
    List<Manifests.ManifestObject> manifestObjects =
        manifests.getObjectManifestsOfKinds(ImmutableSet.of(KubernetesVerifiers.DEPLOYMENT_KIND));

    consoleLogger.println(
        Messages.KubernetesEngineBuilder_VerifyingNObjects(manifestObjects.size()));

    return VerificationTask.verifyObjects(
        kubectl, manifestObjects, consoleLogger, verifyTimeoutInMinutes);
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Symbol("kubernetesEngineDeploy")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    private ClientFactory clientFactory;

    @Nonnull
    @Override
    public String getDisplayName() {
      return Messages.KubernetesEngineBuilder_DisplayName();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @VisibleForTesting
    ClientFactory getClientFactory(Jenkins context, String credentialsId) throws AbortException {
      if (this.clientFactory == null
          || !this.clientFactory.getCredentialsId().equals(credentialsId)) {
        this.clientFactory = new ClientFactory(context, credentialsId);
      }
      return this.clientFactory;
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new StandardListBoxModel();
      }

      return new StandardListBoxModel()
          .includeEmptyValue()
          .includeMatchingAs(
              ACL.SYSTEM,
              context,
              StandardCredentials.class,
              Collections.<DomainRequirement>emptyList(),
              CredentialsMatchers.instanceOf(GoogleOAuth2Credentials.class));
    }

    public FormValidation doCheckCredentialsId(
        @AncestorInPath Jenkins context,
        @QueryParameter("credentialsId") final String credentialsId) {
      if (credentialsId.isEmpty()) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_NoCredential());
      }

      try {
        CredentialsUtil.getAccessToken(context, credentialsId);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, Messages.KubernetesEngineBuilder_CredentialAuthFailed(), ex);
        return FormValidation.error(Messages.KubernetesEngineBuilder_CredentialAuthFailed());
      }

      return FormValidation.ok();
    }

    public ListBoxModel doFillProjectIdItems(
        @AncestorInPath Jenkins context,
        @QueryParameter("projectId") final String projectId,
        @QueryParameter("credentialsId") final String credentialsId) {
      ListBoxModel items = new ListBoxModel();
      items.add(EMPTY_NAME, EMPTY_VALUE);
      if (Strings.isNullOrEmpty(credentialsId)) {
        return items;
      }

      ClientFactory clientFactory;
      try {
        clientFactory = this.getClientFactory(context, credentialsId);
      } catch (AbortException | RuntimeException ex) {
        LOGGER.log(Level.SEVERE, Messages.KubernetesEngineBuilder_CredentialAuthFailed(), ex);
        items.clear();
        items.add(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), EMPTY_VALUE);
        return items;
      }

      String defaultProjectId = clientFactory.getDefaultProjectId();
      try {
        CloudResourceManagerClient client = clientFactory.cloudResourceManagerClient();
        List<Project> projects = client.getAccountProjects();

        if (projects.isEmpty()) {
          return items;
        }

        projects.stream()
            .filter(p -> !p.getProjectId().equals(defaultProjectId))
            .forEach(p -> items.add(p.getProjectId()));

        if (Strings.isNullOrEmpty(defaultProjectId)) {
          selectOption(items, projectId);
          return items;
        }

        if (projects.size() == items.size() && Strings.isNullOrEmpty(projectId)) {
          items.add(new Option(defaultProjectId, defaultProjectId, true));
        } else {
          // Add defaultProjectId anyway, but select the appropriate projectID based on
          // the previously entered projectID
          items.add(defaultProjectId);
          selectOption(items, projectId);
        }
        return items;
      } catch (IOException ioe) {
        LOGGER.log(Level.SEVERE, Messages.KubernetesEngineBuilder_ProjectIDFillError(), ioe);
        items.clear();
        items.add(Messages.KubernetesEngineBuilder_ProjectIDFillError(), EMPTY_VALUE);
        return items;
      }
    }

    public FormValidation doCheckProjectId(
        @AncestorInPath Jenkins context,
        @QueryParameter("projectId") final String projectId,
        @QueryParameter("credentialsId") final String credentialsId) {
      if (Strings.isNullOrEmpty(credentialsId) && Strings.isNullOrEmpty(projectId)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ProjectIDRequired());
      } else if (Strings.isNullOrEmpty(credentialsId)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ProjectCredentialIDRequired());
      }

      ClientFactory clientFactory;
      try {
        clientFactory = getClientFactory(context, credentialsId);
      } catch (AbortException | RuntimeException e) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_CredentialAuthFailed());
      }

      try {
        CloudResourceManagerClient client = clientFactory.cloudResourceManagerClient();
        List<Project> projects = client.getAccountProjects();
        if (Strings.isNullOrEmpty(projectId)) {
          return FormValidation.error(Messages.KubernetesEngineBuilder_ProjectIDRequired());
        }

        Optional<Project> matchingProject =
            projects.stream().filter(p -> projectId.equals(p.getProjectId())).findFirst();
        if (!matchingProject.isPresent()) {
          return FormValidation.error(
              Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential());
        }
      } catch (IOException ioe) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ProjectIDVerificationError());
      }

      return FormValidation.ok();
    }

    public ListBoxModel doFillClusterItems(
        @AncestorInPath Jenkins context,
        @QueryParameter("cluster") final String cluster,
        @QueryParameter("credentialsId") final String credentialsId,
        @QueryParameter("projectId") final String projectId) {
      ListBoxModel items = new ListBoxModel();
      items.add(EMPTY_NAME, EMPTY_VALUE);
      if (Strings.isNullOrEmpty(credentialsId) || Strings.isNullOrEmpty(projectId)) {
        return items;
      }

      ClientFactory clientFactory;
      try {
        clientFactory = getClientFactory(context, credentialsId);
      } catch (AbortException | RuntimeException ex) {
        LOGGER.log(Level.SEVERE, Messages.KubernetesEngineBuilder_CredentialAuthFailed(), ex);
        items.clear();
        items.add(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), EMPTY_VALUE);
        return items;
      }

      try {
        ContainerClient client = clientFactory.containerClient();
        List<Cluster> clusters = client.listAllClusters(projectId);

        if (clusters.isEmpty()) {
          return items;
        }

        clusters.forEach(c -> items.add(ClusterUtil.toNameAndZone(c)));
        selectOption(items, cluster);
        return items;
      } catch (IOException ioe) {
        LOGGER.log(Level.SEVERE, Messages.KubernetesEngineBuilder_ClusterFillError(), ioe);
        items.clear();
        items.add(Messages.KubernetesEngineBuilder_ClusterFillError(), EMPTY_VALUE);
        return items;
      }
    }

    public FormValidation doCheckCluster(
        @AncestorInPath Jenkins context,
        @QueryParameter("cluster") final String cluster,
        @QueryParameter("credentialsId") final String credentialsId,
        @QueryParameter("projectId") final String projectId) {
      if (Strings.isNullOrEmpty(credentialsId) && Strings.isNullOrEmpty(cluster)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterRequired());
      } else if (Strings.isNullOrEmpty(credentialsId)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterCredentialIDRequired());
      }

      ClientFactory clientFactory;
      try {
        clientFactory = getClientFactory(context, credentialsId);
      } catch (AbortException | RuntimeException ex) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_CredentialAuthFailed());
      }

      if (Strings.isNullOrEmpty(projectId) && Strings.isNullOrEmpty(cluster)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterRequired());
      } else if (Strings.isNullOrEmpty(projectId)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterProjectIDRequired());
      }

      try {
        ContainerClient client = clientFactory.containerClient();
        List<Cluster> clusters = client.listAllClusters(projectId);
        if (Strings.isNullOrEmpty(cluster)) {
          return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterRequired());
        } else if (clusters.size() == 0) {
          return FormValidation.error(Messages.KubernetesEngineBuilder_NoClusterInProjectZone());
        }
        Optional<Cluster> clusterOption =
            clusters.stream().filter(c -> cluster.equals(ClusterUtil.toNameAndZone(c))).findFirst();
        if (!clusterOption.isPresent()) {
          return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterNotInProjectZone());
        }
      } catch (IOException ioe) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ClusterVerificationError());
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckNamespace(@QueryParameter("namespace") final String namespace) {
      /* Regex from
       * https://github.com/kubernetes/apimachinery/blob/7d08eb7a76fdbc79f7bc1b5fb061ae44f3324bfa/pkg/util/validation/validation.go#L110
       */
      if (!Strings.isNullOrEmpty(namespace)
          && !namespace.matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?")) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_NamespaceInvalid());
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckManifestPattern(
        @QueryParameter("manifestPattern") final String manifestPattern) {
      if (Strings.isNullOrEmpty(manifestPattern)) {
        return FormValidation.error(Messages.KubernetesEngineBuilder_ManifestRequired());
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckVerifyTimeoutInMinutes(
        @QueryParameter("verifyTimeoutInMinutes") final String verifyTimeoutInMinutes) {
      if (Strings.isNullOrEmpty(verifyTimeoutInMinutes)) {
        return FormValidation.error(
            Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesRequired());
      }

      if (!verifyTimeoutInMinutes.matches("([1-9]\\d*)")) {
        return FormValidation.error(
            Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesFormatError());
      }

      return FormValidation.ok();
    }
  }

  private static void selectOption(ListBoxModel listBoxModel, String optionValue) {
    Optional<Option> item;
    if (!Strings.isNullOrEmpty(optionValue)) {
      item = listBoxModel.stream().filter(option -> optionValue.equals(option.value)).findFirst();
      if (item.isPresent()) {
        item.get().selected = true;
        return;
      }
    }
    item = listBoxModel.stream().filter(option -> !Strings.isNullOrEmpty(option.value)).findFirst();
    item.ifPresent(i -> i.selected = true);
  }

  private static ContainerClient getContainerClient(String credentialsId) throws AbortException {
    return new ClientFactory(
            Jenkins.get(),
            ImmutableList.<DomainRequirement>of(),
            credentialsId,
            Optional.<HttpTransport>empty())
        .containerClient();
  }

  @FunctionalInterface
  interface KubeConfigAfterBuildStep extends Serializable {
    public void perform(
        KubeConfig kubeConfig,
        Run<?, ?> run,
        FilePath workspace,
        Launcher launcher,
        TaskListener listener)
        throws AbortException, InterruptedException, IOException;
  }
}
