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

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
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
  public ImmutableList<Project> getAccountProjects() throws IOException {
    List<Project> projects = cloudResourceManager.projects().list().execute().getProjects();
    if (projects == null) {
      projects = ImmutableList.of();
    }

    // Sort by project ID
    projects.sort(Comparator.comparing(Project::getProjectId));

    return ImmutableList.copyOf(projects);
  }
}
