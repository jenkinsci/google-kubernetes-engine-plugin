/*
 * Copyright 2019 Google Inc. All Rights Reserved.
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

package com.google.jenkins.plugins.k8sengine.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hudson.AbortException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class CloudResourceManagerClientTest {
  private static final String SORTED_CREDENTIALS_ID = "sorted-credentials";
  private static final String UNSORTED_CREDENTIALS_ID = "unsorted-credentials";
  private static final String EMPTY_CREDENTIALS_ID = "empty-credentials";
  private static final String ERROR_CREDENTIALS_ID = "error-credentials";
  private static final String NULL_CREDENTIALS_ID = "null-credentials";
  private static final List<String> TEST_PROJECT_IDS_SORTED =
      Arrays.asList("test-project-id-a1", "test-project-id-a2", "test-project-id-a3");
  private static final List<String> TEST_PROJECT_IDS_UNSORTED =
      Arrays.asList("test-project-id-b4", "test-project-id-b2", "test-project-id-b5");

  private static Jenkins jenkins;
  private static ClientFactory clientFactorySpy;

  @BeforeClass
  public static void init() throws IOException {
    jenkins = Mockito.mock(Jenkins.class);
    clientFactorySpy = Mockito.spy(ClientFactory.class);
    Mockito.doAnswer(
            mockClientFactoryAnswer(
                new ImmutableMap.Builder<String, List<Project>>()
                    .put(SORTED_CREDENTIALS_ID, initProjectList(TEST_PROJECT_IDS_SORTED))
                    .put(UNSORTED_CREDENTIALS_ID, initProjectList(TEST_PROJECT_IDS_UNSORTED))
                    .put(EMPTY_CREDENTIALS_ID, ImmutableList.of())
                    .build()))
        .when(clientFactorySpy)
        .makeClientFactory(ArgumentMatchers.any(), ArgumentMatchers.anyString());
  }

  @Test(expected = IOException.class)
  public void testGetAccountProjectsErrorWithInvalidCredentials() throws IOException {
    CloudResourceManagerClient client = getClient(ERROR_CREDENTIALS_ID);
    client.getAccountProjects();
  }

  @Test
  public void testGetAccountProjectsNullReturnsEmpty() throws IOException {
    CloudResourceManagerClient client = getClient(NULL_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(ImmutableList.of(), projects);
  }

  @Test
  public void testGetAccountProjectsEmptyReturnsEmpty() throws IOException {
    CloudResourceManagerClient client = getClient(EMPTY_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(ImmutableList.of(), projects);
  }

  @Test
  public void testGetAccountProjectsSorted() throws IOException {
    CloudResourceManagerClient client = getClient(SORTED_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(initProjectList(TEST_PROJECT_IDS_SORTED), projects);
  }

  @Test
  public void testGetAccountProjectsUnsortedReturnedAsSorted() throws IOException {
    List<Project> expected = initProjectList(TEST_PROJECT_IDS_UNSORTED);
    expected.sort(Comparator.comparing(Project::getProjectId));
    CloudResourceManagerClient client = getClient(UNSORTED_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(expected, projects);
  }

  // Credentials are passed in when creating the ClientFactory, which makes this step necessary.
  private static CloudResourceManagerClient getClient(String credentialsId) throws AbortException {
    ClientFactory clientFactory = clientFactorySpy.makeClientFactory(jenkins, credentialsId);
    return clientFactory.cloudResourceManagerClient();
  }

  private static List<Project> initProjectList(List<String> projectIds) {
    List<Project> projects = new ArrayList<>();
    projectIds.forEach(id -> projects.add(new Project().setProjectId(id)));
    return projects;
  }

  private static Answer<ClientFactory> mockClientFactoryAnswer(
      ImmutableMap<String, List<Project>> credentialsIdToProjects) {
    return invocation -> {
      // Argument 0 is the Jenkins context.
      String credentialsId = invocation.getArgument(1);

      Projects.List projectsListCall = Mockito.mock(Projects.List.class);
      if (credentialsIdToProjects.containsKey(credentialsId)) {
        List<Project> projectsList = new ArrayList<>(credentialsIdToProjects.get(credentialsId));
        Mockito.when(projectsListCall.execute())
            .thenReturn(new ListProjectsResponse().setProjects(projectsList));
      } else if (ERROR_CREDENTIALS_ID.equals(credentialsId)) {
        Mockito.when(projectsListCall.execute()).thenThrow(new IOException());
      } else {
        Mockito.when(projectsListCall.execute())
            .thenReturn(new ListProjectsResponse().setProjects(null));
      }

      CloudResourceManager cloudResourceManager = Mockito.mock(CloudResourceManager.class);
      Projects projects = Mockito.mock(Projects.class);
      Mockito.when(cloudResourceManager.projects()).thenReturn(projects);
      Mockito.when(projects.list()).thenReturn(projectsListCall);
      CloudResourceManagerClient client = new CloudResourceManagerClient(cloudResourceManager);
      ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
      Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(client);
      return clientFactory;
    };
  }
}
