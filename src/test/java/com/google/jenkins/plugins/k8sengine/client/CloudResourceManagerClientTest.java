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

package com.google.jenkins.plugins.k8sengine.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudResourceManagerClientTest {
  private static final List<String> TEST_PROJECT_IDS_SORTED =
      Arrays.asList("test-project-id-a1", "test-project-id-a2", "test-project-id-a3");
  private static final List<String> TEST_PROJECT_IDS_UNSORTED =
      Arrays.asList("test-project-id-b4", "test-project-id-b2", "test-project-id-b5");

  @Test(expected = IOException.class)
  public void testGetAccountProjectsErrorWithInvalidCredentials() throws IOException {
    CloudResourceManagerClient client = setUpClient(null, new IOException());
    client.getAccountProjects();
  }

  @Test
  public void testGetAccountProjectsNullReturnsEmpty() throws IOException {
    CloudResourceManagerClient client = setUpClient(null, null);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(ImmutableList.of(), projects);
  }

  @Test
  public void testGetAccountProjectsEmptyReturnsEmpty() throws IOException {
    CloudResourceManagerClient client = setUpClient(ImmutableList.of(), null);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(ImmutableList.of(), projects);
  }

  @Test
  public void testGetAccountProjectsSorted() throws IOException {
    CloudResourceManagerClient client = setUpClient(TEST_PROJECT_IDS_SORTED, null);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(initProjectList(TEST_PROJECT_IDS_SORTED), projects);
  }

  @Test
  public void testGetAccountProjectsUnsortedReturnedAsSorted() throws IOException {
    List<Project> expected = initProjectList(TEST_PROJECT_IDS_UNSORTED);
    expected.sort(Comparator.comparing(Project::getProjectId));
    CloudResourceManagerClient client = setUpClient(TEST_PROJECT_IDS_UNSORTED, null);
    List<Project> projects = client.getAccountProjects();
    assertNotNull(projects);
    assertEquals(expected, projects);
  }

  private static CloudResourceManagerClient setUpClient(
      List<String> initial, IOException ioException) throws IOException {
    CloudResourceManager cloudResourceManager = Mockito.mock(CloudResourceManager.class);
    Projects projects = Mockito.mock(Projects.class);
    Mockito.when(cloudResourceManager.projects()).thenReturn(projects);
    Projects.List projectsListCall = Mockito.mock(Projects.List.class);
    Mockito.when(projects.list()).thenReturn(projectsListCall);

    if (ioException != null) {
      Mockito.when(projectsListCall.execute()).thenThrow(ioException);
    } else if (initial == null) {
      Mockito.when(projectsListCall.execute())
          .thenReturn(new ListProjectsResponse().setProjects(null));
    } else {
      List<Project> projectList = initProjectList(initial);
      Mockito.when(projectsListCall.execute())
          .thenReturn(new ListProjectsResponse().setProjects(projectList));
    }

    return new CloudResourceManagerClient(cloudResourceManager);
  }

  private static List<Project> initProjectList(List<String> projectIds) {
    List<Project> projects = new ArrayList<>();
    projectIds.forEach(id -> projects.add(new Project().setProjectId(id)));
    return projects;
  }
}
