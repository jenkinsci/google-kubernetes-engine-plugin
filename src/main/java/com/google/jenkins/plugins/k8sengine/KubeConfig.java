/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.jenkins.plugins.k8sengine;

import com.google.api.services.container.model.Cluster;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Writer;
import org.yaml.snakeyaml.Yaml;

/**
 * Encapsulates KubeConfig data, its construction from Cluster data, and its output to Yaml.
 * NOTE(craigatgoogle): This is a temporary stop-gap measure which will be replaced once the GKE API
 * supports a server-side method for this functionality: b/120097899.
 */
public class KubeConfig {
  private static final String KUBECONTEXT_FORMAT = "gke_%s_%s_%s";
  private static final String KUBESERVER_FORMAT = "https://%s";
  private static final String API_VERSION = "v1";
  private static final String CONFIG_KIND = "Config";

  private ImmutableList<Object> contexts;
  private ImmutableList<Object> clusters;
  private ImmutableList<Object> users;
  private String currentContext;

  private KubeConfig() {}

  /** @return This config's contexts. */
  public ImmutableList<Object> getContexts() {
    return contexts;
  }

  private void setContexts(ImmutableList<Object> contexts) {
    this.contexts = Preconditions.checkNotNull(contexts);
  }

  /** @return This config's clusters. */
  public ImmutableList<Object> getClusters() {
    return clusters;
  }

  private void setClusters(ImmutableList<Object> clusters) {
    this.clusters = Preconditions.checkNotNull(clusters);
  }

  /** @return This config's users. */
  public ImmutableList<Object> getUsers() {
    return users;
  }

  private void setUsers(ImmutableList<Object> users) {
    this.users = Preconditions.checkNotNull(users);
  }

  /** @return This config's current context. */
  public String getCurrentContext() {
    return currentContext;
  }

  private void setCurrentContext(String currentContext) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(currentContext));
    this.currentContext = currentContext;
  }

  /**
   * Write a Yaml dump of this {@link KubeConfig}'s data to the specified {@link Writer}.
   * NOTE(craigatgoogle): The logic here is taken directly from the `gcloud containers clutsers
   * get-credentials` command's implementation: <a
   * href="https://github.com/google-cloud-sdk/google-cloud-sdk/blob/2cc7b066a0621fe5d78ac9e69157de56a142e126/lib/surface/container/clusters/get_credentials.py#L87">link</a>.
   *
   * @return A string containing a Yaml dump of this {@link KubeConfig}.
   * @throws IOException If an error was encountered while exporting to Yaml.
   */
  public String toYaml() throws IOException {
    return new Yaml()
        .dumpAsMap(
            new ImmutableMap.Builder<String, Object>()
                .put("apiVersion", API_VERSION)
                .put("kind", CONFIG_KIND)
                .put("current-context", getCurrentContext())
                .put("clusters", getClusters())
                .put("contexts", getContexts())
                .put("users", getUsers())
                .build());
  }

  /** Builder for {@link KubeConfig}. */
  public static class Builder {
    private KubeConfig config;

    public Builder() {
      config = new KubeConfig();
    }

    public Builder currentContext(String currentContext) {
      config.setCurrentContext(currentContext);
      return this;
    }

    public Builder users(ImmutableList<Object> users) {
      config.setUsers(users);
      return this;
    }

    public Builder contexts(ImmutableList<Object> contexts) {
      config.setContexts(contexts);
      return this;
    }

    public Builder clusters(ImmutableList<Object> clusters) {
      config.setClusters(clusters);
      return this;
    }

    public KubeConfig build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getCurrentContext()));
      return config;
    }
  }

  /**
   * Creates a {@link KubeConfig} from the specified {@link Cluster}.
   *
   * @param projectId The ID of the project the cluster resides in.
   * @param cluster The cluster data will be drawn from.
   * @return A {@link KubeConfig} from the specified {@link Cluster}.
   */
  public static KubeConfig fromCluster(String projectId, Cluster cluster) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    Preconditions.checkNotNull(cluster);

    final String currentContext = contextString(projectId, cluster.getZone(), cluster.getName());
    return new KubeConfig.Builder()
        .currentContext(currentContext)
        .contexts(ImmutableList.<Object>of(context(currentContext)))
        .users(ImmutableList.<Object>of(user(currentContext, cluster)))
        .clusters(ImmutableList.<Object>of(cluster(currentContext, cluster)))
        .build();
  }

  @VisibleForTesting
  static String contextString(String project, String zone, String cluster) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(project));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(zone));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(cluster));
    return String.format(KUBECONTEXT_FORMAT, project, zone, cluster);
  }

  @VisibleForTesting
  static String clusterServer(Cluster cluster) {
    Preconditions.checkNotNull(cluster);
    return String.format(KUBESERVER_FORMAT, cluster.getEndpoint());
  }

  private static ImmutableMap<String, Object> context(String currentContext) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(currentContext));
    return new ImmutableMap.Builder<String, Object>()
        .put("name", currentContext)
        .put(
            "context",
            new ImmutableMap.Builder<String, Object>()
                .put("cluster", currentContext)
                .put("user", currentContext)
                .build())
        .build();
  }

  private static ImmutableMap<String, Object> user(String currentContext, Cluster cluster) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(currentContext));
    Preconditions.checkNotNull(cluster);
    return new ImmutableMap.Builder<String, Object>()
        .put("name", currentContext)
        .put(
            "user",
            new ImmutableMap.Builder<String, Object>()
                .put("client-certificate-data", cluster.getMasterAuth().getClientCertificate())
                .put("client-key-data", cluster.getMasterAuth().getClientKey())
                .build())
        .build();
  }

  private static ImmutableMap<String, Object> cluster(String currentContext, Cluster cluster) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(currentContext));
    Preconditions.checkNotNull(cluster);
    return new ImmutableMap.Builder<String, Object>()
        .put("name", currentContext)
        .put(
            "cluster",
            new ImmutableMap.Builder<String, Object>()
                .put("server", clusterServer(cluster))
                .put(
                    "certificate-authority-data", cluster.getMasterAuth().getClusterCaCertificate())
                .build())
        .build();
  }
}
