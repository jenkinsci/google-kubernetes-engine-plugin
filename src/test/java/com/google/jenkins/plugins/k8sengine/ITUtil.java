/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jenkins.plugins.k8sengine;

import com.google.common.io.ByteStreams;
import hudson.FilePath;
import hudson.model.Project;
import hudson.model.Run;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.rules.TemporaryFolder;

/** Provides a library of utility functions for integration tests. */
public class ITUtil {
  /**
   * Formats a random name using the given prefix.
   *
   * @param prefix The prefix to be randomly formatted.
   * @return The randomly formatted name.
   */
  static String formatRandomName(String prefix) {
    return String.format("%s-%s", prefix, UUID.randomUUID().toString().replace("-", ""));
  }

  /**
   * Creates a temporary workspace for testing, configuring it as a custom workspace for the
   * specified {@link Project}.
   *
   * @param testProject The {@link Project} the test workspace for.
   * @return The {@link TemporaryFolder} serving as the test workspace.
   * @throws IOException If an error occurred while creating the test workspace.
   */
  static TemporaryFolder createTestWorkspace(Project testProject) throws IOException {
    TemporaryFolder testWorkspace = new TemporaryFolder();
    testWorkspace.create();
    testProject.setCustomWorkspace(testWorkspace.getRoot().toString());
    return testWorkspace;
  }

  /**
   * Copies the contents of the specified file to the specified directory.
   *
   * @param testClass The test class related to the file being copied.
   * @param toDir The path of the target directory.
   * @param testFile The test file to copied.
   * @throws IOException If an error occurred while copying test file.
   * @throws InterruptedException If an error occurred while copying test file.
   */
  static void copyTestFileToDir(Class testClass, String toDir, String testFile)
      throws IOException, InterruptedException {
    FilePath dirPath = new FilePath(new File(toDir));
    String testFileContents = loadResource(testClass, testFile);
    FilePath testWorkspaceFile = dirPath.child(testFile);
    testWorkspaceFile.write(testFileContents, StandardCharsets.UTF_8.toString());
  }

  /**
   * Loads the content of the specified resource.
   *
   * @param testClass The test class the resource is being loaded for.
   * @param name The name of the resource being loaded.
   * @return The contents of the loaded resource.
   * @throws IOException If an error occurred during loading.
   */
  static String loadResource(Class testClass, String name) throws IOException {
    return new String(ByteStreams.toByteArray(testClass.getResourceAsStream(name)));
  }

  /**
   * Dumps the logs from the specified {@link Run}.
   *
   * @param logger The {@link Logger} to be written to.
   * @param run The {@link Run} from which the logs will be read.
   * @throws IOException If an error occurred while dumping the logs.
   */
  static void dumpLog(Logger logger, Run<?, ?> run) throws IOException {
    BufferedReader reader = new BufferedReader(run.getLogReader());
    String line = null;
    while ((line = reader.readLine()) != null) {
      logger.info(line);
    }
  }
}
