/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jenkins.plugins.k8sengine;

import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.EMPTY_NAME;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.EMPTY_VALUE;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.OTHER_PROJECT_ID;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.TEST_CREDENTIALS_ID;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.TEST_PROJECT_ID;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.TEST_ZONE_A;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.TEST_ZONE_B;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.assertListBoxModelEquals;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.assertValueSelected;
import static com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilderTest.initExpected;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.google.api.services.container.model.Cluster;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.DescriptorImpl;
import com.google.jenkins.plugins.k8sengine.client.ClientFactory;
import com.google.jenkins.plugins.k8sengine.client.ContainerClient;
import hudson.AbortException;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class KubernetesEngineBuilderClusterTest {
  private static final String TEST_CLUSTER = "testCluster (us-west1-a)";
  private static final String OTHER_CLUSTER = "otherCluster (us-east1-b)";

  private static Jenkins jenkins;

  @BeforeClass
  public static void init() {
    jenkins = Mockito.mock(Jenkins.class);
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
}
