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

import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.SecretBytes;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import hudson.FilePath;
import hudson.model.Project;
import hudson.model.Run;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
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
    return IOUtils.toString(testClass.getResourceAsStream(name), StandardCharsets.UTF_8);
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

  /**
   * Retrieves the location set through environment variables
   *
   * @return A Google Compute Resource Region (us-west1) or Zone (us-west1-a) string
   */
  static String getLocation() {
    String location = System.getenv("GOOGLE_PROJECT_LOCATION");
    if (location == null) {
      location = System.getenv("GOOGLE_PROJECT_ZONE");
    }
    assertNotNull("GOOGLE_PROJECT_LOCATION env var must be set", location);
    return location;
  }

  /**
   * Retrieves the credentials set through environment variables and converts into a Service Account
   * configuration for use in the Jenkins credential store.
   *
   * @return A {@link ServiceAccountConfig} for the provided Json Key.
   */
  public static ServiceAccountConfig getServiceAccountConfig() {
    String serviceAccountKeyJson = System.getenv("GOOGLE_CREDENTIALS");
    assertNotNull("GOOGLE_CREDENTIALS env var must be set", serviceAccountKeyJson);
    SecretBytes bytes =
        SecretBytes.fromBytes(serviceAccountKeyJson.getBytes(StandardCharsets.UTF_8));
    JsonServiceAccountConfig config = new JsonServiceAccountConfig();
    config.setSecretJsonKey(bytes);
    assertNotNull(config.getAccountId());
    return config;
  }
}
