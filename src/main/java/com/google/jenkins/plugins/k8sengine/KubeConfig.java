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
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
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

  private List<Object> contexts;
  private List<Object> clusters;
  private List<Object> users;
  private String currentContext;

  private KubeConfig() {}

  /** @return This config's contexts. */
  public List<Object> getContexts() {
    return contexts;
  }

  private void setContexts(List<Object> contexts) {
    Objects.requireNonNull(contexts);
    this.contexts = contexts;
  }

  /** @return This config's clusters. */
  public List<Object> getClusters() {
    return clusters;
  }

  private void setClusters(List<Object> clusters) {
    Objects.requireNonNull(clusters);
    this.clusters = clusters;
  }

  /** @return This config's users. */
  public List<Object> getUsers() {
    return users;
  }

  private void setUsers(List<Object> users) {
    Objects.requireNonNull(users);
    this.users = users;
  }

  /** @return This config's current context. */
  public String getCurrentContext() {
    return currentContext;
  }

  private void setCurrentContext(String currentContext) {
    if (StringUtils.isEmpty(currentContext)) {
      throw new IllegalArgumentException("current context cannot be empty");
    }
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
            new MapBuilder()
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

    public Builder users(List<Object> users) {
      config.setUsers(users);
      return this;
    }

    public Builder contexts(List<Object> contexts) {
      config.setContexts(contexts);
      return this;
    }

    public Builder clusters(List<Object> clusters) {
      config.setClusters(clusters);
      return this;
    }

    public KubeConfig build() {
      if (StringUtils.isBlank(config.getCurrentContext())) {
        throw new IllegalArgumentException("current context cannot be empty");
      }
      return config;
    }
  }

  /**
   * Creates a {@link KubeConfig} from the specified {@link Cluster}.
   *
   * @param projectId The ID of the project the cluster resides in.
   * @param cluster The cluster data will be drawn from.
   * @param accessToken Access token for GKE API access.
   * @return A {@link KubeConfig} from the specified {@link Cluster}.
   */
  public static KubeConfig fromCluster(String projectId, Cluster cluster, String accessToken) {
    if (StringUtils.isBlank(projectId)) {
      throw new IllegalArgumentException("projectId cannot be empty");
    }
    Objects.requireNonNull(cluster);

    final String currentContext =
        contextString(projectId, cluster.getLocation(), cluster.getName());
    return new KubeConfig.Builder()
        .currentContext(currentContext)
        .contexts(Collections.singletonList(context(currentContext)))
        .users(Collections.singletonList(user(currentContext, cluster, accessToken)))
        .clusters(Collections.singletonList(cluster(currentContext, cluster)))
        .build();
  }

  static String contextString(String project, String location, String cluster) {
    if (StringUtils.isBlank(project)) {
      throw new IllegalArgumentException("project cannot be empty");
    }
    if (StringUtils.isBlank(location)) {
      throw new IllegalArgumentException("location cannot be empty");
    }
    if (StringUtils.isBlank(cluster)) {
      throw new IllegalArgumentException("cluster cannot be empty");
    }
    return String.format(KUBECONTEXT_FORMAT, project, location, cluster);
  }

  static String clusterServer(Cluster cluster) {
    Objects.requireNonNull(cluster);
    return String.format(KUBESERVER_FORMAT, cluster.getEndpoint());
  }

  private static Map<String, Object> context(String currentContext) {
    if (StringUtils.isBlank(currentContext)) {
      throw new IllegalArgumentException("currentContext cannot be empty");
    }
    return new MapBuilder()
        .put("name", currentContext)
        .put(
            "context",
            new MapBuilder()
                .put("cluster", currentContext)
                .put("user", currentContext)
                .put("namespace", "default")
                .build())
        .build();
  }

  private static Map<String, Object> user(
      String currentContext, Cluster cluster, String accessToken) {
    if (StringUtils.isEmpty(currentContext)) {
      throw new IllegalArgumentException("current context cannot be empty");
    }
    Objects.requireNonNull(cluster);
    if (StringUtils.isEmpty(accessToken)) {
      throw new IllegalArgumentException("accessToken cannot be empty");
    }

    return new MapBuilder()
        .put("name", currentContext)
        .put("user", new MapBuilder().put("token", accessToken).map)
        .build();
  }

  private static Map<String, Object> cluster(String currentContext, Cluster cluster) {
    if (StringUtils.isEmpty(currentContext)) {
      throw new IllegalArgumentException("current context cannot be empty");
    }
    Objects.requireNonNull(cluster);
    return new MapBuilder()
        .put("name", currentContext)
        .put(
            "cluster",
            new MapBuilder()
                .put("server", clusterServer(cluster))
                .put(
                    "certificate-authority-data", cluster.getMasterAuth().getClusterCaCertificate())
                .build())
        .build();
  }

  private static class MapBuilder<String, Object> {

    private Map<String, Object> map = new HashMap<>();

    MapBuilder<String, Object> put(String k, Object v) {
      map.put(k, v);
      return this;
    }

    Map<String, Object> build() {
      return Collections.unmodifiableMap(map);
    }
  }
}
