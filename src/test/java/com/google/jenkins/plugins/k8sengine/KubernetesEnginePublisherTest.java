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

package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.api.services.compute.model.Zone;
import com.google.common.base.Strings;
import com.google.jenkins.plugins.k8sengine.client.ComputeClient;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesEnginePublisherTest {
  private static final String TEST_ZONE_A = "us-west1-a";
  private static final String TEST_ZONE_B = "us-west1-b";
  private static final String TEST_PROJECT_ID = "test-project-id";
  private static final String ERROR_PROJECT_ID = "error-project-id";
  private static final String TEST_CREDENTIALS_ID = "test-credentials-id";

  private static List<Zone> listOfZones;
  private static Jenkins jenkins;
  private KubernetesEnginePublisher.DescriptorImpl descriptor =
      new KubernetesEnginePublisher.DescriptorImpl();

  @BeforeClass
  public static void init() throws IOException {
    listOfZones = new ArrayList<>();
    ComputeClient computeClient = Mockito.mock(ComputeClient.class);
    Mockito.when(computeClient.getZones(TEST_PROJECT_ID)).thenReturn(listOfZones);
    Mockito.when(computeClient.getZones(ERROR_PROJECT_ID)).thenThrow(new IOException());
    KubernetesEnginePublisher.DescriptorImpl.setComputeClient(computeClient);
    jenkins = Mockito.mock(Jenkins.class);
  }

  @Before
  public void before() {
    listOfZones.clear();
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyArguments() {
    listOfZones.add(new Zone().setName(TEST_ZONE_A));
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID);
    testEmptyResult(zones);
    zones = descriptor.doFillZoneItems(jenkins, TEST_PROJECT_ID, null);
    testEmptyResult(zones);
    zones = descriptor.doFillZoneItems(jenkins, null, null);
    testEmptyResult(zones);
  }

  @Test
  public void testDoFillZoneItemsWithValidArguments() {
    listOfZones.add(new Zone().setName(TEST_ZONE_A));
    listOfZones.add(new Zone().setName(TEST_ZONE_B));
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(zones);
    assertEquals(2, zones.size());
    assertEquals(TEST_ZONE_A, zones.get(0).value);
    assertEquals(TEST_ZONE_B, zones.get(1).value);
  }

  @Test
  public void testDoFillZoneItemsWithValidArgumentsNoZones() {
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    testEmptyResult(zones);
  }

  @Test
  public void testDoFillZoneItemsIOException() {
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, ERROR_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(zones);
    assertEquals(1, zones.size());
    assertEquals(Messages.KubernetesEnginePublisher_ZoneFillError(), zones.get(0).name);
    assertTrue(Strings.isNullOrEmpty(zones.get(0).value));
  }

  private void testEmptyResult(ListBoxModel zones) {
    assertNotNull(zones);
    assertEquals(1, zones.size());
    assertTrue(Strings.isNullOrEmpty(zones.get(0).value));
  }
}
