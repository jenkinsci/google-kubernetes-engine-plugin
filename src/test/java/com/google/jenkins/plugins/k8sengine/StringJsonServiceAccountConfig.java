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

import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.PemReader;
import com.google.common.base.Strings;
import com.google.jenkins.plugins.credentials.oauth.JsonKey;
import com.google.jenkins.plugins.credentials.oauth.KeyUtils;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/** Provides a means for read/writing JSON service account config files for integration tests. */
public class StringJsonServiceAccountConfig extends ServiceAccountConfig {
  private static final long serialVersionUID = 6818111194672325387L;
  private static final Logger LOGGER =
      Logger.getLogger(StringJsonServiceAccountConfig.class.getName());
  private String jsonKeyFile;
  private transient JsonKey jsonKey;

  @DataBoundConstructor
  public StringJsonServiceAccountConfig(String jsonKeyString) {
    if (jsonKeyString != null) {
      InputStream stream = new ByteArrayInputStream(jsonKeyString.getBytes(StandardCharsets.UTF_8));
      try {
        JsonKey jsonKey = JsonKey.load(new JacksonFactory(), stream);
        if (jsonKey.getClientEmail() != null && jsonKey.getPrivateKey() != null) {
          try {
            this.jsonKeyFile = writeJsonKeyToFile(jsonKey);
          } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write json key to file", e);
          }
        }
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to read json key from file", e);
      }
    }
  }

  private String writeJsonKeyToFile(JsonKey jsonKey) throws IOException {
    File jsonKeyFile = KeyUtils.createKeyFile("key", ".json");
    KeyUtils.writeKeyToFileEncoded(jsonKey.toPrettyString(), jsonKeyFile);
    return jsonKeyFile.getAbsolutePath();
  }

  @Override
  public com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig.DescriptorImpl
      getDescriptor() {
    return (com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig.DescriptorImpl)
        Jenkins.get()
            .getDescriptorOrDie(
                com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig.class);
  }

  /** @returns The path to the created JSON key file. */
  public String getJsonKeyFile() {
    return jsonKeyFile;
  }

  @Override
  public String getAccountId() {
    JsonKey jsonKey = getJsonKey();
    if (jsonKey != null) {
      return jsonKey.getClientEmail();
    }
    return null;
  }

  @Override
  public PrivateKey getPrivateKey() {
    JsonKey jsonKey = getJsonKey();
    if (jsonKey != null) {
      String privateKey = jsonKey.getPrivateKey();
      if (privateKey != null && !privateKey.isEmpty()) {
        PemReader pemReader = new PemReader(new StringReader(privateKey));
        try {
          PemReader.Section section = pemReader.readNextSection();
          PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(section.getBase64DecodedBytes());
          return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
          LOGGER.log(Level.SEVERE, "Failed to read private key", e);
        }
      }
    }
    return null;
  }

  private JsonKey getJsonKey() {
    if (jsonKey != null || Strings.isNullOrEmpty(jsonKeyFile)) {
      return jsonKey;
    }

    try (FileInputStream keyFileIs = new FileInputStream(jsonKeyFile)) {
      jsonKey = JsonKey.load(new JacksonFactory(), keyFileIs);
      File jsonKeyFileObject = new File(jsonKeyFile);
      KeyUtils.updatePermissions(jsonKeyFileObject);
      KeyUtils.writeKeyToFileEncoded(jsonKey.toPrettyString(), jsonKeyFileObject);
    } catch (IOException ignored) {
      LOGGER.log(Level.WARNING, "Failed to update permissions", ignored);
    }

    return jsonKey;
  }
}
