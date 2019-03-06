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
import hudson.AbortException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudResourceManagerClientTest {
  private static final String TEST_CREDENTIALS_ID = "test-credentials";
  private static final String ERROR_CREDENTIALS_ID = "error-credentials";
  private static final String NULL_CREDENTIALS_ID = "null-credentials";
  private static final List<String> TEST_PROJECT_IDS_SORTED =
      Arrays.asList("test-project-id-a1", "test-project-id-a2", "test-project-id-a3");
  private static final List<String> TEST_PROJECT_IDS_UNSORTED =
      Arrays.asList("test-project-id-b4", "test-project-id-b2", "test-project-id-b5");

  private static List<Project> listOfProjects;
  private static Jenkins jenkins;
  private static ClientFactory clientFactorySpy;

  @BeforeClass
  public static void init() throws IOException {
    listOfProjects = new ArrayList<>();

    CloudResourceManager cloudResourceManager = Mockito.mock(CloudResourceManager.class);
    Projects projects = Mockito.mock(Projects.class);
    Projects.List listProjects = Mockito.mock(Projects.List.class);
    ListProjectsResponse listProjectsResponse = Mockito.mock(ListProjectsResponse.class);
    Mockito.when(cloudResourceManager.projects()).thenReturn(projects);
    Mockito.when(projects.list()).thenReturn(listProjects);
    Mockito.when(listProjects.execute()).thenReturn(listProjectsResponse);
    Mockito.when(listProjectsResponse.getProjects()).thenReturn(listOfProjects);
    CloudResourceManagerClient client = new CloudResourceManagerClient(cloudResourceManager);

    CloudResourceManager errorCloudResourceManager = Mockito.mock(CloudResourceManager.class);
    Projects errorProjects = Mockito.mock(Projects.class);
    Projects.List errorListProjects = Mockito.mock(Projects.List.class);
    Mockito.when(errorCloudResourceManager.projects()).thenReturn(errorProjects);
    Mockito.when(errorProjects.list()).thenReturn(errorListProjects);
    Mockito.when(errorListProjects.execute()).thenThrow(new IOException());
    CloudResourceManagerClient errorClient =
        new CloudResourceManagerClient(errorCloudResourceManager);

    CloudResourceManager nullCloudResourceManager = Mockito.mock(CloudResourceManager.class);
    Projects nullProjects = Mockito.mock(Projects.class);
    Projects.List nullListProjects = Mockito.mock(Projects.List.class);
    ListProjectsResponse nullListProjectsResponse = Mockito.mock(ListProjectsResponse.class);
    Mockito.when(nullCloudResourceManager.projects()).thenReturn(nullProjects);
    Mockito.when(nullProjects.list()).thenReturn(nullListProjects);
    Mockito.when(nullListProjects.execute()).thenReturn(nullListProjectsResponse);
    Mockito.when(nullListProjectsResponse.getProjects()).thenReturn(null);
    CloudResourceManagerClient nullClient = new CloudResourceManagerClient(nullCloudResourceManager);

    // Credentials are passed in at the time of creation.
    jenkins = Mockito.mock(Jenkins.class);
    clientFactorySpy = Mockito.spy(ClientFactory.class);
    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    ClientFactory errorClientFactory = Mockito.mock(ClientFactory.class);
    ClientFactory nullClientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(client);
    Mockito.when(errorClientFactory.cloudResourceManagerClient()).thenReturn(errorClient);
    Mockito.when(nullClientFactory.cloudResourceManagerClient()).thenReturn(nullClient);
    Mockito.doReturn(clientFactory)
        .when(clientFactorySpy)
        .makeClientFactory(jenkins, TEST_CREDENTIALS_ID);
    Mockito.doReturn(errorClientFactory)
        .when(clientFactorySpy)
        .makeClientFactory(jenkins, ERROR_CREDENTIALS_ID);
    Mockito.doReturn(nullClientFactory)
        .when(clientFactorySpy)
        .makeClientFactory(jenkins, NULL_CREDENTIALS_ID);
  }

  @Before
  public void before() {
    listOfProjects.clear();
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
    CloudResourceManagerClient client = getClient(TEST_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(ImmutableList.of(), projects);
  }

  @Test
  public void testGetAccountProjectsSorted() throws IOException {
    fillListOfProjects(TEST_PROJECT_IDS_SORTED, listOfProjects);
    CloudResourceManagerClient client = getClient(TEST_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(initProjectList(TEST_PROJECT_IDS_SORTED), projects);
  }

  @Test
  public void testGetAccountProjectsUnsortedReturnedAsSorted() throws IOException {
    fillListOfProjects(TEST_PROJECT_IDS_UNSORTED, listOfProjects);
    List<Project> expected = initProjectList(TEST_PROJECT_IDS_UNSORTED);
    expected.sort(Comparator.comparing(Project::getProjectId));
    CloudResourceManagerClient client = getClient(TEST_CREDENTIALS_ID);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(expected, projects);
  }

  private static CloudResourceManagerClient getClient(String credentialsId) throws AbortException {
    ClientFactory clientFactory = clientFactorySpy.makeClientFactory(jenkins, credentialsId);
    return clientFactory.cloudResourceManagerClient();
  }

  private static void fillListOfProjects(List<String> projectIds, List<Project> projects) {
    projectIds.forEach(id -> projects.add(new Project().setProjectId(id)));
  }

  private static List<Project> initProjectList(List<String> projectIds) {
    List<Project> projects = new ArrayList<>();
    fillListOfProjects(projectIds, projects);
    return projects;
  }
}
