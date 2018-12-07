/*
 * Copyright 2018 Google Inc.
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

import com.google.api.services.container.Container;
import com.google.api.services.container.model.Cluster;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/** Tests {@link ContainerClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ContainerClientTest {
  private ContainerClient containerClient;
  @Mock private Container.Projects.Zones.Clusters clusters;

  @Before
  public void init() throws Exception {
    Container container = Mockito.mock(Container.class);
    ;
    Container.Projects projects = Mockito.mock(Container.Projects.class);
    Container.Projects.Zones zones = Mockito.mock(Container.Projects.Zones.class);
    Mockito.when(container.projects()).thenReturn(projects);
    Mockito.when(projects.zones()).thenReturn(zones);
    Mockito.when(zones.clusters()).thenReturn(clusters);
    this.containerClient = new ContainerClient(container);
  }

  @Test
  public void testGetClusterReturnsProperlyWhenClusterExists() throws Exception {
    Cluster testCluster = new Cluster();
    testCluster.setName("testCluster");
    testCluster.setZone("us-central1-c");
    Mockito.when(clusters.get(Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
        .thenAnswer(
            mockClustersGetAnswer(
                new ImmutableMap.Builder<String, ImmutableList<Cluster>>()
                    .put("testProject", ImmutableList.<Cluster>of(testCluster))
                    .build()));

    Cluster response = containerClient.getCluster("testProject", "us-central1-c", "testCluster");
    assertNotNull(response);
    assertEquals(response, testCluster);
  }

  @Test(expected = IOException.class)
  public void testGetClusterThrowsErrorWhenClusterDoesntExists() throws Exception {
    Cluster testCluster = new Cluster();
    testCluster.setName("testCluster");
    testCluster.setZone("us-central1-c");
    Mockito.when(clusters.get(Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
        .thenAnswer(
            mockClustersGetAnswer(
                new ImmutableMap.Builder<String, ImmutableList<Cluster>>()
                    .put("testProject", ImmutableList.<Cluster>of(testCluster))
                    .build()));

    containerClient.getCluster("testProject", "us-central-c", "otherCluster");
  }

  private Answer<Container.Projects.Zones.Clusters.Get> mockClustersGetAnswer(
      ImmutableMap<String, ImmutableList<Cluster>> projectToClusters) {
    return new Answer<Container.Projects.Zones.Clusters.Get>() {
      @Override
      public Container.Projects.Zones.Clusters.Get answer(InvocationOnMock invocation)
          throws IOException {
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
      }
    };
  }
}
