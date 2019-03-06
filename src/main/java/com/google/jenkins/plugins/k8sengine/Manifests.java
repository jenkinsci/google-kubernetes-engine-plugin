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

import hudson.FilePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility library for loading Kubernetes manifests files from a {@link FilePath} into a list of
 * descriptive wrappers, {@link ManifestObject}.
 */
public class Manifests {
  static Yaml yaml = new Yaml();
  private List<ManifestObject> objects = new ArrayList<ManifestObject>();

  /** ManifestObject wrapper that encapsulates an object spec loaded from a supplied manifest. */
  public static class ManifestObject {
    private Map<String, Object> source;

    /**
     * Build the manifest object from source.
     *
     * @param source The YAML map source for the object.
     */
    public ManifestObject(Map<String, Object> source) {
      this.source = source;
    }

    /** @return The YAML map source for the object. */
    public Map<String, Object> getSource() {
      return source;
    }

    /** @return The apiVersion. */
    public String getApiVersion() {
      return (String) source.get("apiVersion");
    }

    /** @return The kind. */
    public String getKind() {
      return (String) source.get("kind");
    }

    /** @return The name. */
    @SuppressWarnings("unchecked")
    public String getName() {
      Map<String, Object> metadata = (Map<String, Object>) source.get("metadata");
      return (String) metadata.get("name");
    }

    /** @return The description of the object in {ApiVersion}/{Kind}: {Name} */
    public String describe() {
      return String.format("%s/%s: %s", getApiVersion(), getKind(), getName());
    }
  }

  /** Private constructor constructs {@link Manifests} from a FilePath. */
  private Manifests(FilePath filePath) throws IOException, InterruptedException {
    this(Arrays.asList(new FilePath[] {filePath}));
  }

  /** Private constructor constructs {@link Manifests} from a list of FilePaths. */
  private Manifests(List<FilePath> files) throws IOException, InterruptedException {
    for (FilePath fp : files) {
      loadFile(fp);
    }
  }

  /**
   * Factory method for single {@link FilePath}.
   *
   * @param file The {@link FilePath} containing the manifests.
   * @return A {@link Manifests} object containing the individual manifests contained at the file
   *     path.
   * @throws IOException If an error occurred while loading the file.
   * @throws InterruptedException If a threading error occurred while loading the file.
   */
  public static Manifests fromFile(FilePath file) throws IOException, InterruptedException {
    if (file.isDirectory()) {
      return new Manifests(Arrays.asList(file.list("**/*")));
    }

    return new Manifests(file);
  }

  /**
   * Factory method for list of {@link FilePath} objects.
   *
   * @param files The list of {@link FilePath} objects containing the manifests.
   * @return Manifests object constructed based on the list of {@link FilePath}'s.
   * @throws IOException If an error occurred while loading the file.
   * @throws InterruptedException If a threading error occurred while loading the file.
   */
  public static Manifests fromFileList(List<FilePath> files)
      throws IOException, InterruptedException {
    return new Manifests(files);
  }

  /** Loads the file with the given path (Assuming it's a file). */
  @SuppressWarnings("unchecked")
  private void loadFile(FilePath filePath) throws IOException, InterruptedException {
    InputStream mis = filePath.read();
    Iterable<Object> iter = yaml.loadAll(new InputStreamReader(mis, "UTF-8"));
    iter.forEach((o) -> objects.add(new ManifestObject((Map<String, Object>) o)));
  }

  /** @return The {@link ManifestObject}'s that were loaded. */
  public List<ManifestObject> getObjectManifests() {
    return objects;
  }

  /**
   * Get the list of {@link ManifestObject} that match the given kind.
   *
   * @param includedKinds The kinds of Kubernetes objects to include in the list.
   * @return The manifest objects that match the included kinds.
   */
  public List<ManifestObject> getObjectManifestsOfKinds(Set<String> includedKinds) {
    return getObjectManifests().stream()
        .filter((manifest) -> includedKinds.contains(manifest.getKind().toLowerCase()))
        .collect(Collectors.toList());
  }
}
