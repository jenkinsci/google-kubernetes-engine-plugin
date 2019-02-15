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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.compute.model.Zone;
import com.google.common.base.Strings;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.CloudResourceManagerClient;
import com.google.jenkins.plugins.k8sengine.client.ComputeClient;
import hudson.AbortException;
import hudson.util.FormValidation;
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
  private static final String OTHER_PROJECT_ID = "other-project-id";
  private static final String ERROR_PROJECT_ID = "error-project-id";
  private static final String TEST_CREDENTIALS_ID = "test-credentials-id";
  private static final String EMPTY_PROJECT_CREDENTIALS_ID = "empty-project-credentials-id";
  private static final String PROJECT_ERROR_CREDENTIALS_ID = "project-error-credentials-id";
  private static final String ERROR_CREDENTIALS_ID = "error-credentials-id";
  private static final String TEST_ERROR_MESSAGE = "error";

  private static List<Zone> listOfZones;
  private static List<Project> listOfProjects;
  private static Jenkins jenkins;
  private static KubernetesEnginePublisher.DescriptorImpl descriptor;

  @BeforeClass
  public static void init() throws IOException {
    listOfZones = new ArrayList<>();
    listOfProjects = new ArrayList<>();
    descriptor = Mockito.spy(KubernetesEnginePublisher.DescriptorImpl.class);
    jenkins = Mockito.mock(Jenkins.class);
    ComputeClient computeClient = Mockito.mock(ComputeClient.class);
    Mockito.when(computeClient.getZones(TEST_PROJECT_ID)).thenReturn(listOfZones);
    Mockito.when(computeClient.getZones(ERROR_PROJECT_ID)).thenThrow(new IOException());

    CloudResourceManagerClient cloudResourceManagerClient =
        Mockito.mock(CloudResourceManagerClient.class);
    Mockito.when(cloudResourceManagerClient.getAccountProjects()).thenReturn(listOfProjects);
    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(clientFactory.getDefaultProjectId()).thenReturn(TEST_PROJECT_ID);
    Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(cloudResourceManagerClient);
    Mockito.when(clientFactory.computeClient()).thenReturn(computeClient);
    Mockito.doReturn(clientFactory).when(descriptor).getClientFactory(jenkins, TEST_CREDENTIALS_ID);

    ClientFactory emptyProjectClientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(emptyProjectClientFactory.getDefaultProjectId()).thenReturn("");
    Mockito.when(emptyProjectClientFactory.cloudResourceManagerClient())
        .thenReturn(cloudResourceManagerClient);
    Mockito.doReturn(emptyProjectClientFactory)
        .when(descriptor)
        .getClientFactory(jenkins, EMPTY_PROJECT_CREDENTIALS_ID);

    CloudResourceManagerClient errorCloudResourceManagerClient =
        Mockito.mock(CloudResourceManagerClient.class);
    Mockito.when(errorCloudResourceManagerClient.getAccountProjects()).thenThrow(new IOException());
    ClientFactory projectErrorClientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(projectErrorClientFactory.getDefaultProjectId()).thenReturn(ERROR_PROJECT_ID);
    Mockito.when(projectErrorClientFactory.cloudResourceManagerClient())
        .thenReturn(errorCloudResourceManagerClient);
    Mockito.doReturn(projectErrorClientFactory)
        .when(descriptor)
        .getClientFactory(jenkins, PROJECT_ERROR_CREDENTIALS_ID);

    Mockito.doThrow(new AbortException(TEST_ERROR_MESSAGE))
        .when(descriptor)
        .getClientFactory(jenkins, ERROR_CREDENTIALS_ID);
  }

  @Before
  public void before() {
    listOfZones.clear();
    listOfProjects.clear();
  }

  @Test
  public void testDoFillProjectIdItemsErrorMessageWithAbortException() {
    listOfProjects.add(new Project().setProjectId(TEST_PROJECT_ID));
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, ERROR_CREDENTIALS_ID);
    assertNotNull(projects);
    assertEquals(1, projects.size());
    assertEquals(TEST_ERROR_MESSAGE, projects.get(0).name);
    assertEquals("", projects.get(0).value);
  }

  @Test
  public void testDoFillProjectIdItemsDefaultProjectIdWithIOException() {
    listOfProjects.add(new Project().setProjectId(TEST_PROJECT_ID));
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, PROJECT_ERROR_CREDENTIALS_ID);
    assertNotNull(projects);
    assertEquals(2, projects.size());
    assertEquals("- none -", projects.get(0).name);
    assertEquals("", projects.get(0).value);
    assertFalse(projects.get(0).selected);
    assertEquals(ERROR_PROJECT_ID, projects.get(1).name);
    assertEquals(ERROR_PROJECT_ID, projects.get(1).value);
    assertTrue(projects.get(1).selected);
  }

  @Test
  public void testDoFillProjectIdItemsEmptyWithEmptyCredentialsId() {
    listOfProjects.add(new Project().setProjectId(TEST_PROJECT_ID));
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, null);
    assertNotNull(projects);
    assertEquals(1, projects.size());
    assertEquals("- none -", projects.get(0).name);
    assertEquals("", projects.get(0).value);
  }

  @Test
  public void testDoFillProjectIdItemsEmptyWithValidCredentialsIdNoProjects() {
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, TEST_CREDENTIALS_ID);
    assertNotNull(projects);
    assertEquals(1, projects.size());
    assertEquals("- none -", projects.get(0).name);
    assertEquals("", projects.get(0).value);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsId() {
    listOfProjects.add(new Project().setProjectId(TEST_PROJECT_ID));
    listOfProjects.add(new Project().setProjectId(OTHER_PROJECT_ID));
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, TEST_CREDENTIALS_ID);
    assertNotNull(projects);
    assertEquals(3, projects.size());
    assertEquals("- none -", projects.get(0).name);
    assertEquals("", projects.get(0).value);
    assertFalse(projects.get(0).selected);
    assertEquals(OTHER_PROJECT_ID, projects.get(1).name);
    assertEquals(OTHER_PROJECT_ID, projects.get(1).value);
    assertFalse(projects.get(1).selected);
    assertEquals(TEST_PROJECT_ID, projects.get(2).name);
    assertEquals(TEST_PROJECT_ID, projects.get(2).value);
    assertTrue(projects.get(2).selected);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsIdNoDefaultProject() {
    listOfProjects.add(new Project().setProjectId(OTHER_PROJECT_ID));
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, TEST_CREDENTIALS_ID);
    assertNotNull(projects);
    assertEquals(2, projects.size());
    assertEquals("- none -", projects.get(0).name);
    assertEquals("", projects.get(0).value);
    assertFalse(projects.get(0).selected);
    assertEquals(OTHER_PROJECT_ID, projects.get(1).name);
    assertEquals(OTHER_PROJECT_ID, projects.get(1).value);
    assertTrue(projects.get(1).selected);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsAndEmptyProject() {
    listOfProjects.add(new Project().setProjectId(OTHER_PROJECT_ID));
    listOfProjects.add(new Project().setProjectId(TEST_PROJECT_ID));
    ListBoxModel projects = descriptor.doFillProjectIdItems(jenkins, EMPTY_PROJECT_CREDENTIALS_ID);
    assertNotNull(projects);
    assertEquals(3, projects.size());
    assertEquals("- none -", projects.get(0).name);
    assertEquals("", projects.get(0).value);
    assertFalse(projects.get(0).selected);
    assertEquals(OTHER_PROJECT_ID, projects.get(1).name);
    assertEquals(OTHER_PROJECT_ID, projects.get(1).value);
    assertTrue(projects.get(1).selected);
    assertEquals(TEST_PROJECT_ID, projects.get(2).name);
    assertEquals(TEST_PROJECT_ID, projects.get(2).value);
    assertFalse(projects.get(2).selected);
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyProjectId() {
    listOfZones.add(new Zone().setName(TEST_ZONE_A));
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID);
    testZoneEmptyResult(zones);
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyCredentialsId() {
    listOfZones.add(new Zone().setName(TEST_ZONE_A));
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, TEST_PROJECT_ID, null);
    testZoneEmptyResult(zones);
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
  public void testDoFillZoneItemsEmptyWithValidArgumentsNoZones() {
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    testZoneEmptyResult(zones);
  }

  @Test
  public void testDoFillZoneItemsErrorMessageWithIOException() {
    ListBoxModel zones = descriptor.doFillZoneItems(jenkins, ERROR_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(zones);
    assertEquals(1, zones.size());
    assertEquals(Messages.KubernetesEnginePublisher_ZoneFillError(), zones.get(0).name);
    assertTrue(Strings.isNullOrEmpty(zones.get(0).value));
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyZone() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEnginePublisher_ZoneRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyProjectId() {
    FormValidation result = descriptor.doCheckZone(jenkins, TEST_ZONE_A, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEnginePublisher_ZoneProjectIdCredentialRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyCredentialsId() {
    FormValidation result = descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_PROJECT_ID, null);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEnginePublisher_ZoneProjectIdCredentialRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithNoAvailableZones() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEnginePublisher_ZoneNotInProject(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithNonMatchingZones() {
    listOfZones.add(new Zone().setName(TEST_ZONE_B));
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEnginePublisher_ZoneNotInProject(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithIOException() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, ERROR_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEnginePublisher_ZoneVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneOKWithMatchingZones() {
    listOfZones.add(new Zone().setName(TEST_ZONE_A));
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  private void testZoneEmptyResult(ListBoxModel zones) {
    assertNotNull(zones);
    assertEquals(1, zones.size());
    assertTrue(Strings.isNullOrEmpty(zones.get(0).value));
  }
}
