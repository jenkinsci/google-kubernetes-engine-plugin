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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests for verifying the behavior of {@link ClusterUtil methods} */
class ClusterUtilTest {

    @Test
    void testToNameAndLocationNullCluster() {
        assertThrows(NullPointerException.class, () -> ClusterUtil.toNameAndLocation(null));
    }

    @Test
    void testToNameAndLocationNullName() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.toNameAndLocation(null, "us-west1-a"));
    }

    @Test
    void testToNameAndLocationEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.toNameAndLocation("", "us-west1-a"));
    }

    @Test
    void testToNameAndLocationNullLocation() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.toNameAndLocation("test-cluster", null));
    }

    @Test
    void testToNameAndLocationEmptyLocation() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.toNameAndLocation("test-cluster", ""));
    }

    @Test
    void testToNameAndLocationValidInputs() {
        assertEquals("test-cluster (us-west1-a)", ClusterUtil.toNameAndLocation("test-cluster", "us-west1-a"));
    }

    @Test
    void testValuesFromNameAndLocationNullInput() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.valuesFromNameAndLocation(null));
    }

    @Test
    void testValuesFromNameAndLocationEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.valuesFromNameAndLocation(""));
    }

    @Test
    void testValuesFromNameAndLocationMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> ClusterUtil.valuesFromNameAndLocation("test us-west1-a"));
    }

    @Test
    void testValuesFromNameAndLocationValidInput() {
        ClusterUtil.valuesFromNameAndLocation("test-cluster (us-west1-a)");
    }
}
