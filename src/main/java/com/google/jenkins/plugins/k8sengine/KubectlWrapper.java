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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.util.ArgumentListBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Encapsulates the logic of executing kubectl commands in the workspace. NOTE(craigatgoogle): This
 * is a temporary stop-gap measure which will be replaced once the Kubernetes server-side apply
 * method is added: <a href="https://github.com/kubernetes/enhancements/issues/555">issue#555</a>.
 */
public class KubectlWrapper {
  private static final Logger LOGGER = Logger.getLogger(KubectlWrapper.class.getName());
  private static final String CHARSET = "UTF-8";

  private KubeConfig kubeConfig;
  private Launcher launcher;
  private FilePath workspace;
  private String namespace;

  private KubectlWrapper() {}

  private KubeConfig getKubeConfig() {
    return kubeConfig;
  }

  private void setKubeConfig(KubeConfig kubeConfig) {
    this.kubeConfig = kubeConfig;
  }

  private void setLauncher(Launcher launcher) {
    this.launcher = launcher;
  }

  private Launcher getLauncher() {
    return launcher;
  }

  private void setWorkspace(FilePath workspace) {
    this.workspace = workspace;
  }

  private FilePath getWorkspace() {
    return workspace;
  }

  private void setNamespace(String namespace) {
    this.namespace = namespace == null ? "" : namespace;
  }

  private String getNamespace() {
    return this.namespace;
  }

  /**
   * Runs the specified kubectl command.
   *
   * @param command The kubectl command to be run.
   * @param args Arguments for the command.
   * @throws IOException If an error occurred while executing the command.
   * @throws InterruptedException If an error occured while executing the command.
   * @return result From kubectl command.
   */
  public String runKubectlCommand(String command, ImmutableList<String> args)
      throws IOException, InterruptedException {
    String output = "";
    Set<String> tempFiles = new HashSet<>();
    try {
      // Set up the kubeconfig file for authentication
      FilePath kubeConfigFile = workspace.createTempFile(".kube", "config");
      tempFiles.add(kubeConfigFile.getRemote());
      String config = getKubeConfig().toYaml();

      // Setup the kubeconfig
      kubeConfigFile.write(config, /* encoding */ null);
      launchAndJoinCommand(
          getLauncher(),
          new ArgumentListBuilder()
              .add("kubectl")
              .add("--kubeconfig")
              .add(kubeConfigFile.getRemote())
              .add("config")
              .add("use-context")
              .add(kubeConfig.getCurrentContext())
              .toList());

      // Run the kubectl command
      ArgumentListBuilder kubectlCmdBuilder =
          new ArgumentListBuilder()
              .add("kubectl")
              .add("--kubeconfig")
              .add(kubeConfigFile.getRemote())
              .add(command);
      if (!namespace.isEmpty()) {
        kubectlCmdBuilder.add("--namespace").add(namespace);
      }
      args.forEach(kubectlCmdBuilder::add);
      output = launchAndJoinCommand(getLauncher(), kubectlCmdBuilder.toList());
    } catch (IOException | InterruptedException e) {
      LOGGER.log(
          Level.SEVERE,
          String.format("Failed to execute kubectl command: %s, args: %s", command, args),
          e);
      throw e;
    } finally {
      for (String tempFile : tempFiles) {
        getWorkspace().child(tempFile).delete();
      }
    }

    return output;
  }

  private static String launchAndJoinCommand(Launcher launcher, List<String> args)
      throws IOException, InterruptedException {
    ByteArrayOutputStream cmdLogStream = new ByteArrayOutputStream();
    int status = launcher.launch().cmds(args).stderr(cmdLogStream).stdout(cmdLogStream).join();
    if (status != 0) {
      String logs = cmdLogStream.toString(CHARSET);
      LOGGER.log(Level.SEVERE, String.format("kubectl command log: %s", logs));
      throw new IOException(
          String.format(
              "Failed to launch command args: %s, status: %s. Logs: %s", args, status, logs));
    }

    return cmdLogStream.toString(CHARSET);
  }

