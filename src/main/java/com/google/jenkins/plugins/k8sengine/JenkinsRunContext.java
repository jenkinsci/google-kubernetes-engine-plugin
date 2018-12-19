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

import com.google.common.base.Preconditions;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

/** Serves as a simple struct encapsulating contextual data for a Jenkins run. */
public class JenkinsRunContext {
  private Run<?, ?> run;
  private FilePath workspace;
  private Launcher launcher;
  private TaskListener listener;

  private JenkinsRunContext() {}

  /** @return This context's {@link Run}. */
  public Run<?, ?> getRun() {
    return run;
  }

  private void setRun(Run<?, ?> run) {
    Preconditions.checkNotNull(run);
    this.run = run;
  }

  /** @return This context's workspace. */
  public FilePath getWorkspace() {
    return workspace;
  }

  private void setWorkspace(FilePath workspace) {
    this.workspace = workspace;
  }

  /** @return This context's {@link Launcher}. */
  public Launcher getLauncher() {
    return launcher;
  }

  private void setLauncher(Launcher launcher) {
    this.launcher = launcher;
  }

  /** @return This context's {@link TaskListener}. */
  public TaskListener getTaskListener() {
    return listener;
  }

  private void setTaskListener(TaskListener listener) {
    this.listener = listener;
  }

  /** Builder for {@link JenkinsRunContext}. */
  public static class Builder {
    private JenkinsRunContext context;

    public Builder() {
      context = new JenkinsRunContext();
    }

    public Builder run(Run<?, ?> run) {
      context.setRun(run);
      return this;
    }

    public Builder workspace(FilePath workspace) {
      context.setWorkspace(workspace);
      return this;
    }

    public Builder launcher(Launcher launcher) {
      context.setLauncher(launcher);
      return this;
    }

    public Builder taskListener(TaskListener listener) {
      context.setTaskListener(listener);
      return this;
    }

    public JenkinsRunContext build() {
      Preconditions.checkNotNull(context.getRun());
      Preconditions.checkNotNull(context.getWorkspace());
      Preconditions.checkNotNull(context.getLauncher());
      Preconditions.checkNotNull(context.getTaskListener());
      return context;
    }
  }
}
