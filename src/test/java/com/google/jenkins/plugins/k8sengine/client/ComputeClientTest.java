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
import static org.junit.Assert.assertTrue;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Zones;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.compute.model.ZoneList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Test {@link ComputeClient}. */
@RunWith(MockitoJUnitRunner.class)
public class ComputeClientTest {

  private static final String TEST_PROJECT_ID = "test-project";
  private static final String WRONG_PROJECT_ID = "wrong-project";
  private static final List<String> ZONE_NAMES =
      Arrays.asList("us-west-1b", "us-central-1a", "us-east-1c");
  private static ComputeClient computeClient;

  private static List<Zone> listOfZones;

  @BeforeClass
  public static void init() throws IOException {
    listOfZones = new ArrayList<>();

    // Mock zones
    Compute compute = Mockito.mock(Compute.class);
    Zones zones = Mockito.mock(Zones.class);
    Zones.List zonesListCall = Mockito.mock(Zones.List.class);
    ZoneList zoneList = Mockito.mock(ZoneList.class);
    Zones.List wrongZonesListCall = Mockito.mock(Zones.List.class);
    ZoneList wrongZoneList = Mockito.mock(ZoneList.class);
    Mockito.when(compute.zones()).thenReturn(zones);
    Mockito.when(zones.list(TEST_PROJECT_ID)).thenReturn(zonesListCall);
    Mockito.when(zonesListCall.execute()).thenReturn(zoneList);
    Mockito.when(zoneList.getItems()).thenReturn(listOfZones);
    Mockito.when(zones.list(WRONG_PROJECT_ID)).thenReturn(wrongZonesListCall);
    Mockito.when(wrongZonesListCall.execute()).thenReturn(wrongZoneList);
    Mockito.when(wrongZoneList.getItems()).thenReturn(null);
    computeClient = new ComputeClient(compute);
  }

  @Before
  public void beforeTest() {
    listOfZones.clear();
  }

  @Test
  public void testGetZonesReturnsEmptyWhenEmpty() throws IOException {
    testGetZones(TEST_PROJECT_ID, new ArrayList<>());
  }

  @Test
  public void testGetZonesReturnsEmptyWhenProjectIdWrong() throws IOException {
    testGetZones(WRONG_PROJECT_ID, new ArrayList<>());
  }

  @Test
  public void testGetZonesReturnsAllSortedWhenProjectIdCorrect() throws IOException {
    ZONE_NAMES.forEach(name -> listOfZones.add(new Zone().setName(name)));
    testGetZones(TEST_PROJECT_ID, ZONE_NAMES);
  }

  private void testGetZones(String projectId,List<String> zoneNames) throws IOException {
    List<Zone> zones = computeClient.getZones(projectId);
    assertNotNull("zones was null.", zones);
    String message = String.format("Expected %d zones but %d zones retrieved.", zones.size(), zoneNames.size());
    assertEquals(message, zoneNames.size(), zones.size());

    if (zoneNames.size() > 0) {
      zoneNames.sort(String::compareTo);
      for (int i = 0; i < zoneNames.size(); i++) {
        message = String.format("Zones not sorted. Expected %s but was %s.", zoneNames.get(i), zones.get(i).getName());
        assertEquals(message, zoneNames.get(i), zones.get(i).getName());
      }
    }
  }
}
