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

package com.google.jenkins.plugins.k8sengine;

import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.EMPTY_NAME;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.EMPTY_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.compute.model.Zone;
import com.google.api.services.container.model.Cluster;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.DescriptorImpl;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.CloudResourceManagerClient;
import com.google.jenkins.plugins.k8sengine.client.ComputeClient;
import com.google.jenkins.plugins.k8sengine.client.ContainerClient;
import hudson.AbortException;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.io.IOException;
import java.util.ArrayList;
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
  private static final String TEST_CLUSTER = "testCluster (us-west1-a)";
  private static final String OTHER_CLUSTER = "otherCluster (us-central1-b)";
  private static final String TEST_CREDENTIALS_ID = "test-credentials-id";

  private static Jenkins jenkins;

  // TODO(#49): Separate out tests into separate classes for better organization.

  @BeforeClass
  public static void init() {
    jenkins = Mockito.mock(Jenkins.class);
  }

  @Test
  public void testDoFillProjectIdItemsErrorMessageWithAbortException() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(), "", new AbortException(), null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillProjectIdItemsErrorMessageWithIOException() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(), "", null, new IOException());
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_ProjectIDFillError()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillProjectIdItemsEmptyWithEmptyCredentialsId() throws IOException {
    DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, null);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsId() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(
            ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), TEST_PROJECT_ID, null, null);

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
  public void testDoFillProjectIdItemsWithValidCredentialsIdAndPreviousValueAndDefault()
      throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(
            ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), TEST_PROJECT_ID, null, null);
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
  public void testDoFillProjectIdItemsWithValidCredentialsIdAndPreviousValueAndEmptyDefault()
      throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), "", null, null);
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
  public void testDoFillProjectIdItemsWithValidCredentialsIdMissingDefaultProject()
      throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID), TEST_PROJECT_ID, null, null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
            ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
    ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_PROJECT_ID);
  }

  @Test
  public void testDoFillProjectIdItemsWithValidCredentialsAndEmptyProject() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(
            ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), TEST_PROJECT_ID, null, null);
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
  public void testDoCheckProjectIdMessageWithEmptyProjectID() throws IOException {
    DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
    FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ProjectIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithEmptyCredentialsID() throws IOException {
    DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
    FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, null);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectCredentialIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithAbortException() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(), "", new AbortException(), null);
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithIOException() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(), "", null, new IOException());
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithAbortExceptionAndEmptyProjectId() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(), "", new AbortException(), null);
    FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithIOExceptionAndEmptyProjectId() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(), "", null, new IOException());
    FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithNoProjects() throws IOException {
    DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithWrongProjects() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID), "", null, null);
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential(), result.getMessage());
  }

  @Test
  public void testDoCheckProjectIdMessageWithValidProject() throws IOException {
    DescriptorImpl descriptor =
        setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), "", null, null);
    FormValidation result =
        descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok().getMessage(), result.getMessage());
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyProjectId() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, null);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsEmptyWithEmptyCredentialsId() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result = descriptor.doFillZoneItems(jenkins, null, null, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsWithValidArguments() throws IOException {
    DescriptorImpl descriptor =
        setUpZoneDescriptor(ImmutableList.of(TEST_ZONE_A, TEST_ZONE_B), null, null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_ZONE_A, TEST_ZONE_B),
            ImmutableList.of(EMPTY_VALUE, TEST_ZONE_A, TEST_ZONE_B));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, EMPTY_VALUE);
  }

  @Test
  public void testDoFillZoneItemsWithValidArgumentsAndPreviousValue() throws IOException {
    DescriptorImpl descriptor =
        setUpZoneDescriptor(ImmutableList.of(TEST_ZONE_A, TEST_ZONE_B), null, null);
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
  public void testDoFillZoneItemsEmptyWithValidArgumentsNoZones() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsErrorMessageWithAbortException() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), new AbortException(), null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillZoneItemsErrorMessageWithIOException() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, new IOException());
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_ZoneFillError()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillZoneItems(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoCheckZoneOkWithEmptyZone() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyProjectId() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    FormValidation result = descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, null);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneProjectIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithEmptyCredentialsId() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    FormValidation result = descriptor.doCheckZone(jenkins, TEST_ZONE_A, null, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneCredentialIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithNoAvailableZones() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, null);
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneNotInProject(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithNonMatchingZones() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(TEST_ZONE_B), null, null);
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, OTHER_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneNotInProject(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithAbortException() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), new AbortException(), null);
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneMessageWithIOException() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, new IOException());
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ZoneVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckZoneOkWithAbortExceptionAndEmptyZone() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), new AbortException(), null);
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoCheckZoneOkWithIOExceptionAndEmptyZone() throws IOException {
    DescriptorImpl descriptor = setUpZoneDescriptor(ImmutableList.of(), null, new IOException());
    FormValidation result =
        descriptor.doCheckZone(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoCheckZoneOKWithMatchingZones() throws IOException {
    DescriptorImpl descriptor =
        setUpZoneDescriptor(ImmutableList.of(TEST_ZONE_A, TEST_ZONE_B), null, null);
    FormValidation result =
        descriptor.doCheckZone(jenkins, TEST_ZONE_A, TEST_CREDENTIALS_ID, TEST_PROJECT_ID);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoFillClusterItemsEmptyWithEmptyCredentialsId() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterItems(jenkins, null, null, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterItemsEmptyWithEmptyProjectId() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterItems(jenkins, null, TEST_CREDENTIALS_ID, null, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterItemsErrorMessageWithAbortException() throws IOException {
    DescriptorImpl descriptor =
        setUpClusterDescriptor(ImmutableList.of(), new AbortException(), null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterItemsWithIOException() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, new IOException());
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(Messages.KubernetesEngineBuilder_ClusterFillError()),
            ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterItemsWithInvalidCluster() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(TEST_CLUSTER), null, null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, "wrong", TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_CLUSTER);
  }

  @Test
  public void testDoFillClusterItemsEmptyWithValidInputsNoClusters() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    ListBoxModel expected =
        initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, null, TEST_CREDENTIALS_ID, OTHER_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
  }

  @Test
  public void testDoFillClusterItemsWithValidInputsOneCluster() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(TEST_CLUSTER), null, null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_CLUSTER);
  }

  @Test
  public void testDoFillClusterItemsWithValidInputsMultipleClusters() throws IOException {
    DescriptorImpl descriptor =
        setUpClusterDescriptor(ImmutableList.of(OTHER_CLUSTER, TEST_CLUSTER), null, null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_CLUSTER, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, OTHER_CLUSTER, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, OTHER_CLUSTER);
  }

  @Test
  public void testDoFillClusterItemsWithValidInputsMultipleClustersAndPreviousValue()
      throws IOException {
    DescriptorImpl descriptor =
        setUpClusterDescriptor(ImmutableList.of(OTHER_CLUSTER, TEST_CLUSTER), null, null);
    ListBoxModel expected =
        initExpected(
            ImmutableList.of(EMPTY_NAME, OTHER_CLUSTER, TEST_CLUSTER),
            ImmutableList.of(EMPTY_VALUE, OTHER_CLUSTER, TEST_CLUSTER));
    ListBoxModel result =
        descriptor.doFillClusterItems(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertListBoxModelEquals(expected, result);
    assertValueSelected(result, TEST_CLUSTER);
  }

  @Test
  public void testDoFillClusterItemsWithNonEmptyZone() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(TEST_CLUSTER), null, null);
    {
      ListBoxModel expected =
          initExpected(
              ImmutableList.of(EMPTY_NAME, TEST_CLUSTER),
              ImmutableList.of(EMPTY_VALUE, TEST_CLUSTER));
      ListBoxModel result =
          descriptor.doFillClusterItems(
              jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
      assertNotNull(result);
      assertListBoxModelEquals(expected, result);
      assertValueSelected(result, TEST_CLUSTER);
    }
  }

  @Test
  public void testDoFillClusterItemsWithNonEmptyZoneAndPreviousValue() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    {
      ListBoxModel expected =
          initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
      // The scenario is when the user had previously selected cluster then changed zone
      ListBoxModel result =
          descriptor.doFillClusterItems(
              jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_B);
      assertNotNull(result);
      assertListBoxModelEquals(expected, result);
    }
  }

  @Test
  public void testDoCheckClusterMessageWithEmptyCluster() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    FormValidation result =
        descriptor.doCheckCluster(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithEmptyCredentialsId() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    FormValidation result =
        descriptor.doCheckCluster(jenkins, TEST_CLUSTER, null, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_ClusterCredentialIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithEmptyProjectId() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    FormValidation result =
        descriptor.doCheckCluster(jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, null, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterProjectIDRequired(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithValidInputsNoClusters() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, null);
    FormValidation result =
        descriptor.doCheckCluster(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_NoClusterInProjectZone(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterOkWithValidInputs() throws IOException {
    DescriptorImpl descriptor =
        setUpClusterDescriptor(ImmutableList.of(OTHER_CLUSTER, TEST_CLUSTER), null, null);
    FormValidation result =
        descriptor.doCheckCluster(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoCheckClusterOkWithNonEmptyMatchingZone() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(OTHER_CLUSTER), null, null);
    FormValidation result =
        descriptor.doCheckCluster(
            jenkins, OTHER_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_B);
    assertNotNull(result);
    assertEquals(FormValidation.ok(), result);
  }

  @Test
  public void testDoCHeckClusterMessageWithNonEmptyNonMatchingZone() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(TEST_CLUSTER), null, null);
    FormValidation result =
        descriptor.doCheckCluster(
            jenkins, OTHER_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, TEST_ZONE_A);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterNotInProjectZone(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithAbortException() throws IOException {
    DescriptorImpl descriptor =
        setUpClusterDescriptor(ImmutableList.of(), new AbortException(), null);
    FormValidation result =
        descriptor.doCheckCluster(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithIOException() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, new IOException());
    FormValidation result =
        descriptor.doCheckCluster(
            jenkins, TEST_CLUSTER, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithAbortExceptionAndEmptyCluster() throws IOException {
    DescriptorImpl descriptor =
        setUpClusterDescriptor(ImmutableList.of(), new AbortException(), null);
    FormValidation result =
        descriptor.doCheckCluster(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
  }

  @Test
  public void testDoCheckClusterMessageWithIOExceptionAndEmptyCluster() throws IOException {
    DescriptorImpl descriptor = setUpClusterDescriptor(ImmutableList.of(), null, new IOException());
    FormValidation result =
        descriptor.doCheckCluster(jenkins, null, TEST_CREDENTIALS_ID, TEST_PROJECT_ID, EMPTY_VALUE);
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_ClusterVerificationError(), result.getMessage());
  }

  @Test
  public void testDoCheckVerifyTimeoutInMinutesNAN() {
    DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);
    FormValidation result = descriptor.doCheckVerifyTimeoutInMinutes("abc");
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesFormatError(), result.getMessage());
  }

  @Test
  public void testDoCheckVerifyTimeoutInMinutesZero() {
    DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);
    FormValidation result = descriptor.doCheckVerifyTimeoutInMinutes("0");
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesFormatError(), result.getMessage());
  }

  @Test
  public void testDoCheckVerifyTimeoutInMinutesEmpty() {
    DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);
    FormValidation result = descriptor.doCheckVerifyTimeoutInMinutes("");
    assertNotNull(result);
    assertEquals(
        Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesRequired(), result.getMessage());
  }

  private DescriptorImpl setUpProjectDescriptor(
      List<String> initialProjects,
      String defaultProjectId,
      AbortException abortException,
      IOException ioException)
      throws IOException {
    DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);

    if (abortException != null) {
      Mockito.doThrow(abortException)
          .when(descriptor)
          .getClientFactory(any(Jenkins.class), anyString());
      return descriptor;
    }

    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    Mockito.doReturn(clientFactory)
        .when(descriptor)
        .getClientFactory(any(Jenkins.class), anyString());

    Mockito.when(clientFactory.getDefaultProjectId()).thenReturn(defaultProjectId);
    CloudResourceManagerClient cloudResourceManagerClient =
        Mockito.mock(CloudResourceManagerClient.class);
    Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(cloudResourceManagerClient);

    if (ioException != null) {
      Mockito.when(cloudResourceManagerClient.getAccountProjects()).thenThrow(ioException);
      return descriptor;
    }

    List<Project> projects = new ArrayList<>();
    initialProjects.forEach(p -> projects.add(new Project().setProjectId(p)));
    Mockito.when(cloudResourceManagerClient.getAccountProjects())
        .thenReturn(ImmutableList.copyOf(projects));
    return descriptor;
  }

  private DescriptorImpl setUpZoneDescriptor(
      List<String> initialZones, AbortException abortException, IOException ioException)
      throws IOException {
    DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);

    if (abortException != null) {
      Mockito.doThrow(abortException)
          .when(descriptor)
          .getClientFactory(any(Jenkins.class), anyString());
      return descriptor;
    }

    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    Mockito.doReturn(clientFactory)
        .when(descriptor)
        .getClientFactory(any(Jenkins.class), anyString());

    ComputeClient computeClient = Mockito.mock(ComputeClient.class);
    Mockito.when(clientFactory.computeClient()).thenReturn(computeClient);

    if (ioException != null) {
      Mockito.when(computeClient.getZones(anyString())).thenThrow(ioException);
      return descriptor;
    }

    List<Zone> zones = new ArrayList<>();
    initialZones.forEach(z -> zones.add(new Zone().setName(z)));
    Mockito.when(computeClient.getZones(anyString())).thenReturn(ImmutableList.copyOf(zones));
    return descriptor;
  }

  private DescriptorImpl setUpClusterDescriptor(
      List<String> initialClusters, AbortException abortException, IOException ioException)
      throws IOException {
    DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);

    if (abortException != null) {
      Mockito.doThrow(abortException)
          .when(descriptor)
          .getClientFactory(any(Jenkins.class), anyString());
      return descriptor;
    }

    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    Mockito.doReturn(clientFactory)
        .when(descriptor)
        .getClientFactory(any(Jenkins.class), anyString());

    ContainerClient containerClient = Mockito.mock(ContainerClient.class);
    Mockito.when(clientFactory.containerClient()).thenReturn(containerClient);

    if (ioException != null) {
      Mockito.when(containerClient.listClusters(anyString(), anyString())).thenThrow(ioException);
      return descriptor;
    }

    List<Cluster> clusters = new ArrayList<>();
    initialClusters.forEach(c -> clusters.add(ContainerClient.fromNameAndZone(c)));
    Mockito.when(containerClient.listClusters(anyString(), anyString()))
        .thenReturn(ImmutableList.copyOf(clusters));
    return descriptor;
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
