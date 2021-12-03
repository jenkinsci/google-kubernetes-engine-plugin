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

import com.google.api.services.container.model.Cluster;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;

/** Utility functions for converting between {@link Cluster}s and their String representations. */
class ClusterUtil {
  /**
   * Given a GKE {@link Cluster} return a String representation containing the name and location.
   *
   * @param cluster The {@link Cluster} object to extract values from.
   * @return A String of the form "name (location)" based on the given cluster's properties.
   */
  static String toNameAndLocation(Cluster cluster) {
    Objects.requireNonNull(cluster);
    return toNameAndLocation(cluster.getName(), cluster.getLocation());
  }

  /**
   * Given a name and location for a cluster, return the combined String representation.
   *
   * @param name A non-empty cluster name
   * @param location A non-empty GCP resource location.
   * @return A String of the form "name (location)".
   */
  static String toNameAndLocation(String name, String location) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("name cannot be null");
    }
    if (StringUtils.isBlank(location)) {
      throw new IllegalArgumentException("location cannot be null");
    }
    return String.format("%s (%s)", name, location);
  }

  /**
   * Only used for mocking the {@link
   * com.google.cloud.graphite.platforms.plugin.client.ContainerClient}. Constructs a {@link
   * Cluster} from the given nameAndLocation value.
   *
   * @param nameAndLocation A non-empty String of the form "name (location)"
   * @return A cluster with the name and location properties from the provided nameAndLocation.
   */
  static Cluster fromNameAndLocation(String nameAndLocation) {
    if (StringUtils.isBlank(nameAndLocation)) {
      throw new IllegalArgumentException("nameAndLocation cannot be null");
    }
    String[] values = valuesFromNameAndLocation(nameAndLocation);
    return new Cluster().setName(values[0]).setLocation(values[1]);
  }

  /**
   * Extracts the individual values from a combined nameAndLocation String.
   *
   * @param nameAndLocation A non-empty String of the form "name (location)"
   * @return The String array {name, location} from the provided value.
   */
  static String[] valuesFromNameAndLocation(String nameAndLocation) {
    if (StringUtils.isBlank(nameAndLocation)) {
      throw new IllegalArgumentException("nameAndLocation cannot be null");
    }
    String[] clusters = nameAndLocation.split(" [(]");
    if (clusters.length != 2) {
      throw new IllegalArgumentException("nameAndLocation should be of the form 'name (location)'");
    }
    clusters[1] = clusters[1].substring(0, clusters[1].length() - 1);
    return clusters;
  }
}
