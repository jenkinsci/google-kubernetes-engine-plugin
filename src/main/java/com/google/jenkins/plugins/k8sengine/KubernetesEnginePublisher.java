/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2Credentials;
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
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/** Provides a build step for publishing build artifacts to a Kubernetes cluster running on GKE. */
public class KubernetesEnginePublisher extends Notifier implements SimpleBuildStep, Serializable {
  private static final Logger LOGGER = Logger.getLogger(KubernetesEnginePublisher.class.getName());

  private String projectId;
  private String cluster;
  private String namespace;
  private String manifestPattern;
  private boolean verifyDeployments;
  private boolean verifyServices;

  @DataBoundSetter
  public void setProjectId(String projectId) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    this.projectId = projectId;
  }

  public String getProjectId() {
    return this.projectId;
  }

  @DataBoundSetter
  public void setCluster(String cluster) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(cluster));
    this.cluster = cluster;
  }

  public String getCluster() {
    return this.cluster;
  }

  @DataBoundSetter
  public void setNamespace(String namespace) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(namespace));
    this.namespace = namespace;
  }

  public String getNamespace() {
    return this.namespace;
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

  @Override
  public void perform(
      @Nonnull Run<?, ?> run,
      @Nonnull FilePath workspace,
      @Nonnull Launcher launcher,
      @Nonnull TaskListener listener) /*throws InterruptedException, IOException */ {
    // TODO finish me
    LOGGER.log(
        Level.INFO,
        String.format(
            "GKE Deploy, projectId: %s cluster: %s namespace: %s", projectId, cluster, namespace));
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

  @Symbol("kubernetesEngineDeploy")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    @Nonnull
    @Override
    public String getDisplayName() {
      return Messages.KubernetesEnginePublisher_DisplayName();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    public FormValidation doCheckCluster(@QueryParameter String value) {
      if (Strings.isNullOrEmpty(value)) {
        return FormValidation.error(Messages.KubernetesEnginePublisher_ClusterRequired());
      }

      // TODO check to ensure the cluster exists within GKE cluster
      return FormValidation.ok();
    }

    public FormValidation doCheckNamespace(@QueryParameter String value) {
      if (Strings.isNullOrEmpty(value)) {
        return FormValidation.error(Messages.KubernetesEnginePublisher_NamespaceRequired());
      }

      // TODO check to ensure the namespace exists within the GKE cluster.
      return FormValidation.ok();
    }

    public FormValidation doCheckManifestPattern(@QueryParameter String value) {
      if (Strings.isNullOrEmpty(value)) {
        return FormValidation.error(Messages.KubernetesEnginePublisher_ManifestRequired());
      }

      return FormValidation.ok();
    }

    public FormValidation doCheckProjectId(@QueryParameter String value) {
      if (Strings.isNullOrEmpty(value)) {
        return FormValidation.error(Messages.KubernetesEnginePublisher_ProjectIDRequired());
      }
      return FormValidation.ok();
    }

    public ListBoxModel doFillCredentialsIdItems(
        @AncestorInPath Jenkins context, @QueryParameter String value) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new StandardListBoxModel();
      }

      return new StandardListBoxModel()
          .withEmptySelection()
          .withMatching(
              CredentialsMatchers.instanceOf(GoogleOAuth2Credentials.class),
              CredentialsProvider.lookupCredentials(
                  StandardCredentials.class, context, ACL.SYSTEM));
    }

    public FormValidation doCheckCredentialsId(
        @AncestorInPath Jenkins context,
        @QueryParameter("projectId") String projectId,
        @QueryParameter String value) {
      if (value.isEmpty()) {
        return FormValidation.error(Messages.KubernetesEnginePublisher_NoCredential());
      }

      if (projectId.isEmpty()) {
        return FormValidation.error(
            Messages.KubernetesEnginePublisher_CredentialProjectIDRequired());
      }

      // TODO Fill in with a validation using the GKE client.

      return FormValidation.ok();
    }
  }
}
