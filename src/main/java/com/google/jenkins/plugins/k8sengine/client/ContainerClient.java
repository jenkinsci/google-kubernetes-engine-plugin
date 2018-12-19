/*
 * Copyright 2019 Google Inc.
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
import com.google.api.services.container.model.CreateClusterRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;

/**
 * Client for communicating with the Google GKE API.
 *
 * @see <a href="https://cloud.google.com/kubernetes-engine/docs/reference/rest/">Kubernetes
 *     Engine</a>
 */
public class ContainerClient {
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
   * @param projectId The ID of the project the clusters reside in.
   * @param zone The location of the clusters.
   * @param cluster The name of the cluster b
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
   * Creates a {@link Cluster}.
   *
   * @param projectId The ID of the project the cluster will be created in.
   * @param zone The location of of the cluster.
   * @param clusterName The name of the cluster.
   * @param nodeCount The initial node count of the cluster.
   * @return The created {@link Cluster}.
   * @throws IOException When an error occurred during cluster creation.
   */
  @VisibleForTesting
  public Cluster createCluster(String projectId, String zone, String clusterName, int nodeCount)
      throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(zone));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(clusterName));

    CreateClusterRequest request = new CreateClusterRequest();
    request.setParent(formatParentLocation(projectId, zone));
    request.setProjectId(projectId);
    request.setZone(zone);
    Cluster cluster = new Cluster();
    cluster.setName(clusterName);
    cluster.setInitialNodeCount(nodeCount);
    request.setCluster(cluster);
    container.projects().zones().clusters().create(projectId, zone, request).execute();
    return request.getCluster();
  }

  /**
   * Deletes a {@link Cluster}.
   *
   * @param projectId The ID of the project the cluster resides in.
   * @param zone The location of the cluster.
   * @param cluster The name of the cluster to be deleted.
   * @throws IOException When an error occurred during cluster deletion.
   */
  @VisibleForTesting
  public void deleteCluster(String projectId, String zone, String cluster) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(zone));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(cluster));
    container.projects().zones().clusters().delete(projectId, zone, cluster).execute();
  }

  private static String formatParentLocation(String projectId, String zone) {
    return String.format("projects/%s/locations/%s", projectId, zone);
  }
}