  /**
   * Using the kubectl CLI tool as the API client for the caller, this method unmarshalls the JSON
   * output of the CLI to a JSON Object.
   *
   * @param kind The kind of Kubernetes Object.
   * @param name The name of the Kubernetes Object.
   * @return The JSON object unmarshalled from the kubectl get command's output.
   * @throws IOException If an error occurred while executing the command.
   * @throws InterruptedException If an error occurred while executing the command.
   */
  public Object getObject(String kind, String name) throws IOException, InterruptedException {
    String json = runKubectlCommand("get", ImmutableList.<String>of(kind, name, "-o", "json"));
    return Configuration.defaultConfiguration().jsonProvider().parse(json);
  }

  /**
   * Using the kubectl CLI tool as the API client for the caller, this method unmarshalls the JSON
   * output of objects matching the supplied labels.
   *
   * @param kind The kind of Kubernetes Object.
   * @param labels The key-value labels set represented as a map.
   * @return A list of JSON Objects unmarshalled from the kubectl get command's output.
   * @throws IOException If an error occurred while executing the command.
   * @throws InterruptedException If an error occurred while executing the command.
   * @throws InvalidJsonException If an error occurred parsing the JSON return value.
   */
  @SuppressWarnings("unchecked")
  public ImmutableList<Object> getObjectsThatMatchLabels(String kind, Map<String, String> labels)
      throws IOException, InterruptedException, InvalidJsonException {
    String labelsArg =
        labels.keySet().stream()
            .map((k) -> String.format("%s=%s", k, labels.get(k)))
            .collect(Collectors.joining(","));
    String json = runKubectlCommand("get", ImmutableList.<String>of(kind + "s", labelsArg));
    Map<String, Object> result =
        (Map<String, Object>) Configuration.defaultConfiguration().jsonProvider().parse(json);
    List<Object> items = (List<Object>) result.get("items");
    return ImmutableList.copyOf(items);
  }

  /** Builder for {@link KubectlWrapper}. */
  public static class Builder {
    private KubectlWrapper wrapper = new KubectlWrapper();

    /**
     * Sets the {@link Launcher} to be used by the wrapper.
     *
     * @param launcher The {@link Launcher} to be set.
     * @return A reference to the {@link Builder}.
     */
    public Builder launcher(Launcher launcher) {
      wrapper.setLauncher(launcher);
      return this;
    }

    /**
     * Sets the {@link KubeConfig} to be used by the wrapper.
     *
     * @param kubeConfig The {@link KubeConfig} to be set.
     * @return A reference to the {@link Builder}.
     */
    public Builder kubeConfig(KubeConfig kubeConfig) {
      wrapper.setKubeConfig(kubeConfig);
      return this;
    }

    /**
     * Sets the workspace to be used by the wrapper.
     *
     * @param workspace The workspace to be set.
     * @return A reference to the {@link Builder}.
     */
    public Builder workspace(FilePath workspace) {
      wrapper.setWorkspace(workspace);
      return this;
    }

    /**
     * Sets the namespace to be used by the wrapper.
     *
     * @param namespace The namespace to be set.
     * @return A reference to the {@link Builder}.
     */
    public Builder namespace(String namespace) {
      wrapper.setNamespace(namespace);
      return this;
    }

    /**
     * Builds a new {@link KubectlWrapper}.
     *
     * @return A new {@link KubectlWrapper}.
     */
    public KubectlWrapper build() {
      Preconditions.checkNotNull(wrapper.getLauncher());
      Preconditions.checkNotNull(wrapper.getKubeConfig());
      Preconditions.checkNotNull(wrapper.getWorkspace());
      Preconditions.checkNotNull(wrapper.getNamespace());
      return wrapper;
    }
  }
}
