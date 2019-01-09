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

import com.google.common.collect.ImmutableList;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the logic of executing kubectl commands in the workspace. NOTE(craigatgoogle): This
 * is a temporary stop-gap measure which will be replaced once the Kubernetes server-side apply
 * method is added: <a href="https://github.com/kubernetes/enhancements/issues/555">issue#555</a>.
 */
public class KubectlWrapper {
  private static final Logger LOGGER = Logger.getLogger(KubectlWrapper.class.getName());

  /**
   * Runs the specified kubectl command within the specified {@link JenkinsRunContext}'s
   * environment.
   *
   * @param context The {@link JenkinsRunContext} whose environment the command will execute in.
   * @param kubeConfig The {@link KubeConfig} containing the credentials for the cluster being
   *     executed against.
   * @param command The kubectl command to be run.
   * @param args Arguments for the command.
   * @throws IOException If an error occurred while executing the command.
   * @throws InterruptedException If an error occured while executing the command.
   */
  public static void runKubectlCommand(
      JenkinsRunContext context, KubeConfig kubeConfig, String command, ImmutableList<String> args)
      throws IOException, InterruptedException {
    Set<String> tempFiles = new HashSet<>();
    try {
      // Set up the kubeconfig file for authentication
      FilePath kubeConfigFile = context.getWorkspace().createTempFile(".kube", "config");
      tempFiles.add(kubeConfigFile.getRemote());
      StringWriter configWriter = new StringWriter();
      kubeConfig.toYaml(configWriter);

      // Setup the kubeconfig
      kubeConfigFile.write(configWriter.toString(), /* encoding */ null);
      EnvVars envVars = context.getRun().getEnvironment(context.getTaskListener());
      envVars.put("KUBECONFIG", kubeConfigFile.getRemote());
      launchAndJoinCommand(
          context.getLauncher(),
          new ArgumentListBuilder()
              .add("kubectl")
              .add("config")
              .add("use-context")
              .add(kubeConfig.getCurrentContext())
              .toList());

      // Run the kubectl command
      ArgumentListBuilder kubectlCmdBuilder = new ArgumentListBuilder().add("kubectl").add(command);
      args.forEach(kubectlCmdBuilder::add);
      launchAndJoinCommand(context.getLauncher(), kubectlCmdBuilder.toList());
    } catch (IOException | InterruptedException e) {
      LOGGER.log(
          Level.SEVERE,
          String.format("Failed to execute kubectl command: %s, args: %s", command, args),
          e);
      throw e;
    } finally {
      for (String tempFile : tempFiles) {
        context.getWorkspace().child(tempFile).delete();
      }
    }
  }

  private static void launchAndJoinCommand(Launcher launcher, List<String> args)
      throws IOException, InterruptedException {
    ByteArrayOutputStream cmdLogStream = new ByteArrayOutputStream();
    int status = launcher.launch().cmds(args).stderr(cmdLogStream).stdout(cmdLogStream).join();
    if (status != 0) {
      LOGGER.log(Level.SEVERE, String.format("kubectl command log: %s", cmdLogStream.toString()));
      throw new IOException(
          String.format("Failed to launch command args: %s, status: %s", args, status));
    }
  }
}
