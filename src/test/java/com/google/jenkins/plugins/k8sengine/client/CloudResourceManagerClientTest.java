package com.google.jenkins.plugins.k8sengine.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Projects;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import hudson.AbortException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    // Credentials are passed in at the time of creation.
    jenkins = Mockito.mock(Jenkins.class);
    clientFactorySpy = Mockito.spy(ClientFactory.class);
    ClientFactory clientFactory = Mockito.mock(ClientFactory.class);
    ClientFactory errorClientFactory = Mockito.mock(ClientFactory.class);
    Mockito.when(clientFactory.cloudResourceManagerClient()).thenReturn(client);
    Mockito.when(errorClientFactory.cloudResourceManagerClient()).thenReturn(errorClient);
    Mockito.doReturn(clientFactory)
        .when(clientFactorySpy)
        .makeClientFactory(jenkins, TEST_CREDENTIALS_ID);
    Mockito.doReturn(errorClientFactory)
        .when(clientFactorySpy)
        .makeClientFactory(jenkins, ERROR_CREDENTIALS_ID);
  }

  @Before
  public void before() {
    listOfProjects.clear();
  }

  @Test(expected = IOException.class)
  public void testGetAccountProjectsErrorWithInvalidCredentials() throws IOException {
    testGetProjects(ERROR_CREDENTIALS_ID, Collections.emptyList());
  }

  @Test
  public void testGetAccountProjectsEmptyReturnsEmpty() throws IOException {
    testGetProjects(TEST_CREDENTIALS_ID, Collections.emptyList());
  }

  @Test
  public void testGetAccountProjectsSorted() throws IOException {
    fillListOfProjects(TEST_PROJECT_IDS_SORTED);
    testGetProjects(TEST_CREDENTIALS_ID, TEST_PROJECT_IDS_SORTED);
  }

  @Test
  public void testGetAccountProjectsUnsortedReturnedAsSorted() throws IOException {
    fillListOfProjects(TEST_PROJECT_IDS_UNSORTED);
    testGetProjects(TEST_CREDENTIALS_ID, TEST_PROJECT_IDS_UNSORTED);
  }

  private static CloudResourceManagerClient getClient(String credentialsId) throws AbortException {
    ClientFactory clientFactory = clientFactorySpy.makeClientFactory(jenkins, credentialsId);
    return clientFactory.cloudResourceManagerClient();
  }

  private static void fillListOfProjects(List<String> projectIds) {
    projectIds.forEach(id -> listOfProjects.add(new Project().setProjectId(id)));
  }

  private static void testGetProjects(String credentialsId, List<String> projectIds)
      throws IOException {
    CloudResourceManagerClient client = getClient(credentialsId);
    List<String> expectedProjectIds = new ArrayList<>(projectIds);
    List<Project> projects = client.getAccountProjects();
    assertNotNull("projects was null.", projects);
    assertEquals(
        String.format(
            "Excepted %d projects but %d projects retrieved.",
            expectedProjectIds.size(), projects.size()),
        expectedProjectIds.size(),
        projects.size());

    if (expectedProjectIds.size() > 0) {
      expectedProjectIds.sort(String::compareTo);
      for (int i = 0; i < expectedProjectIds.size(); i++) {
        assertEquals(
            String.format(
                "Projects not sorted. Expected %s but was %s.",
                expectedProjectIds.get(i), projects.get(i).getName()),
            expectedProjectIds.get(i),
            projects.get(i).getProjectId());
      }
    }
  }
}
