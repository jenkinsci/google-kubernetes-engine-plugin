package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import hudson.FilePath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

public class KubernetesEngineBuilderNamespaceTest {
  @Test
  public void testAddStarNamespaceWithNoPreviousNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = Mockito.mock(FilePath.class);
    Mockito.when(manifestFile.read())
        .thenReturn(
            new ByteArrayInputStream(
                String.join(
                        "\n",
                        "apiVersion: apps/v1",
                        "kind: Deployment",
                        "metadata:",
                        "  name: nginx-deployment",
                        "  labels:",
                        "     app: nginx")
                    .getBytes()));
    Mockito.doAnswer(
            invocation -> {
              Yaml yaml = new Yaml();
              Manifests.ManifestObject manifest =
                  new Manifests.ManifestObject(
                      yaml.load((String) invocation.getArguments()[0]), manifestFile);
              Optional<String> namespace = manifest.getNamespace();
              assertNotNull(namespace);
              assertTrue(namespace.isPresent());
              assertEquals("default", namespace.get());
              return null;
            })
        .when(manifestFile)
        .write(anyString(), anyString());
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "*");
    assertEquals("default", commandNameSpace);
  }

  @Test
  public void testAddStarNamespaceWithExistingNamespace() throws IOException, InterruptedException {
    FilePath manifestFile = Mockito.mock(FilePath.class);
    Mockito.when(manifestFile.read())
        .thenReturn(
            new ByteArrayInputStream(
                String.join(
                        "\n",
                        "apiVersion: apps/v1",
                        "kind: Deployment",
                        "metadata:",
                        "  name: nginx-deployment",
                        "  namespace: test",
                        "  labels:",
                        "     app: nginx")
                    .getBytes()));
    Mockito.doAnswer(
            invocation -> {
              Yaml yaml = new Yaml();
              Manifests.ManifestObject manifest =
                  new Manifests.ManifestObject(
                      yaml.load((String) invocation.getArguments()[0]), manifestFile);
              Optional<String> namespace = manifest.getNamespace();
              assertNotNull(namespace);
              assertTrue(namespace.isPresent());
              assertEquals("test", namespace.get());
              return null;
            })
        .when(manifestFile)
        .write(anyString(), anyString());
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "*");
    assertEquals("test", commandNameSpace);
  }

  @Test
  public void testAddCustomNamespaceWithNoPreviousNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = Mockito.mock(FilePath.class);
    Mockito.when(manifestFile.read())
        .thenReturn(
            new ByteArrayInputStream(
                String.join(
                        "\n",
                        "apiVersion: apps/v1",
                        "kind: Deployment",
                        "metadata:",
                        "  name: nginx-deployment",
                        "  labels:",
                        "     app: nginx")
                    .getBytes()));
    Mockito.doAnswer(
            invocation -> {
              Yaml yaml = new Yaml();
              Manifests.ManifestObject manifest =
                  new Manifests.ManifestObject(
                      yaml.load((String) invocation.getArguments()[0]), manifestFile);
              Optional<String> namespace = manifest.getNamespace();
              assertNotNull(namespace);
              assertTrue(namespace.isPresent());
              assertEquals("test", namespace.get());
              return null;
            })
        .when(manifestFile)
        .write(anyString(), anyString());
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "test");
    assertEquals("test", commandNameSpace);
  }

  @Test
  public void testAddCustomNamespaceWithPreviousNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = Mockito.mock(FilePath.class);
    Mockito.when(manifestFile.read())
        .thenReturn(
            new ByteArrayInputStream(
                String.join(
                        "\n",
                        "apiVersion: apps/v1",
                        "kind: Deployment",
                        "metadata:",
                        "  name: nginx-deployment",
                        "  namespace: random",
                        "  labels:",
                        "     app: nginx")
                    .getBytes()));
    Mockito.doAnswer(
            invocation -> {
              Yaml yaml = new Yaml();
              Manifests.ManifestObject manifest =
                  new Manifests.ManifestObject(
                      yaml.load((String) invocation.getArguments()[0]), manifestFile);
              Optional<String> namespace = manifest.getNamespace();
              assertNotNull(namespace);
              assertTrue(namespace.isPresent());
              assertEquals("default", namespace.get());
              return null;
            })
        .when(manifestFile)
        .write(anyString(), anyString());
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "default");
    assertEquals("default", commandNameSpace);
  }
}
