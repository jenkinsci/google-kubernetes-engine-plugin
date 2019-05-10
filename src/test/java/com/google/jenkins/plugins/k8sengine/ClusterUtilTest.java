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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Tests for verifying the behavior of {@link ClusterUtil methods} */
public class ClusterUtilTest {
  @Test(expected = NullPointerException.class)
  public void testToNameAndZoneNullCluster() {
    ClusterUtil.toNameAndZone(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndZoneNullName() {
    ClusterUtil.toNameAndZone(null, "us-west1-a");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndZoneEmptyName() {
    ClusterUtil.toNameAndZone("", "us-west1-a");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndZoneNullZone() {
    ClusterUtil.toNameAndZone("test-cluster", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndZoneEmptyZone() {
    ClusterUtil.toNameAndZone("test-cluster", "");
  }

  @Test
  public void testToNameAndZoneValidInputs() {
    assertEquals(
        "test-cluster (us-west1-a)", ClusterUtil.toNameAndZone("test-cluster", "us-west1-a"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuesFromNameAndZoneNullInput() {
    ClusterUtil.valuesFromNameAndZone(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuesFromNameAndZoneEmptyInput() {
    ClusterUtil.valuesFromNameAndZone("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuesFromNameAndZoneMalformedInput() {
    ClusterUtil.valuesFromNameAndZone("test us-west1-a");
  }

  @Test
  public void testValuesFromNameAndZoneValidInput() {
    ClusterUtil.valuesFromNameAndZone("test-cluster (us-west1-a)");
  }
}
