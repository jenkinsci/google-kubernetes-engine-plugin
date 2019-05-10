package com.google.jenkins.plugins.k8sengine;

import com.google.common.collect.ImmutableList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests for verifying the use of the verboseLogging flag in {@link KubectlWrapper}.*/
@RunWith(MockitoJUnitRunner.class)
public class KubectlWrapperVerboseLoggingTest {
  @Test(expected = IOException.class)
  public void testQuietLoggingApplied() throws IOException, InterruptedException {
    KubectlWrapper kubectlWrapper =
        Mockito.spy(
            new KubectlWrapper.Builder()
                .launcher(setUpLauncher())
                .kubeConfig(setUpKubeconfig())
                .workspace(setUpWorkspace())
                .namespace("")
                .verboseLogging(false)
                .build());
    kubectlWrapper.runKubectlCommand("apply", ImmutableList.of( "-f", "manifest.yaml"));
  }

  @Test
  public void testVerboseLoggingApplied() throws IOException, InterruptedException {
    KubectlWrapper kubectlWrapper =
        Mockito.spy(
            new KubectlWrapper.Builder()
                .launcher(setUpLauncher())
                .kubeConfig(setUpKubeconfig())
                .workspace(setUpWorkspace())
                .namespace("")
                .verboseLogging(true)
                .build());
    kubectlWrapper.runKubectlCommand("apply", ImmutableList.of( "-f", "manifest.yaml"));
  }

  private Launcher setUpLauncher() throws IOException, InterruptedException {
    ProcStarter procStarter = Mockito.mock(ProcStarter.class);
    Mockito.when(procStarter.cmds(Mockito.anyList())).thenReturn(procStarter);
    Mockito.when(procStarter.stdout(Mockito.any(OutputStream.class))).thenReturn(procStarter);
    Mockito.when(procStarter.stderr(Mockito.any(OutputStream.class))).thenReturn(procStarter);
    Mockito.when(procStarter.join()).thenReturn(0);
    ProcStarter quietProcStarter = Mockito.mock(ProcStarter.class);
    Mockito.when(quietProcStarter.join()).thenReturn(1);
    Mockito.when(procStarter.quiet(true)).thenReturn(quietProcStarter);
    Mockito.when(procStarter.quiet(false)).thenReturn(procStarter);
    Launcher launcher = Mockito.mock(Launcher.class);
    Mockito.when(launcher.launch()).thenReturn(procStarter);
    return launcher;
  }

  private KubeConfig setUpKubeconfig() throws IOException {
    KubeConfig kubeConfig = Mockito.mock(KubeConfig.class);
    Mockito.when(kubeConfig.toYaml()).thenReturn("yaml");
    Mockito.when(kubeConfig.getCurrentContext()).thenReturn("currentContext");
    return kubeConfig;
  }

  private FilePath setUpWorkspace() throws IOException, InterruptedException {
    FilePath kubeConfigFile = Mockito.mock(FilePath.class);
    Mockito.when(kubeConfigFile.getRemote()).thenReturn("remote");
    Mockito.when(kubeConfigFile.delete()).thenReturn(true);
    FilePath workspace = Mockito.mock(FilePath.class);
    Mockito.when(workspace.createTempFile(".kube","config")).thenReturn(kubeConfigFile);
    Mockito.when(workspace.child("remote")).thenReturn(kubeConfigFile);
    return workspace;
  }
}
