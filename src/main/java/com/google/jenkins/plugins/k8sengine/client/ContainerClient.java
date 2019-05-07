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

package com.google.jenkins.plugins.k8sengine.client;

import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Client for communicating with the Google GKE API.
 *
 * @see <a href="https://cloud.google.com/kubernetes-engine/docs/reference/rest/">Kubernetes
 *     Engine</a>
 */
public class ContainerClient {
  private static final String ZONE_WILDCARD = "-";
  private final Container container;

  /**
   * Constructs a new {@link ContainerClient} instance.
   *
   * @param container The {@link Container} instance this class will utilize for interacting with
   *     the GKE API.
   */
  public ContainerClient(Container container) {
    this.container = Preconditions.checkNotNull(container);
  }

  /**
   * Retrieves a {@link Cluster} from the container client.
   *
   * @param projectId The ID of the project the cluster resides in.
   * @param zone The location of the cluster.
   * @param cluster The name of the cluster.
   * @return The retrieved {@link Cluster}.
   * @throws IOException When an error occurred attempting to get the cluster.
   */
  public Cluster getCluster(String projectId, String zone, String cluster) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(zone));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(cluster));
    return container.projects().zones().clusters().get(projectId, zone, cluster).execute();
  }

  /**
   * Retrieves a list of {@link Cluster} objects from the container client.
   *
   * @param projectId The ID of the project the clusters resides.
   * @param zone The location of the clusters.
   * @return The retrieved list of {@link Cluster} objects.
   * @throws IOException When an error occurred attempting to get the cluster.
   */
  public List<Cluster> listClusters(String projectId, String zone) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    List<Cluster> clusters =
        container
            .projects()
            .zones()
            .clusters()
            .list(projectId, toFilter(zone))
            .execute()
            .getClusters();
    if (clusters == null) {
      return ImmutableList.of();
    }
    clusters.sort(Comparator.comparing(Cluster::getName));
    return ImmutableList.copyOf(clusters);
  }

  private static String toFilter(String zone) {
    if (Strings.isNullOrEmpty(zone)) {
      return ZONE_WILDCARD;
    }
    return zone;
  }

  public static String toNameAndZone(Cluster cluster) {
    return toNameAndZone(cluster.getName(), cluster.getZone());
  }

  public static String toNameAndZone(String name, String zone) {
    return String.format("%s (%s)", name, zone);
  }

  @VisibleForTesting
  public static Cluster fromNameAndZone(String nameAndZone) {
    String[] values = valuesFromNameAndZone(nameAndZone);
    return new Cluster().setName(values[0]).setZone(values[1]);
  }

  public static String[] valuesFromNameAndZone(String nameAndZone) {
    String[] clusters = nameAndZone.split(" [(]");
    clusters[1] = clusters[1].substring(0, clusters[1].length() - 1);
    return clusters;
  }
}
