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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.graphite.platforms.plugin.client.ClientFactory;
import com.google.cloud.graphite.platforms.plugin.client.CloudResourceManagerClient;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.DescriptorImpl;
import hudson.AbortException;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KubernetesEngineBuilderTest {

    static final String TEST_PROJECT_ID = "test-project-id";
    static final String OTHER_PROJECT_ID = "other-project-id";
    static final String TEST_CREDENTIALS_ID = "test-credentials-id";

    private static Jenkins jenkins;

    // TODO(#49): Separate out tests into separate classes for better organization.

    @BeforeAll
    static void init() {
        jenkins = Mockito.mock(Jenkins.class);
    }

    @Test
    void testDoFillProjectIdItemsErrorMessageWithAbortException() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", new AbortException(), null);
        ListBoxModel expected = initExpected(
                ImmutableList.of(Messages.KubernetesEngineBuilder_CredentialAuthFailed()),
                ImmutableList.of(EMPTY_VALUE));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertListBoxModelEquals(expected, result);
    }

    @Test
    void testDoFillProjectIdItemsErrorMessageWithIOException() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, new IOException());
        ListBoxModel expected = initExpected(
                ImmutableList.of(Messages.KubernetesEngineBuilder_ProjectIDFillError()), ImmutableList.of(EMPTY_VALUE));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertListBoxModelEquals(expected, result);
    }

    @Test
    void testDoFillProjectIdItemsEmptyWithEmptyCredentialsId() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
        ListBoxModel expected = initExpected(ImmutableList.of(EMPTY_NAME), ImmutableList.of(EMPTY_VALUE));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, null);
        assertNotNull(result);
        assertListBoxModelEquals(expected, result);
    }

    @Test
    void testDoFillProjectIdItemsWithValidCredentialsId() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(
                ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), TEST_PROJECT_ID, null, null);

        ListBoxModel expected = initExpected(
                ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
                ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertListBoxModelEquals(expected, result);
        assertValueSelected(result, TEST_PROJECT_ID);
    }

    @Test
    void testDoFillProjectIdItemsWithValidCredentialsIdAndPreviousValueAndDefault() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(
                ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), TEST_PROJECT_ID, null, null);
        ListBoxModel expected = initExpected(
                ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
                ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, OTHER_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertListBoxModelEquals(expected, result);
        assertValueSelected(result, OTHER_PROJECT_ID);
    }

    @Test
    void testDoFillProjectIdItemsWithValidCredentialsIdAndPreviousValueAndEmptyDefault() throws IOException {
        DescriptorImpl descriptor =
                setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), "", null, null);
        ListBoxModel expected = initExpected(
                ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
                ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, OTHER_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertListBoxModelEquals(expected, result);
        assertValueSelected(result, OTHER_PROJECT_ID);
    }

    @Test
    void testDoFillProjectIdItemsWithValidCredentialsIdMissingDefaultProject() throws IOException {
        DescriptorImpl descriptor =
                setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID), TEST_PROJECT_ID, null, null);
        ListBoxModel expected = initExpected(
                ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
                ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, null, TEST_CREDENTIALS_ID);
        assertListBoxModelEquals(expected, result);
        assertValueSelected(result, OTHER_PROJECT_ID);
    }

    @Test
    void testDoFillProjectIdItemsWithValidCredentialsAndEmptyProject() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(
                ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), TEST_PROJECT_ID, null, null);
        ListBoxModel expected = initExpected(
                ImmutableList.of(EMPTY_NAME, OTHER_PROJECT_ID, TEST_PROJECT_ID),
                ImmutableList.of(EMPTY_VALUE, OTHER_PROJECT_ID, TEST_PROJECT_ID));
        ListBoxModel result = descriptor.doFillProjectIdItems(jenkins, OTHER_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertListBoxModelEquals(expected, result);
        assertValueSelected(result, OTHER_PROJECT_ID);
    }

    @Test
    void testDoCheckProjectIdMessageWithEmptyProjectID() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_ProjectIDRequired(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithEmptyCredentialsID() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, null);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_ProjectCredentialIDRequired(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithAbortException() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", new AbortException(), null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithIOException() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, new IOException());
        FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_ProjectIDVerificationError(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithAbortExceptionAndEmptyProjectId() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", new AbortException(), null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_CredentialAuthFailed(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithIOExceptionAndEmptyProjectId() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, new IOException());
        FormValidation result = descriptor.doCheckProjectId(jenkins, null, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_ProjectIDVerificationError(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithNoProjects() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(), "", null, null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithWrongProjects() throws IOException {
        DescriptorImpl descriptor = setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID), "", null, null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_ProjectIDNotUnderCredential(), result.getMessage());
    }

    @Test
    void testDoCheckProjectIdMessageWithValidProject() throws IOException {
        DescriptorImpl descriptor =
                setUpProjectDescriptor(ImmutableList.of(OTHER_PROJECT_ID, TEST_PROJECT_ID), "", null, null);
        FormValidation result = descriptor.doCheckProjectId(jenkins, TEST_PROJECT_ID, TEST_CREDENTIALS_ID);
        assertNotNull(result);
        assertEquals(FormValidation.ok().getMessage(), result.getMessage());
    }

    @Test
    void testDoCheckVerifyTimeoutInMinutesNAN() {
        DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);
        FormValidation result = descriptor.doCheckVerifyTimeoutInMinutes("abc");
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesFormatError(), result.getMessage());
    }

    @Test
    void testDoCheckVerifyTimeoutInMinutesZero() {
        DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);
        FormValidation result = descriptor.doCheckVerifyTimeoutInMinutes("0");
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesFormatError(), result.getMessage());
    }

    @Test
    void testDoCheckVerifyTimeoutInMinutesEmpty() {
        DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);
        FormValidation result = descriptor.doCheckVerifyTimeoutInMinutes("");
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_VerifyTimeoutInMinutesRequired(), result.getMessage());
    }

    private DescriptorImpl setUpProjectDescriptor(
            List<String> initialProjects,
            String defaultProjectId,
            AbortException abortException,
            IOException ioException)
            throws IOException {
        DescriptorImpl descriptor = Mockito.spy(DescriptorImpl.class);

        if (abortException != null) {
            Mockito.doThrow(abortException).when(descriptor).getClientFactory(any(Jenkins.class), anyString());
            return descriptor;
        }

        ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
        Mockito.doReturn(clientFactory).when(descriptor).getClientFactory(any(Jenkins.class), anyString());

        Mockito.doReturn(defaultProjectId).when(descriptor).getDefaultProjectId(any(Jenkins.class), anyString());

        CloudResourceManagerClient cloudResourceManagerClient = Mockito.mock(CloudResourceManagerClient.class);
        Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(cloudResourceManagerClient);

        if (ioException != null) {
            Mockito.when(cloudResourceManagerClient.listProjects()).thenThrow(ioException);
            return descriptor;
        }

        List<Project> projects = new ArrayList<>();
        initialProjects.forEach(p -> projects.add(new Project().setProjectId(p)));
        Mockito.when(cloudResourceManagerClient.listProjects()).thenReturn(ImmutableList.copyOf(projects));
        return descriptor;
    }

    static ListBoxModel initExpected(List<String> expectedNames, List<String> expectedValues) {
        ListBoxModel expected = new ListBoxModel();
        for (int i = 0; i < expectedNames.size(); i++) {
            expected.add(expectedNames.get(i), expectedValues.get(i));
        }
        return expected;
    }

    static void assertListBoxModelEquals(ListBoxModel expected, ListBoxModel result) {
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).name, result.get(i).name);
            assertEquals(expected.get(i).value, result.get(i).value);
        }
    }

    static void assertValueSelected(ListBoxModel result, String expectedValue) {
        Optional<Option> expectedOption =
                result.stream().filter(i -> expectedValue.equals(i.value)).findFirst();
        expectedOption.ifPresent(option -> assertTrue(option.selected));
    }
}
