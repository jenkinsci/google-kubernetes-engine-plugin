package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import com.google.common.base.Strings;
import com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.DescriptorImpl;
import hudson.FilePath;
import hudson.util.FormValidation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

public class KubernetesEngineBuilderNamespaceTest {
  @Test
  public void testAddBlankNamespaceWithNoManifestNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = setupManifest("", "default");
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "");
    assertEquals("default", commandNameSpace);
  }

  @Test
  public void testAddBlankNamespaceWithManifestNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = setupManifest("test", "test");
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "");
    assertEquals("test", commandNameSpace);
  }

  @Test
  public void testAddCustomNamespaceWithNoManifestNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = setupManifest("", "test");
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "test");
    assertEquals("test", commandNameSpace);
  }

  @Test
  public void testAddCustomNamespaceWithManifestNamespace()
      throws IOException, InterruptedException {
    FilePath manifestFile = setupManifest("random", "default");
    String commandNameSpace = KubernetesEngineBuilder.addNamespace(manifestFile, "default");
    assertEquals("default", commandNameSpace);
  }

  @Test
  public void testAddBlankNamespaceWithMultipleManifestNamespaces()
      throws IOException, InterruptedException {
    FilePath manifestPath = Mockito.mock(FilePath.class);
    FilePath testManifest = setupManifest("test", "test");
    FilePath defaultManifest = setupManifest("default", "default");
    Mockito.when(manifestPath.isDirectory()).thenReturn(true);
    FilePath[] manifests = {testManifest, defaultManifest};
    Mockito.when(manifestPath.list(anyString())).thenReturn(manifests);
    String commandNamespace = KubernetesEngineBuilder.addNamespace(manifestPath, "");
    assertEquals("", commandNamespace);
  }

  @Test
  public void testAddCustomNamespaceWithMultipleManifestNamespaces()
      throws IOException, InterruptedException {
    FilePath manifestPath = Mockito.mock(FilePath.class);
    FilePath testManifest = setupManifest("test", "custom");
    FilePath defaultManifest = setupManifest("default", "custom");
    Mockito.when(manifestPath.isDirectory()).thenReturn(true);
    FilePath[] manifests = {testManifest, defaultManifest};
    Mockito.when(manifestPath.list(anyString())).thenReturn(manifests);
    String commandNamespace = KubernetesEngineBuilder.addNamespace(manifestPath, "custom");
    assertEquals("custom", commandNamespace);
  }

  @Test(expected = NullPointerException.class)
  public void testDoCheckNamespaceNPEWithNull() {
    DescriptorImpl descriptor = new DescriptorImpl();
    descriptor.doCheckNamespace(null);
  }

  @Test
  public void testDoCheckNamespaceOKWithEmptyString() {
    DescriptorImpl descriptor = new DescriptorImpl();
    FormValidation result = descriptor.doCheckNamespace("");
    assertNotNull(result);
    assertEquals(FormValidation.ok().getMessage(), result.getMessage());
  }

  @Test
  public void testDoCheckNamespaceWithOKProperlyFormedString() {
    DescriptorImpl descriptor = new DescriptorImpl();
    FormValidation result = descriptor.doCheckNamespace("test-a-23-b");
    assertNotNull(result);
    assertEquals(FormValidation.ok().getMessage(), result.getMessage());
  }

  @Test
  public void testDoCheckNamespaceErrorWithValidCharactersMalformedString() {
    DescriptorImpl descriptor = new DescriptorImpl();
    FormValidation result = descriptor.doCheckNamespace("-test");
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_NamespaceInvalid(), result.getMessage());
  }

  @Test
  public void testDoCheckNamespaceErrorWithInvalidCharacters() {
    DescriptorImpl descriptor = new DescriptorImpl();
    FormValidation result = descriptor.doCheckNamespace("*");
    assertNotNull(result);
    assertEquals(Messages.KubernetesEngineBuilder_NamespaceInvalid(), result.getMessage());
  }

  /**
   * @param inputNamespace The namespace to be included in the original file contents if this is
   *     null or "", then no namespace line will be included
   * @param expectedNamespace The namespace that should be included on the object after writing.
   * @return The mocked FilePath for this manifest
   * @throws IOException Included because of the mocked use of read and write
   * @throws InterruptedException Included because of the mocked use of read and write
   */
  private FilePath setupManifest(String inputNamespace, String expectedNamespace)
      throws IOException, InterruptedException {
    FilePath manifestFile = Mockito.mock(FilePath.class);
    StringBuilder builder =
        new StringBuilder()
            .append("apiVersion: apps/v1\n")
            .append("kind: Deployment\n")
            .append("metadata:\n")
            .append("  name: nginx-deployment\n")
            .append("  labels:\n")
            .append("    app: nginx\n");
    if (!Strings.isNullOrEmpty(inputNamespace)) {
      builder.append("  namespace: ").append(inputNamespace);
    }
    Mockito.when(manifestFile.read())
        .thenReturn(new ByteArrayInputStream(builder.toString().getBytes()));
    Mockito.doAnswer(
            invocation -> {
              Yaml yaml = new Yaml();
              Manifests.ManifestObject manifest =
                  new Manifests.ManifestObject(
                      yaml.load((String) invocation.getArguments()[0]), manifestFile);
              Optional<String> actualNamespace = manifest.getNamespace();
              assertNotNull(actualNamespace);
              assertTrue(actualNamespace.isPresent());
              assertEquals(expectedNamespace, actualNamespace.get());
              return null;
            })
        .when(manifestFile)
        .write(anyString(), anyString());
    return manifestFile;
  }
}
