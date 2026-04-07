/*
 * Copyright 2019 Google LLC
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

package com.google.jenkins.plugins.k8sengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.MasterAuth;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Tests {@link KubeConfig}. */
@ExtendWith(MockitoExtension.class)
class KubeConfigTest {

    @Test
    void testContextStringReturnsProperly() {
        String result = KubeConfig.contextString("testProject", "us-central1-c", "testCluster");
        assertNotNull(result);
        assertEquals("gke_testProject_us-central1-c_testCluster", result);
    }

    @Test
    void testClusterServerReturnsProperly() {
        Cluster cluster = Mockito.mock(Cluster.class);
        Mockito.when(cluster.getEndpoint()).thenReturn("testEndpoint");
        String result = KubeConfig.clusterServer(cluster);
        assertNotNull(result);
        assertEquals("https://testEndpoint", result);
    }

    @Test
    void testFromClusterReturnsProperly() {
        Cluster cluster = Mockito.mock(Cluster.class);
        Mockito.when(cluster.getEndpoint()).thenReturn("testEndpoint");
        Mockito.when(cluster.getLocation()).thenReturn("us-central1-c");
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        MasterAuth auth = Mockito.mock(MasterAuth.class);
        Mockito.when(cluster.getMasterAuth()).thenReturn(auth);
        Mockito.when(auth.getClusterCaCertificate()).thenReturn("testCaCert");

        KubeConfig result = KubeConfig.fromCluster("testProject", cluster, "testAccessToken");
        assertNotNull(result);

        String currentContext = result.getCurrentContext();
        assertNotNull(currentContext);
        assertEquals(currentContext, KubeConfig.contextString("testProject", "us-central1-c", "testCluster"));
        assertNotNull(result.getUsers());
        assertEquals(1, result.getUsers().size());
        assertNotNull(result.getContexts());
        assertEquals(1, result.getContexts().size());
        assertNotNull(result.getClusters());
        assertEquals(1, result.getClusters().size());
        // NOTE: The verification of the contents happens in the toYaml test
    }

    @Test
    void testToYamlReturnsProperly() throws Exception {
        Cluster cluster = Mockito.mock(Cluster.class);
        Mockito.when(cluster.getEndpoint()).thenReturn("testEndpoint");
        Mockito.when(cluster.getLocation()).thenReturn("us-central1-c");
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        MasterAuth auth = Mockito.mock(MasterAuth.class);
        Mockito.when(cluster.getMasterAuth()).thenReturn(auth);
        Mockito.when(auth.getClusterCaCertificate()).thenReturn("testCaCert");
        KubeConfig config = KubeConfig.fromCluster("testProject", cluster, "testAccessToken");
        String result = config.toYaml();

        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        BufferedReader reader = Files.newBufferedReader(Paths.get(
                KubeConfigTest.class.getResource("/expectedKubeConfig.yml").toURI()));
        reader.lines().forEach(printWriter::println);
        printWriter.flush();
        String expected = writer.toString();
        assertTrue(yamlEquals(expected, result));
    }

    private static boolean yamlEquals(String expectedYaml, String testYaml) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> testConfig = yaml.load(new BufferedReader(new StringReader(testYaml)));
        Map<String, Object> expectedConfig = yaml.load(new BufferedReader(new StringReader(expectedYaml)));

        TriFunction<TriFunction, Object, Object, Boolean> deepCollectionEquals = (f, expected, test) -> {
            if (!expected.getClass().equals(test.getClass())) {
                return false;
            }

            if (expected instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> expectedMap = (Map<String, Object>) expected;
                @SuppressWarnings("unchecked")
                Map<String, Object> testMap = (Map<String, Object>) test;
                for (String key : expectedMap.keySet()) {

                    @SuppressWarnings("unchecked")
                    Object result = f.apply(f, expectedMap.get(key), testMap.get(key));
                    if (!(Boolean) result) {
                        return false;
                    }
                }
            }

            if (expected instanceof List) {
                Iterator expectedItr = ((List) expected).listIterator();
                Iterator testItr = ((List) test).listIterator();
                while (expectedItr.hasNext()) {
                    if (!testItr.hasNext()) {
                        return false;
                    }

                    @SuppressWarnings("unchecked")
                    Object result = f.apply(f, expectedItr.next(), testItr.next());
                    if (!(Boolean) result) {
                        return false;
                    }
                }

                if (testItr.hasNext()) {
                    return false;
                }
            }

            return expected.equals(test);
        };

        return deepCollectionEquals.apply(deepCollectionEquals, expectedConfig, testConfig);
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {

        R apply(A a, B b, C c);

        default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }
    }
}
