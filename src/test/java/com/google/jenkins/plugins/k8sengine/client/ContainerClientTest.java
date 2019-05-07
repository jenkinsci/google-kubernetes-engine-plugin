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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;

import com.google.api.services.container.Container;
import com.google.api.services.container.Container.Projects.Zones.Clusters;
import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.ListClustersResponse;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests {@link ContainerClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ContainerClientTest {
  private static final String TEST_PROJECT_ID = "test-project-id";
  private static final String TEST_ZONE = "us-west1-a";
  private static final String TEST_CLUSTER = "testCluster";
  private static final String OTHER_CLUSTER = "otherCluster";

  @Test(expected = IllegalArgumentException.class)
  public void testGetClustersErrorWithNullProjectId() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, null);
    containerClient.getCluster(null, TEST_ZONE, TEST_CLUSTER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClustersErrorWithNullZone() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, null);
    containerClient.getCluster(TEST_PROJECT_ID, null, TEST_CLUSTER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClustersErrorWithNullClusterName() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, null);
    containerClient.getCluster(TEST_PROJECT_ID, TEST_ZONE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClustersErrorWithEmptyProjectId() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, null);
    containerClient.getCluster("", TEST_ZONE, TEST_CLUSTER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClustersErrorWithEmptyZone() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, null);
    containerClient.getCluster(TEST_PROJECT_ID, "", TEST_CLUSTER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetClustersErrorWithEmptyClusterName() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, null);
    containerClient.getCluster(TEST_PROJECT_ID, TEST_ZONE, "");
  }

  @Test
  public void testGetClusterReturnsProperlyWhenClusterExists() throws IOException {
    ContainerClient containerClient = setUpGetClient(TEST_CLUSTER, null);
    Cluster expected = new Cluster().setName(TEST_CLUSTER);
    Cluster response = containerClient.getCluster(TEST_PROJECT_ID, TEST_ZONE, TEST_CLUSTER);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test(expected = IOException.class)
  public void testGetClusterThrowsErrorWhenClusterDoesntExists() throws IOException {
    ContainerClient containerClient = setUpGetClient(null, new IOException());
    containerClient.getCluster(TEST_PROJECT_ID, TEST_ZONE, TEST_CLUSTER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListClustersErrorWithNullProjectId() throws IOException {
    ContainerClient containerClient = setUpListClient(null, null);
    containerClient.listClusters(null, TEST_ZONE);
  }

  @Test
  public void testListClustersErrorWithNullZone() throws IOException {
    ContainerClient containerClient = setUpListClient(ImmutableList.of(TEST_CLUSTER), null);
    List<Cluster> expected = initClusterList(ImmutableList.of(TEST_CLUSTER));
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, null);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListClustersErrorWithEmptyProjectId() throws IOException {
    ContainerClient containerClient = setUpListClient(null, null);
    containerClient.listClusters("", TEST_ZONE);
  }

  @Test
  public void testListClustersWithEmptyZoneClustersExist() throws IOException {
    ContainerClient containerClient = setUpListClient(ImmutableList.of(TEST_CLUSTER), null);
    List<Cluster> expected = initClusterList(ImmutableList.of(TEST_CLUSTER));
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, "");
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersWithValidInputsWhenClustersExist() throws IOException {
    ContainerClient containerClient = setUpListClient(ImmutableList.of(TEST_CLUSTER), null);
    List<Cluster> expected = initClusterList(ImmutableList.of(TEST_CLUSTER));
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersSortedWithMultipleClusters() throws IOException {
    ContainerClient containerClient =
        setUpListClient(ImmutableList.of(TEST_CLUSTER, OTHER_CLUSTER), null);
    List<Cluster> expected = initClusterList(ImmutableList.of(OTHER_CLUSTER, TEST_CLUSTER));
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersWithValidInputsWhenClustersIsNull() throws IOException {
    ContainerClient containerClient = setUpListClient(null, null);
    List<Cluster> expected = ImmutableList.of();
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test
  public void testListClustersEmptyWithValidProjectWithNoClusters() throws IOException {
    ContainerClient containerClient = setUpListClient(ImmutableList.of(), null);
    List<Cluster> expected = ImmutableList.of();
    List<Cluster> response = containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE);
    assertNotNull(response);
    assertEquals(expected, response);
  }

  @Test(expected = IOException.class)
  public void testListClustersThrowsErrorWithInvalidProject() throws IOException {
    ContainerClient containerClient = setUpListClient(null, new IOException());
    containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE);
  }

  private static List<Cluster> initClusterList(List<String> clusterNames) {
    List<Cluster> clusters = new ArrayList<>();
    clusterNames.forEach(e -> clusters.add(new Cluster().setName(e).setZone(TEST_ZONE)));
    return clusters;
  }

  private static ContainerClient setUpClient(Clusters.List listCall, Clusters.Get getCall)
      throws IOException {
    Container container = Mockito.mock(Container.class);
    Container.Projects projects = Mockito.mock(Container.Projects.class);
    Container.Projects.Zones zones = Mockito.mock(Container.Projects.Zones.class);
    Clusters clusters = Mockito.mock(Container.Projects.Zones.Clusters.class);
    Mockito.when(container.projects()).thenReturn(projects);
    Mockito.when(projects.zones()).thenReturn(zones);
    Mockito.when(zones.clusters()).thenReturn(clusters);
    if (getCall != null) {
      Mockito.when(clusters.get(anyString(), anyString(), anyString())).thenReturn(getCall);
    }
    if (listCall != null) {
      Mockito.when(clusters.list(anyString(), anyString())).thenReturn(listCall);
    }
    return new ContainerClient(container);
  }

  private static ContainerClient setUpGetClient(String clusterName, IOException ioException)
      throws IOException {
    Clusters.Get getCall = Mockito.mock(Clusters.Get.class);
    ContainerClient client = setUpClient(null, getCall);

    if (ioException != null) {
      Mockito.when(getCall.execute()).thenThrow(ioException);
    } else {
      Mockito.when(getCall.execute()).thenReturn(new Cluster().setName(clusterName));
    }
    return client;
  }

  private static ContainerClient setUpListClient(List<String> clusters, IOException ioException)
      throws IOException {
    Clusters.List listCall = Mockito.mock(Clusters.List.class);
    ContainerClient client = setUpClient(listCall, null);

    if (ioException != null) {
      Mockito.when(listCall.execute()).thenThrow(ioException);
    } else if (clusters == null) {
      Mockito.when(listCall.execute()).thenReturn(new ListClustersResponse().setClusters(null));
    } else {
      Mockito.when(listCall.execute())
          .thenReturn(new ListClustersResponse().setClusters(initClusterList(clusters)));
    }
    return client;
  }
}
