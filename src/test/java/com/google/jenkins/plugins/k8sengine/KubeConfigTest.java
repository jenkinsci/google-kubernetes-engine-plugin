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

package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.api.services.container.model.Cluster;
import com.google.api.services.container.model.MasterAuth;
import java.io.BufferedReader;
import java.io.IOException;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Tests {@link KubeConfig}. */
@RunWith(MockitoJUnitRunner.class)
public class KubeConfigTest {
  @Test
  public void testContextStringReturnsProperly() {
    String result = KubeConfig.contextString("testProject", "us-central1-c", "testCluster");
    assertNotNull(result);
    assertEquals(result, "gke_testProject_us-central1-c_testCluster");
  }

  @Test
  public void testClusterServerReturnsProperly() {
    Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getEndpoint()).thenReturn("testEndpoint");
    String result = KubeConfig.clusterServer(cluster);
    assertNotNull(result);
    assertEquals(result, "https://testEndpoint");
  }

  @Test
  public void testFromClusterReturnsProperly() throws Exception {
    Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getEndpoint()).thenReturn("testEndpoint");
    Mockito.when(cluster.getZone()).thenReturn("us-central1-c");
    Mockito.when(cluster.getName()).thenReturn("testCluster");
    MasterAuth auth = Mockito.mock(MasterAuth.class);
    Mockito.when(cluster.getMasterAuth()).thenReturn(auth);
    Mockito.when(auth.getClientCertificate()).thenReturn("testClientCert");
    Mockito.when(auth.getClientKey()).thenReturn("testClientKey");
    Mockito.when(auth.getClusterCaCertificate()).thenReturn("testCaCert");

    KubeConfig result = KubeConfig.fromCluster("testProject", cluster);
    assertNotNull(result);

    String currentContext = result.getCurrentContext();
    assertNotNull(currentContext);
    assertEquals(
        currentContext, KubeConfig.contextString("testProject", "us-central1-c", "testCluster"));
    assertNotNull(result.getUsers());
    assertEquals(result.getUsers().size(), 1);
    assertNotNull(result.getContexts());
    assertEquals(result.getContexts().size(), 1);
    assertNotNull(result.getClusters());
    assertEquals(result.getClusters().size(), 1);
    // NOTE: The verification of the contents happens in the toYaml test
  }

  @Test
  public void testToYamlReturnsProperly() throws Exception {
    Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getEndpoint()).thenReturn("testEndpoint");
    Mockito.when(cluster.getZone()).thenReturn("us-central1-c");
    Mockito.when(cluster.getName()).thenReturn("testCluster");
    MasterAuth auth = Mockito.mock(MasterAuth.class);
    Mockito.when(cluster.getMasterAuth()).thenReturn(auth);
    Mockito.when(auth.getClientCertificate()).thenReturn("testClientCert");
    Mockito.when(auth.getClientKey()).thenReturn("testClientKey");
    Mockito.when(auth.getClusterCaCertificate()).thenReturn("testCaCert");
    KubeConfig config = KubeConfig.fromCluster("testProject", cluster);
    String result = config.toYaml();

    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    BufferedReader reader =
        Files.newBufferedReader(
            Paths.get(KubeConfigTest.class.getResource("/expectedKubeConfig.yml").toURI()));
    reader.lines().forEach(printWriter::println);
    printWriter.flush();
    String expected = writer.toString();
    assertTrue(yamlEquals(expected, result));
  }

  private static boolean yamlEquals(String expectedYaml, String testYaml) throws IOException {
    Yaml yaml = new Yaml(new SafeConstructor());
    Map<String, Object> testConfig =
        (Map<String, Object>) yaml.load(new BufferedReader(new StringReader(testYaml)));
    Map<String, Object> expectedConfig =
        (Map<String, Object>) yaml.load(new BufferedReader(new StringReader(expectedYaml)));

    TriFunction<TriFunction, Object, Object, Boolean> deepCollectionEquals =
        (f, expected, test) -> {
          if (!expected.getClass().equals(test.getClass())) {
            return false;
          }

          if (expected instanceof Map) {
            Map<String, Object> expectedMap = (Map<String, Object>) expected;
            Map<String, Object> testMap = (Map<String, Object>) test;
            for (String key : expectedMap.keySet()) {
              if (!(Boolean) f.apply(f, expectedMap.get(key), testMap.get(key))) {
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

              if (!(Boolean) f.apply(f, expectedItr.next(), testItr.next())) {
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
