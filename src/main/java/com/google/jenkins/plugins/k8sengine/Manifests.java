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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility library for loading Kubernetes manifests files from a {@link FilePath} into a list of
 * descriptive wrappers, {@link ManifestObject}.
 */
public class Manifests {
  private static final String DEFAULT_ENCODING = "UTF-8";

  static Yaml yaml = new Yaml();
  private List<ManifestObject> objects = new ArrayList<ManifestObject>();

  /** ManifestObject wrapper that encapsulates an object spec loaded from a supplied manifest. */
  public static class ManifestObject {
    private Map<String, Object> source;
    private FilePath file;

    /**
     * Build the manifest object from source.
     *
     * @param source The YAML map source for the object.
     * @param file The file containing the manifest.
     */
    public ManifestObject(Map<String, Object> source, FilePath file) {
      this.source = source;
      this.file = file;
    }

    /** @return The file containing this manifest. */
    public FilePath getFile() {
      return file;
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
    public Optional<String> getName() {
      return getMetadata().isPresent()
          ? Optional.of((String) getMetadata().get().get("name"))
          : Optional.empty();
    }

    /** @return The namespace. */
    public Optional<String> getNamespace() {
      return getMetadata().isPresent()
          ? Optional.of((String) getMetadata().get().get("namespace"))
          : Optional.empty();
    }

    /**
     * Set the namespace. If namespace is "*" and this {@link ManifestObject} has a namespace, it
     * will not be changed. If namespace is "*" and no namespace currently exists, the namespace
     * will be set to "default".
     *
     * @param namespace The namespace to set on the {@link ManifestObject}'s metadata
     */
    public void setNamespace(String namespace) {
      Map<String, Object> metadata = getOrCreateMetadata();

      String currentNamespace = (String) metadata.getOrDefault("namespace", "default");
      if (namespace.equals("*")) {
        namespace = currentNamespace;
      }
      metadata.put("namespace", namespace);
    }

    /**
     * Ensures this {@link ManifestObject} has labels, modifying in-place as needed, finally
     * returning the labels.
     *
     * @return The labels for this {@link ManifestObject}.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getOrCreateLabels() {
      Map<String, Object> metadata = getOrCreateMetadata();

      if (!metadata.containsKey("labels")) {
        metadata.put("labels", new LinkedHashMap<String, String>());
      }

      return (Map<String, String>) metadata.get("labels");
    }

    /**
     * Adds the specified label key and value to this {@link ManifestObject}'s metadata labels. Will
     * ensure a label map exists upon execution.
     *
     * @param key The key of the label to be added.
     * @param value The value of the label to be added.
     */
    public void addLabel(String key, String value) {
      Map<String, String> labels = getOrCreateLabels();
      // Add the specified label ensuring no duplicate values.
      Set<String> labelValues =
          labels.get(key) != null
              ? new HashSet<>(Arrays.asList(labels.get(key).split(",")))
              : new HashSet<>();
      labelValues.add(value);
      labels.put(key, String.join(",", labelValues));
    }

    /** @return The description of the object in {ApiVersion}/{Kind}: {Name} */
    public String describe() {
      return String.format("%s/%s: %s", getApiVersion(), getKind(), getName().orElse(""));
    }

    /** @return The metadata map for this {@link ManifestObject}. */
    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> getMetadata() {
      return Optional.ofNullable((Map<String, Object>) source.get("metadata"));
    }

    /**
     * Returns this {@link ManifestObject}'s metadata, ensuring its existence.
     *
     * @return The metadata map for this {@link ManifestObject}.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateMetadata() {
      if (source.get("metadata") == null) {
        source.put("metadata", new LinkedHashMap<String, Object>());
      }

      return (Map<String, Object>) source.get("metadata");
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
    Iterable<Object> iter = yaml.loadAll(new InputStreamReader(mis, DEFAULT_ENCODING));
    iter.forEach((o) -> objects.add(new ManifestObject((Map<String, Object>) o, filePath)));
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
    final Set<String> includedKindsLowerCase =
        includedKinds.stream().map(String::toLowerCase).collect(Collectors.toSet());
    return getObjectManifests().stream()
        .filter((manifest) -> includedKindsLowerCase.contains(manifest.getKind().toLowerCase()))
        .collect(Collectors.toList());
  }

  /**
   * Writes the contents of this {@link Manifests}'s objects back to their corresponding files.
   *
   * @throws InterruptedException If an error occurred while dumping to YAML.
   * @throws IOException If an error occurred while writing the file contents.
   */
  public void write() throws InterruptedException, IOException {
    Map<FilePath, List<ManifestObject>> fileToManifestListMap = new LinkedHashMap<>();
    for (ManifestObject manifest : getObjectManifests()) {
      List<ManifestObject> manifestList =
          fileToManifestListMap.getOrDefault(manifest.getFile(), new ArrayList<ManifestObject>());
      manifestList.add(manifest);
      fileToManifestListMap.put(manifest.getFile(), manifestList);
    }

    for (Map.Entry<FilePath, List<ManifestObject>> entry : fileToManifestListMap.entrySet()) {
      FilePath file = entry.getKey();
      List<ManifestObject> manifestObjects = entry.getValue();
      file.write(
          yaml.dumpAll(manifestObjects.stream().map(m -> m.getSource()).iterator()),
          DEFAULT_ENCODING);
    }
  }
}
