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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/** Utility functions for converting between {@link Cluster}s and their String representations. */
class ClusterUtil {
  /**
   * Given a GKE {@link Cluster} return a String representation containing the name and zone.
   *
   * @param cluster The {@link Cluster} object to extract values from.
   * @return A String of the form "name (zone)" based on the given cluster's properties.
   */
  static String toNameAndZone(Cluster cluster) {
    Preconditions.checkNotNull(cluster);
    return toNameAndZone(cluster.getName(), cluster.getZone());
  }

  /**
   * Given a name and zone for a cluster, return the combined String representation.
   *
   * @param name A non-empty cluster name
   * @param zone A non-empty GCP resource zone.
   * @return A String of the form "name (zone)".
   */
  static String toNameAndZone(String name, String zone) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(zone));
    return String.format("%s (%s)", name, zone);
  }

  /**
   * Only used for mocking the {@link com.google.jenkins.plugins.k8sengine.client.ContainerClient}.
   * Constructs a {@link Cluster} from the given nameAndZone value.
   *
   * @param nameAndZone A non-empty String of the form "name (zone)"
   * @return A cluster with the name and zone properties from the provided nameAndZone.
   */
  @VisibleForTesting
  static Cluster fromNameAndZone(String nameAndZone) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(nameAndZone));
    String[] values = valuesFromNameAndZone(nameAndZone);
    return new Cluster().setName(values[0]).setZone(values[1]);
  }

  /**
   * Extracts the individual values from a combined nameAndZone String.
   *
   * @param nameAndZone A non-empty String of the form "name (zone)"
   * @return The String array {name, zone} from the provided value.
   */
  static String[] valuesFromNameAndZone(String nameAndZone) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(nameAndZone));
    String[] clusters = nameAndZone.split(" [(]");
    if (clusters.length != 2) {
      throw new IllegalArgumentException("nameAndZone should be of the form 'name (zone)'");
    }
    clusters[1] = clusters[1].substring(0, clusters[1].length() - 1);
    return clusters;
  }
}
