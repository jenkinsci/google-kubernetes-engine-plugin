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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Test {@link ComputeClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ComputeClientTest {
  private static final String TEST_PROJECT_ID = "test-project";
  private static final String TEST_ZONE_A = "us-central1-a";
  private static final String TEST_ZONE_B = "us-west1-b";
  private static final String TEST_ZONE_C = "us-east1-c";
  private static final List<String> ZONE_NAMES =
      Arrays.asList(TEST_ZONE_B, TEST_ZONE_A, TEST_ZONE_C);

  @Test(expected = IllegalArgumentException.class)
  public void testGetZonesExceptionWhenProjectIdNull() throws IOException {
    ComputeClient computeClient = setUpClient(null, null);
    computeClient.getZones(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetZonesExceptionWhenProjectIdEmpty() throws IOException {
    ComputeClient computeClient = setUpClient(null, null);
    computeClient.getZones("");
  }

  @Test(expected = IOException.class)
  public void testGetZonesExceptionWhenIOException() throws IOException {
    ComputeClient computeClient = setUpClient(null, new IOException());
    computeClient.getZones(TEST_PROJECT_ID);
  }

  @Test
  public void testGetZonesReturnsEmptyWhenZoneListEmpty() throws IOException {
    ComputeClient computeClient = setUpClient(ImmutableList.of(), null);
    List<Zone> zones = computeClient.getZones(TEST_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(ImmutableList.of(), zones);
  }

  @Test
  public void testGetZonesReturnsEmptyWhenZoneListNull() throws IOException {
    ComputeClient computeClient = setUpClient(null, null);
    List<Zone> zones = computeClient.getZones(TEST_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(ImmutableList.of(), zones);
  }

  @Test
  public void testGetZonesReturnsWhenSingleZone() throws IOException {
    ComputeClient computeClient = setUpClient(ImmutableList.of(TEST_ZONE_C), null);
    List<Zone> expectedZones = initZoneList(ImmutableList.of(TEST_ZONE_C));
    List<Zone> zones = computeClient.getZones(TEST_PROJECT_ID);
    assertNotNull(zones);
    assertEquals(expectedZones, zones);
  }

  @Test
  public void testGetZonesReturnsAllSortedWhenMultipleZones() throws IOException {
    ComputeClient computeClient = setUpClient(ZONE_NAMES, null);
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

  private static ComputeClient setUpClient(List<String> initial, IOException ioException)
      throws IOException {
    Compute compute = Mockito.mock(Compute.class);
    Zones zones = Mockito.mock(Zones.class);
    Mockito.when(compute.zones()).thenReturn(zones);
    Zones.List zonesListCall = Mockito.mock(Zones.List.class);
    Mockito.when(zones.list(anyString())).thenReturn(zonesListCall);

    if (ioException != null) {
      Mockito.when(zonesListCall.execute()).thenThrow(ioException);
    } else if (initial == null) {
      Mockito.when(zonesListCall.execute()).thenReturn(new ZoneList().setItems(null));
    } else {
      Mockito.when(zonesListCall.execute())
          .thenReturn(new ZoneList().setItems(initZoneList(initial)));
    }
    return new ComputeClient(compute);
  }
}
