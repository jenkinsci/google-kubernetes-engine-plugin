package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import hudson.FilePath;
import hudson.Launcher;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests for namespace functionality in {@link KubectlWrapper}. */
@RunWith(MockitoJUnitRunner.class)
public class KubectlWrapperNamespaceTest {

  @Mock Launcher launcher;

  @Mock KubeConfig kubeConfig;

  @Mock FilePath workspace;

  @Test
  public void testCreateNamespaceWithExistingNamespace() throws IOException, InterruptedException {
    KubectlWrapper kubectl =
        Mockito.spy(
            new KubectlWrapper.Builder()
                .kubeConfig(kubeConfig)
                .launcher(launcher)
                .namespace("default")
                .workspace(workspace)
                .build());
    Mockito.doReturn("getOutput").when(kubectl).getObject("namespace", "default");
    assertFalse(kubectl.createNamespaceIfMissing());
  }

  @Test
  public void testCreateNamespaceWithMissingNamespace() throws IOException, InterruptedException {
    KubectlWrapper kubectl =
        Mockito.spy(
            new KubectlWrapper.Builder()
                .kubeConfig(kubeConfig)
                .launcher(launcher)
                .namespace("test")
                .workspace(workspace)
                .build());
    Mockito.doThrow(new IOException()).when(kubectl).getObject("namespace", "test");
    Mockito.doReturn("createOutput")
        .when(kubectl)
        .runKubectlCommand("create", ImmutableList.of("namespace", "test"));
    assertTrue(kubectl.createNamespaceIfMissing());
  }

  @Test
  public void testCreateNamespaceWithBlankNamespace() throws IOException, InterruptedException {
    KubectlWrapper kubectl =
        Mockito.spy(
            new KubectlWrapper.Builder()
                .kubeConfig(kubeConfig)
                .launcher(launcher)
                .namespace("")
                .workspace(workspace)
                .build());
    assertFalse(kubectl.createNamespaceIfMissing());
  }

  @Test(expected = IOException.class)
  public void testCreateNameNamespaceWithMissingNamespaceExceptionWhenCreateFails()
      throws IOException, InterruptedException {
    KubectlWrapper kubectl =
        Mockito.spy(
            new KubectlWrapper.Builder()
                .kubeConfig(kubeConfig)
                .launcher(launcher)
                .namespace("test")
                .workspace(workspace)
                .build());
    Mockito.doThrow(new IOException()).when(kubectl).getObject("namespace", "test");
    Mockito.doThrow(new IOException())
        .when(kubectl)
        .runKubectlCommand("create", ImmutableList.of("namespace", "test"));
    kubectl.createNamespaceIfMissing();
  }
}
