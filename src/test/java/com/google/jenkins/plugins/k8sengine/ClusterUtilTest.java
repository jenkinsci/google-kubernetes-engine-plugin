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
  public void testToNameAndLocationNullCluster() {
    ClusterUtil.toNameAndLocation(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndLocationNullName() {
    ClusterUtil.toNameAndLocation(null, "us-west1-a");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndLocationEmptyName() {
    ClusterUtil.toNameAndLocation("", "us-west1-a");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndLocationNullLocation() {
    ClusterUtil.toNameAndLocation("test-cluster", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToNameAndLocationEmptyLocation() {
    ClusterUtil.toNameAndLocation("test-cluster", "");
  }

  @Test
  public void testToNameAndLocationValidInputs() {
    assertEquals(
        "test-cluster (us-west1-a)", ClusterUtil.toNameAndLocation("test-cluster", "us-west1-a"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuesFromNameAndLocationNullInput() {
    ClusterUtil.valuesFromNameAndLocation(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuesFromNameAndLocationEmptyInput() {
    ClusterUtil.valuesFromNameAndLocation("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuesFromNameAndLocationMalformedInput() {
    ClusterUtil.valuesFromNameAndLocation("test us-west1-a");
  }

  @Test
  public void testValuesFromNameAndLocationValidInput() {
    ClusterUtil.valuesFromNameAndLocation("test-cluster (us-west1-a)");
  }
}
