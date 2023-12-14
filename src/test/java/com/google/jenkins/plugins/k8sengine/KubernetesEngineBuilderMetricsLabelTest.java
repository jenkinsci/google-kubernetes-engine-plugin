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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import hudson.FilePath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.yaml.snakeyaml.Yaml;

/** Tests the Kubernetes metrics label behaviors within {@link KubernetesEngineBuilder}. */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesEngineBuilderMetricsLabelTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testAddMetricsLabelProperlyAddsLabel() throws IOException, InterruptedException {
        FilePath manifestFile = Mockito.mock(FilePath.class);
        Mockito.when(manifestFile.read())
                .thenReturn(new ByteArrayInputStream(String.join(
                                "\n",
                                "apiVersion: apps/v1",
                                "kind: Deployment",
                                "metadata:",
                                "  name: nginx-deployment",
                                "  labels:",
                                "    app: nginx")
                        .getBytes()));

        Mockito.doAnswer(invocation -> {
                    Yaml yaml = new Yaml();
                    Manifests.ManifestObject manifest = new Manifests.ManifestObject(
                            (Map<String, Object>) yaml.load((String) invocation.getArguments()[0]), manifestFile);
                    Map<String, String> labels = manifest.getOrCreateLabels();
                    assertNotNull(labels);
                    assertNotNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
                    assertEquals(
                            labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY),
                            KubernetesEngineBuilder.METRICS_LABEL_VALUE);
                    return null;
                })
                .when(manifestFile)
                .write(anyString(), anyString());
        KubernetesEngineBuilder.addMetricsLabel(manifestFile);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddMetricsLabelProperlyAppendsToExistingManagedByLabel() throws IOException, InterruptedException {
        FilePath manifestFile = Mockito.mock(FilePath.class);
        Mockito.when(manifestFile.read())
                .thenReturn(new ByteArrayInputStream(String.join(
                                "\n",
                                "apiVersion: apps/v1",
                                "kind: Deployment",
                                "metadata:",
                                "  name: nginx-deployment",
                                "  labels:",
                                "    app: nginx",
                                "    app.kubernetes.io/managed-by: helm")
                        .getBytes()));

        Mockito.doAnswer(invocation -> {
                    Yaml yaml = new Yaml();
                    Manifests.ManifestObject manifest = new Manifests.ManifestObject(
                            (Map<String, Object>) yaml.load((String) invocation.getArguments()[0]), manifestFile);
                    Map<String, String> labels = manifest.getOrCreateLabels();
                    assertNotNull(labels);
                    assertNotNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
                    List<String> managedByLabelValues =
                            Arrays.asList(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY)
                                    .split(","));
                    assertTrue(managedByLabelValues.contains(KubernetesEngineBuilder.METRICS_LABEL_VALUE));
                    assertTrue(managedByLabelValues.contains("helm"));
                    return null;
                })
                .when(manifestFile)
                .write(anyString(), anyString());
        KubernetesEngineBuilder.addMetricsLabel(manifestFile);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddMetricsLabelProperlyAddsToMissingLabels() throws IOException, InterruptedException {
        FilePath manifestFile = Mockito.mock(FilePath.class);
        Mockito.when(manifestFile.read())
                .thenReturn(new ByteArrayInputStream(
                        String.join("\n", "apiVersion: apps/v1", "kind: Service", "metadata:", "  name: nginx-service")
                                .getBytes()));

        Mockito.doAnswer(invocation -> {
                    Yaml yaml = new Yaml();
                    Manifests.ManifestObject manifest = new Manifests.ManifestObject(
                            (Map<String, Object>) yaml.load((String) invocation.getArguments()[0]), manifestFile);
                    Map<String, String> labels = manifest.getOrCreateLabels();
                    assertNotNull(labels);
                    assertNotNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
                    assertEquals(
                            labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY),
                            KubernetesEngineBuilder.METRICS_LABEL_VALUE);
                    return null;
                })
                .when(manifestFile)
                .write(anyString(), anyString());
        KubernetesEngineBuilder.addMetricsLabel(manifestFile);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddMetricsLabelProperlyHandlesExistingMetricsLabel() throws IOException, InterruptedException {
        FilePath manifestFile = Mockito.mock(FilePath.class);
        Mockito.when(manifestFile.read())
                .thenReturn(new ByteArrayInputStream(String.join(
                                "\n",
                                "apiVersion: apps/v1",
                                "kind: Service",
                                "metadata:",
                                "  name: nginx-service",
                                "  app.kubernetes.io/managed-by: graphite-jenkins-gke")
                        .getBytes()));

        Mockito.doAnswer(invocation -> {
                    Yaml yaml = new Yaml();
                    Manifests.ManifestObject manifest = new Manifests.ManifestObject(
                            (Map<String, Object>) yaml.load((String) invocation.getArguments()[0]), manifestFile);
                    Map<String, String> labels = manifest.getOrCreateLabels();
                    assertNotNull(labels);
                    assertNotNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
                    assertEquals(
                            labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY),
                            KubernetesEngineBuilder.METRICS_LABEL_VALUE);
                    return null;
                })
                .when(manifestFile)
                .write(anyString(), anyString());
        KubernetesEngineBuilder.addMetricsLabel(manifestFile);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddMetricsLabelProperlyHandlesMultipleObjects() throws IOException, InterruptedException {
        FilePath manifestFile = Mockito.mock(FilePath.class);
        Mockito.when(manifestFile.read())
                .thenReturn(new ByteArrayInputStream(String.join(
                                "\n",
                                "apiVersion: apps/v1",
                                "kind: Service",
                                "metadata:",
                                "  name: nginx-service",
                                "\n",
                                "---",
                                "apiVersion: apps/v1",
                                "kind: Deployment",
                                "metadata:",
                                "  name: nginx-deployment",
                                "\n",
                                "---",
                                "apiVersion: apps/v1",
                                "kind: ReplicaSet",
                                "metadata:",
                                "  name: nginx-replicaset")
                        .getBytes()));

        Mockito.doAnswer(invocation -> {
                    Yaml yaml = new Yaml();
                    for (Object yamlObj : yaml.loadAll((String) invocation.getArguments()[0])) {
                        Manifests.ManifestObject manifest =
                                new Manifests.ManifestObject((Map<String, Object>) yamlObj, manifestFile);
                        Map<String, String> labels = manifest.getOrCreateLabels();
                        assertNotNull(labels);
                        assertNotNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
                        assertEquals(
                                labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY),
                                KubernetesEngineBuilder.METRICS_LABEL_VALUE);
                    }
                    return null;
                })
                .when(manifestFile)
                .write(anyString(), anyString());
        KubernetesEngineBuilder.addMetricsLabel(manifestFile);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddMetricsLabelDoesNotAddLabelProperly() throws IOException, InterruptedException {
        FilePath manifestFile = Mockito.mock(FilePath.class);
        Mockito.when(manifestFile.read())
                .thenReturn(new ByteArrayInputStream(String.join(
                                "\n",
                                "apiVersion: apps/v1",
                                "kind: Ingress",
                                "metadata:",
                                "  name: nginx-ingress",
                                "  labels:",
                                "    app: nginx")
                        .getBytes()));

        Mockito.doAnswer(invocation -> {
                    Yaml yaml = new Yaml();
                    Manifests.ManifestObject manifest = new Manifests.ManifestObject(
                            (Map<String, Object>) yaml.load((String) invocation.getArguments()[0]), manifestFile);
                    Map<String, String> labels = manifest.getOrCreateLabels();
                    assertNotNull(labels);
                    assertNull(labels.get(KubernetesEngineBuilder.METRICS_LABEL_KEY));
                    return null;
                })
                .when(manifestFile)
                .write(anyString(), anyString());
        KubernetesEngineBuilder.addMetricsLabel(manifestFile);
    }
}
