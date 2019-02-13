package com.google.jenkins.plugins.k8sengine.client;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client for communicating with the Google Cloud Research Manager API.
 *
 * @see <a href="https://cloud.google.com/resource-manager/reference/rest/">Cloud Research Manager
 *     API</a>
 */
public class CloudResourceManagerClient {
  private final CloudResourceManager cloudResourceManager;

  /**
   * Constructs a new {@link CloudResourceManagerClient} instance.
   *
   * @param cloudResourceManager The {@link CloudResourceManager} instance this class will utilize
   *     for interacting with the Cloud Resource Manager API.
   */
  public CloudResourceManagerClient(CloudResourceManager cloudResourceManager) {
    this.cloudResourceManager = Preconditions.checkNotNull(cloudResourceManager);
  }

  /**
   * Retrieves a list of Projects for the credentials associated with this client.
   *
   * @return The retrieved list of projects
   * @throws IOException When an error occurred attempting to get the projects.
   */
  public List<Project> getAccountProjects() throws IOException {
    List<Project> projects = cloudResourceManager.projects().list().execute().getProjects();
    if (projects == null) {
      projects = new ArrayList<>();
    }

    // Sort by project ID
    projects.sort(Comparator.comparing(Project::getProjectId));

    return projects;
  }
}
