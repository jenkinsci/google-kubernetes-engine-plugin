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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;

import com.google.api.services.container.Container;
import com.google.api.services.container.Container.Projects.Zones.Clusters;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/** Tests {@link ContainerClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ContainerClientTest {
  private static final String TEST_PROJECT_ID = "test-project-id";
  private static final String OTHER_PROJECT_ID = "other-project-id";
  private static final String ERROR_PROJECT_ID = "error-project-id";
  private static final String EMPTY_PROJECT_ID = "empty-project-id";
  private static final String TEST_ZONE = "us-west1-a";
  private static final String OTHER_ZONE = "us-west2-b";
  private static final String TEST_CLUSTER = "testCluster";
  private static final String OTHER_CLUSTER = "otherCluster";

  private static ContainerClient containerClient;
  private static Cluster testCluster;

  @BeforeClass
  public static void init() throws Exception {
    Container container = Mockito.mock(Container.class);
    Container.Projects projects = Mockito.mock(Container.Projects.class);
    Container.Projects.Zones zones = Mockito.mock(Container.Projects.Zones.class);
    Clusters clusters = Mockito.mock(Container.Projects.Zones.Clusters.class);
    Mockito.when(container.projects()).thenReturn(projects);
    Mockito.when(projects.zones()).thenReturn(zones);
    Mockito.when(zones.clusters()).thenReturn(clusters);
    testCluster = new Cluster().setName(TEST_CLUSTER).setZone(TEST_ZONE);
    Cluster otherCluster = new Cluster().setName(OTHER_CLUSTER).setZone(TEST_ZONE);

    Mockito.when(clusters.get(anyString(), anyString(), anyString()))
        .thenAnswer(
            mockClustersGetAnswer(
                new ImmutableMap.Builder<String, List<Cluster>>()
                    .put(TEST_PROJECT_ID, ImmutableList.of(testCluster))
                    .build()));

    Mockito.when(clusters.list(anyString(), anyString()))
        .thenAnswer(
            mockClustersListAnswer(
                new ImmutableMap.Builder<Pair<String, String>, List<Cluster>>()
                    .put(
                        ImmutablePair.of(TEST_PROJECT_ID, TEST_ZONE), ImmutableList.of(testCluster))
                    .put(
                        ImmutablePair.of(OTHER_PROJECT_ID, TEST_ZONE),
                        ImmutableList.of(otherCluster))
                    .put(ImmutablePair.of(TEST_PROJECT_ID, OTHER_ZONE), ImmutableList.of())
                    .build()));
    containerClient = new ContainerClient(container);
  }

  @Test
  public void testGetClusterReturnsProperlyWhenClusterExists() throws IOException {
    Cluster response = containerClient.getCluster(TEST_PROJECT_ID, TEST_ZONE, TEST_CLUSTER);
    assertNotNull(response);
    assertEquals(testCluster, response);
  }

  @Test(expected = IOException.class)
  public void testGetClusterThrowsErrorWhenClusterDoesntExists() throws IOException {
    containerClient.getCluster(TEST_PROJECT_ID, OTHER_ZONE, OTHER_CLUSTER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListClustersErrorWithNullProjectId() throws IOException {
    containerClient.listClusters(null, TEST_ZONE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListClustersErrorWithNullZone() throws IOException {
    containerClient.listClusters(TEST_PROJECT_ID, null);
  }

  @Test
  public void testListClustersWithValidInputsWhenClustersExist() throws IOException {
    List<Cluster> expected = initExpectedClusterList(ImmutableList.of(TEST_CLUSTER));
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersWithDifferentProjectClusters() throws IOException {
    List<Cluster> expected = initExpectedClusterList(ImmutableList.of(OTHER_CLUSTER));
    List<Cluster> response = containerClient.listClusters(OTHER_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersWithValidInputsWhenClustersIsNull() throws IOException {
    List<Cluster> expected = ImmutableList.of();
    List<Cluster> response = containerClient.listClusters(EMPTY_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersEmptyWithValidProjectWithNoClusters() throws IOException {
    List<Cluster> expected = ImmutableList.of();
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, OTHER_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test(expected = IOException.class)
  public void testListClustersThrowsErrorWithInvalidProject() throws IOException {
    containerClient.listClusters(ERROR_PROJECT_ID, TEST_ZONE);
  }

  private static List<Cluster> initExpectedClusterList(List<String> expectedClusterNames) {
    List<Cluster> clusters = new ArrayList<>();
    expectedClusterNames.forEach(e -> clusters.add(new Cluster().setName(e).setZone(TEST_ZONE)));
    return clusters;
  }

  private static Answer<Container.Projects.Zones.Clusters.Get> mockClustersGetAnswer(
      ImmutableMap<String, List<Cluster>> projectToClusters) {
    return invocation -> {
      Object[] args = invocation.getArguments();
      String projectId = (String) args[0];
      String zone = (String) args[1];
      String clusterName = (String) args[2];
      if (!projectToClusters.containsKey(projectId)) {
        throw new IOException(
            String.format(
                "Failed to find cluster, projectId: %s, zone: %s, cluster: %s",
                projectId, zone, clusterName));
      }

      Optional<Cluster> cluster =
          projectToClusters
              .get(projectId)
              .stream()
              .filter(c -> c.getName().equals(clusterName) && c.getZone().equals(zone))
              .findFirst();
      if (!cluster.isPresent()) {
        throw new IOException(
            String.format(
                "Failed to find cluster, projectId: %s, zone: %s, cluster: %s",
                projectId, zone, clusterName));
      }

      Container.Projects.Zones.Clusters.Get getCall =
          Mockito.mock(Container.Projects.Zones.Clusters.Get.class);
      Mockito.when(getCall.execute()).thenReturn(cluster.get());
      return getCall;
    };
  }

  private static Answer<Container.Projects.Zones.Clusters.List> mockClustersListAnswer(
      ImmutableMap<Pair<String, String>, List<Cluster>> projectToClusters) {
    return invocation -> {
      Object[] args = invocation.getArguments();
      String projectId = (String) args[0];
      String zone = (String) args[1];
      Pair<String, String> parent = ImmutablePair.of(projectId, zone);

      List<Cluster> clusters = null;
      if (projectToClusters.containsKey(parent)) {
        clusters = new ArrayList<>(projectToClusters.get(parent));
      } else if (ERROR_PROJECT_ID.equals(projectId)) {
        throw new IOException(
            String.format(
                "Failed to get clusters for projectId: %s and zone: %s", projectId, zone));
      }

      Container.Projects.Zones.Clusters.List listCall =
          Mockito.mock(Container.Projects.Zones.Clusters.List.class);
      ListClustersResponse response = new ListClustersResponse().setClusters(clusters);
      Mockito.when(listCall.execute()).thenReturn(response);
      return listCall;
    };
  }
}
