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

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Zones;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.compute.model.ZoneList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/** Test {@link ComputeClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ComputeClientTest {
  private static final String TEST_PROJECT_ID = "test-project";
  private static final String OTHER_PROJECT_ID = "other-project";
  private static final String NULL_PROJECT_ID = "wrong-project";
  private static final String EMPTY_PROJECT_ID = "empty-project-id";
  private static final String ERROR_PROJECT_ID = "error-project-id";
  private static final String TEST_ZONE_A = "us-central1-a";
  private static final String TEST_ZONE_B = "us-west1-b";
  private static final String TEST_ZONE_C = "us-east1-c";
  private static final List<String> ZONE_NAMES =
      Arrays.asList(TEST_ZONE_B, TEST_ZONE_A, TEST_ZONE_C);
  private static ComputeClient computeClient;

  @BeforeClass
  public static void init() throws IOException {
    Compute compute = Mockito.mock(Compute.class);
    Zones zones = Mockito.mock(Zones.class);
    Mockito.when(compute.zones()).thenReturn(zones);
    Mockito.when(zones.list(anyString()))
        .thenAnswer(
            mockZonesListAnswer(
                new ImmutableMap.Builder<String, List<Zone>>()
                    .put(TEST_PROJECT_ID, initZoneList(ZONE_NAMES))
                    .put(OTHER_PROJECT_ID, initZoneList(ImmutableList.of(TEST_ZONE_C)))
                    .put(EMPTY_PROJECT_ID, ImmutableList.of())
                    .build()));
    computeClient = new ComputeClient(compute);
  }

  @Test(expected = NullPointerException.class)
  public void testGetZonesExceptionWhenProjectIdNull() throws NullPointerException, IOException {
    computeClient.getZones(null);
  }

  @Test(expected = IOException.class)
  public void testGetZonesExceptionWhenIOException() throws IOException {
    computeClient.getZones(ERROR_PROJECT_ID);
  }

  @Test
  public void testGetZonesReturnsEmptyWhenZoneListEmpty() throws IOException {
    List<Zone> zones = computeClient.getZones(EMPTY_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(ImmutableList.of(), zones);
  }

  @Test
  public void testGetZonesReturnsEmptyWhenZoneListNull() throws IOException {
    List<Zone> zones = computeClient.getZones(NULL_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(ImmutableList.of(), zones);
  }

  @Test
  public void testGetZonesReturnsWhenSingleZone() throws IOException {
    List<Zone> zones = computeClient.getZones(OTHER_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(initZoneList(ImmutableList.of(TEST_ZONE_C)), zones);
  }

  @Test
  public void testGetZonesReturnsAllSortedWhenMultipleZones() throws IOException {
    List<Zone> expectedZones = initZoneList(ZONE_NAMES);
    expectedZones.sort(Comparator.comparing(Zone::getName));
    List<Zone> zones = computeClient.getZones(TEST_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(expectedZones, zones);
  }

  private static List<Zone> initZoneList(List<String> names) {
    List<Zone> result = new ArrayList<>();
    names.forEach(z -> result.add(new Zone().setName(z)));
    return result;
  }

  private static Answer<Zones.List> mockZonesListAnswer(
      ImmutableMap<String, List<Zone>> projectIdToZones) {
    return invocation -> {
      Object[] args = invocation.getArguments();
      String projectId = (String) args[0];

      Zones.List listCall = Mockito.mock(Zones.List.class);
      if (projectIdToZones.containsKey(projectId)) {
        List<Zone> zones = new ArrayList<>(projectIdToZones.get(projectId));
        Mockito.when(listCall.execute()).thenReturn(new ZoneList().setItems(zones));
      } else if (ERROR_PROJECT_ID.equals(projectId)) {
        Mockito.when(listCall.execute()).thenThrow(new IOException());
      } else {
        Mockito.when(listCall.execute()).thenReturn(new ZoneList().setItems(null));
      }
      return listCall;
    };
  }
}
