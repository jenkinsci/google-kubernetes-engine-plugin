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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Test {@link ComputeClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ComputeClientTest {
  private static final String TEST_PROJECT_ID = "test-project";
  private static final String NULL_PROJECT_ID = "wrong-project";
  private static final String EMPTY_PROJECT_ID = "empty-project-id";
  private static final String ERROR_PROJECT_ID = "error-project-id";
  private static final List<String> ZONE_NAMES =
      Arrays.asList("us-west-1b", "us-central-1a", "us-east-1c");
  private static ComputeClient computeClient;

  @BeforeClass
  public static void init() throws IOException {
    List<Zone> listOfZones = initZoneList(ZONE_NAMES);
    // Mock zones
    Compute compute = Mockito.mock(Compute.class);

    Zones zones = Mockito.mock(Zones.class);
    Zones.List zonesListCall = Mockito.mock(Zones.List.class);
    ZoneList zoneList = Mockito.mock(ZoneList.class);
    Mockito.when(compute.zones()).thenReturn(zones);
    Mockito.when(zones.list(TEST_PROJECT_ID)).thenReturn(zonesListCall);
    Mockito.when(zonesListCall.execute()).thenReturn(zoneList);
    Mockito.when(zoneList.getItems()).thenReturn(listOfZones);

    Zones.List nullZonesListCall = Mockito.mock(Zones.List.class);
    ZoneList nullZoneList = Mockito.mock(ZoneList.class);
    Mockito.when(zones.list(NULL_PROJECT_ID)).thenReturn(nullZonesListCall);
    Mockito.when(nullZonesListCall.execute()).thenReturn(nullZoneList);
    Mockito.when(nullZoneList.getItems()).thenReturn(null);

    Zones.List emptyZonesListCall = Mockito.mock(Zones.List.class);
    ZoneList emptyZoneList = Mockito.mock(ZoneList.class);
    Mockito.when(zones.list(EMPTY_PROJECT_ID)).thenReturn(emptyZonesListCall);
    Mockito.when(emptyZonesListCall.execute()).thenReturn(emptyZoneList);
    Mockito.when(emptyZoneList.getItems()).thenReturn(ImmutableList.of());

    Zones.List errorZonesListCall = Mockito.mock(Zones.List.class);
    Mockito.when(zones.list(ERROR_PROJECT_ID)).thenReturn(errorZonesListCall);
    Mockito.when(errorZonesListCall.execute()).thenThrow(new IOException());
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
  public void testGetZonesReturnsAllSortedWhenProjectIdCorrect() throws IOException {
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
}
