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

import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.EMPTY_NAME;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.EMPTY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.container.model.Cluster;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.CloudResourceManagerClient;
import com.google.jenkins.plugins.k8sengine.client.ComputeClient;
import com.google.jenkins.plugins.k8sengine.client.ContainerClient;
import hudson.AbortException;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import jenkins.model.Jenkins;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesEngineBuilderTest {
  private static final String TEST_ZONE_A = "us-west1-a";
  private static final String TEST_ZONE_B = "us-central1-b";
  private static final String TEST_PROJECT_ID = "test-project-id";
  private static final String OTHER_PROJECT_ID = "other-project-id";
  private static final String NO_ZONES_PROJECT_ID = "no-zones-project-id";
  private static final String ERROR_PROJECT_ID = "error-project-id";
  private static final String TEST_CLUSTER = "testCluster";
  private static final String OTHER_CLUSTER = "otherCluster";
  private static final String TEST_CREDENTIALS_ID = "test-credentials-id";
  private static final String OTHER_CREDENTIALS_ID = "other-credentials-id";
  private static final String NO_DEFAULT_PROJECT_CREDENTIALS_ID = "empty-project-credentials-id";
  private static final String PROJECT_ERROR_CREDENTIALS_ID = "project-error-credentials-id";
  private static final String ERROR_CREDENTIALS_ID = "error-credentials-id";
  private static final String TEST_ERROR_MESSAGE = "error";

  private static Jenkins jenkins;
  private static KubernetesEngineBuilder.DescriptorImpl descriptor;

  @BeforeClass
  public static void init() throws IOException {
    descriptor = Mockito.spy(KubernetesEngineBuilder.DescriptorImpl.class);
    jenkins = Mockito.mock(Jenkins.class);
    ComputeClient computeClient = Mockito.mock(ComputeClient.class);
    Zone testZoneA = new Zone().setName(TEST_ZONE_A);
    Zone testZoneB = new Zone().setName(TEST_ZONE_B);
    Mockito.when(computeClient.getZones(TEST_PROJECT_ID))
        .thenReturn(ImmutableList.of(testZoneA, testZoneB));
    Mockito.when(computeClient.getZones(OTHER_PROJECT_ID)).thenReturn(ImmutableList.of(testZoneB));
    Mockito.when(computeClient.getZones(NO_ZONES_PROJECT_ID)).thenReturn(ImmutableList.of());
    Mockito.when(computeClient.getZones(ERROR_PROJECT_ID)).thenThrow(new IOException());

    Project testProject = new Project().setProjectId(TEST_PROJECT_ID);
    Project otherProject = new Project().setProjectId(OTHER_PROJECT_ID);
    CloudResourceManagerClient cloudResourceManagerClient =
        Mockito.mock(CloudResourceManagerClient.class);
    Mockito.when(cloudResourceManagerClient.getAccountProjects())
        .thenReturn(ImmutableList.of(otherProject, testProject));

    CloudResourceManagerClient otherCloudResourceManagerClient =
        Mockito.mock(CloudResourceManagerClient.class);
    Mockito.when(otherCloudResourceManagerClient.getAccountProjects())
        .thenReturn(ImmutableList.of(otherProject));

    Cluster testCluster = new Cluster().setName(TEST_CLUSTER);
    Cluster otherCluster = new Cluster().setName(OTHER_CLUSTER);
    ContainerClient containerClient = Mockito.mock(ContainerClient.class);
    Mockito.when(containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE_A))
        .thenReturn(ImmutableList.of(testCluster));
    Mockito.when(containerClient.listClusters(OTHER_PROJECT_ID, TEST_ZONE_A))
        .thenReturn(ImmutableList.of());
    Mockito.when(containerClient.listClusters(TEST_PROJECT_ID, TEST_ZONE_B))
        .thenReturn(ImmutableList.of(otherCluster, testCluster));
    Mockito.when(containerClient.listClusters(ERROR_PROJECT_ID, TEST_ZONE_A))
        .thenThrow(new IOException());

    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(clientFactory.getDefaultProjectId()).thenReturn(TEST_PROJECT_ID);
    Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(cloudResourceManagerClient);
    Mockito.when(clientFactory.computeClient()).thenReturn(computeClient);
    Mockito.when(clientFactory.containerClient()).thenReturn(containerClient);
    Mockito.doReturn(clientFactory).when(descriptor).getClientFactory(jenkins, TEST_CREDENTIALS_ID);

    ClientFactory otherClientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(otherClientFactory.getDefaultProjectId()).thenReturn(TEST_PROJECT_ID);
    Mockito.when(otherClientFactory.cloudResourceManagerClient())
        .thenReturn(otherCloudResourceManagerClient);
    Mockito.doReturn(otherClientFactory)
        .when(descriptor)
        .getClientFactory(jenkins, OTHER_CREDENTIALS_ID);

    ClientFactory noDefaultProjectClientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(noDefaultProjectClientFactory.getDefaultProjectId()).thenReturn("");
    Mockito.when(noDefaultProjectClientFactory.cloudResourceManagerClient())
        .thenReturn(cloudResourceManagerClient);
    Mockito.doReturn(noDefaultProjectClientFactory)
        .when(descriptor)
        .getClientFactory(jenkins, NO_DEFAULT_PROJECT_CREDENTIALS_ID);

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

  @Test
  public void testDoFillProjectIdItemsErrorMessageWithAbortException() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, ERROR_CREDENTIALS_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillProjectIdItemsErrorMessageWithIOException() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_ProjectIDFillError()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillProjectIdItems(jenkins, null, PROJECT_ERROR_CREDENTIALS_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillProjectIdItemsEmptyWithEmptyCredentialsId() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, null);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsId() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
            ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_PROJECT_ID);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsIdAndPreviousValueAndDefault() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
            ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
    ListBoxModel result =
        descriptor.doFillProjectIdItems(jenkins, OTHER_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_PROJECT_ID);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsIdAndPreviousValueAndEmptyDefault() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
            ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
    ListBoxModel result =
        descriptor.doFillProjectIdItems(
            jenkins, OTHER_PROJECT_ID, NO_DEFAULT_PROJECT_CREDENTIALS_ID);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_PROJECT_ID);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsIdMissingDefaultProject() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
            ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, OTHER_CREDENTIALS_ID);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_PROJECT_ID);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsAndEmptyProject() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
            ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
    ListBoxModel result =
        descriptor.doFillProjectIdItems(
            jenkins, OTHER_PROJECT_ID, NO_DEFAULT_PROJECT_CREDENTIALS_ID);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_PROJECT_ID);
  }

  @Test
  public void testDoCheckProjectIdMessageWithEmptyProjectID() {
    FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ProjectIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithEmptyCredentialsID() {
    FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, null);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectCredentialIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithAbortException() {
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, ERROR_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithIOException() {
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, PROJECT_ERROR_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithAbortExceptionAndEmptyProjectId() {
    FormValidation result = descriptor.doCheckProjectId(jenkins, null, ERROR_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithIOExceptionAndEmptyProjectId() {
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, null, PROJECT_ERROR_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithNoProjects() {
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, OTHER_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithWrongProjects() {
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, OTHER_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithValidProject() {
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok().getMessage(), result.getMessage());
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyProjectId() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, null);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyCredentialsId() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillZoneItems(jenkins, null, null, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsWithValidArguments() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_ZONE_A, TEST_ZONE_B),
            ImmutableList.of(EMPTY_VALUE, TEST_ZONE_A, TEST_ZONE_B));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_ZONE_A);
  }

  @Test
  public void testDoFillZoneItemsWithValidArgumentsAndPreviousValue() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_ZONE_A, TEST_ZONE_B),
            ImmutableList.of(EMPTY_VALUE, TEST_ZONE_A, TEST_ZONE_B));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, TEST_ZONE_B, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_ZONE_B);
  }

  @Test
  public void testDoFillZoneItemsEmptyWithValidArgumentsNoZones() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, NO_ZONES_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsErrorMessageWithAbortException() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, ERROR_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsErrorMessageWithIOException() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_ZoneFillError()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, ERROR_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyZone() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyProjectId() {
    FormValidation result = descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, null);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneProjectIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyCredentialsId() {
    FormValidation result = descriptor.doCheckZone(jenkins, TEST_ZONE_A, null, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneCredentialIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithNoAvailableZones() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, NO_ZONES_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneNotInProject(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithNonMatchingZones() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, OTHER_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneNotInProject(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithIOException() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, ERROR_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithAbortExceptionAndEmptyZone() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, ERROR_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckZonedMessageWithIOExceptionAndEmptyZone() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, TEST_CREDENTIALS_ID, ERROR_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneOKWithMatchingZones() {
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoFillClusterNameItemsEmptyWithEmptyCredentialsId() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(jenkins, null, null, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterNameItemsEmptyWithEmptyProjectId() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(jenkins, null, TEST_CREDENTIALS_ID, null, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterNameItemsEmptyWithEmptyZone() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, null);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterNameItemsErrorMessageWithAbortException() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, null, ERROR_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterNameItemsWithInvalidClusterName() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, "wrong", TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_CLUSTER);
  }

  @Test
  public void testDoFillClusterNameItemsEmptyWithValidInputsNoClusters() {
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, null, TEST_CREDENTIALS_ID, OTHER_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterNameItemsWithValidInputsOneCluster() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_CLUSTER);
  }

  @Test
  public void testDoFillClusterNameItemsWithValidInputsMultipleClusters() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_CLUSTER, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, OTHER_CLUSTER, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_B);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_CLUSTER);
  }

  @Test
  public void testDoFillClusterNameItemsWithValidInputsMultipleClustersAndPreviousValue() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_CLUSTER, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, OTHER_CLUSTER, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_B);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_CLUSTER);
  }

  @Test
  public void testDoFillClusterNameItemsErrorMessageWithIOException() {
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_ClusterFillError()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterNameItems(
            jenkins, null, TEST_CREDENTIALS_ID, ERROR_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoCheckClusterNameMessageWithEmptyClusterName() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithEmptyCredentialsId() {
    FormValidation result =
        descriptor.doCheckClusterName(jenkins, TEST_CLUSTER, null, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ClusterCredentialIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithEmptyProjectId() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, null, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterProjectIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithEmptyZone() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, null);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterZoneRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithValidInputsNoClusters() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, OTHER_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_NoClusterInProjectZone(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameOkWithValidInputs() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(FormValidation.ok().getMessage(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithAbortException() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, TEST_CLUSTER, ERROR_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithIOException() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, ERROR_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithAbortExceptionAndEmptyClusterName() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, null, ERROR_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterNameMessageWithIOExceptionAndEmptyClusterName() {
    FormValidation result =
        descriptor.doCheckClusterName(
            jenkins, null, TEST_CREDENTIALS_ID, ERROR_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterVerificationError(), result.getMessage());
  }

  private static ListBoxModel initExpected(
      List<String> expectedNames, List<String> expectedValues) {
    ListBoxModel expected = new ListBoxModel();
    for (int i = 0; i < expectedNames.size(); i++) {
      expected.add(expectedNames.get(i), expectedValues.get(i));
    }
    return expected;
  }

  private static void assertListBoxModelEquals(ListBoxModel expected, ListBoxModel result) {
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i).name, result.get(i).name);
      assertEquals(expected.get(i).value, result.get(i).value);
    }
  }

  private static void assertValueSelected(ListBoxModel result, String expectedValue) {
    Optional<Option> expectedOption =
        result.stream().filter(i -> expectedValue.equals(i.value)).findFirst();
    expectedOption.ifPresent(option -> assertTrue(option.selected));
  }
}
