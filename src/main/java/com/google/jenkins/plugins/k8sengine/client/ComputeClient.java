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

package com.google.jenkins.plugins.k8sengine.client;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Zone;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client for communicating with the Google Compute Engine API
 *
 * @see <a href="https://cloud.google.com/compute/docs/reference/rest/">Compute Engine API</a>
 */
public class ComputeClient {
  private final Compute compute;

  /**
   * Constructs a new {@link ComputeClient} instance.
   *
   * @param compute The {@link Compute} instance this class will utilize for interacting with the
   *     GCE API.
   */
  public ComputeClient(Compute compute) {
    this.compute = Preconditions.checkNotNull(compute);
  }

  /**
   * Retrieves a list of {@link Zone} objects from the container client.
   *
   * @param projectId The ID of the project that zone usage is needed for.
   * @return The retrieved list of {@link Zone} objects.
   * @throws IOException When an error occurred attempting to get the list of zones.
   */
  public List<Zone> getZones(String projectId) throws IOException {
    Preconditions.checkNotNull(projectId);
    List<Zone> zones = compute.zones().list(projectId).execute().getItems();
    if (zones == null) {
      zones = new ArrayList<>();
    }

    // Sort by name
    zones.sort(Comparator.comparing(Zone::getName));

    return zones;
  }
}
