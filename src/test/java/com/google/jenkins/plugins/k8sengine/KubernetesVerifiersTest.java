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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Resources;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;

/** Tests {@link com.google.jenkins.plugins.k8sengine.KubernetesVerifiers} */
public class KubernetesVerifiersTest {
  private static final String VERIFIABLE_DEPLOYMENT_OUTPUT = "verifiableDeploymentOutput.json";
  private static final String UNVERIFIABLE_DEPLOYMENT_OUTPUT = "unverifiableDeploymentOutput.json";

  @Test
  public void testGoodDeploymentVerified() throws Exception {
    Object goodDeploymentOutput = readTestFile(VERIFIABLE_DEPLOYMENT_OUTPUT);
    KubectlWrapper kubectl = Mockito.mock(KubectlWrapper.class);
    Mockito.when(kubectl.getObject("deployment", "nginx-deployment"))
        .thenReturn(goodDeploymentOutput);

    Manifests.ManifestObject goodDeployment = Mockito.mock(Manifests.ManifestObject.class);
    Mockito.when(goodDeployment.getKind()).thenReturn("deployment");
    Mockito.when(goodDeployment.getName()).thenReturn(Optional.<String>of("nginx-deployment"));
    Mockito.when(goodDeployment.getApiVersion()).thenReturn("apps/v1");
    KubernetesVerifiers.VerificationResult result =
        KubernetesVerifiers.verify(kubectl, goodDeployment);
    assertTrue(result.isVerified());
    Integer availableReplicas = JsonPath.read(goodDeploymentOutput, "status.availableReplicas");
    Integer updatedReplicas = JsonPath.read(goodDeploymentOutput, "status.updatedReplicas");
    Integer desiredReplicas = JsonPath.read(goodDeploymentOutput, "spec.replicas");
    String shouldBeInLog =
        String.format(
            "AvailableReplicas = %s, UpdatedReplicas = %s, DesiredReplicas = %s",
            availableReplicas, updatedReplicas, desiredReplicas);
    String verificationLog = result.toString();
    assertTrue(verificationLog.contains(shouldBeInLog));
  }

  @Test
  public void testBadDeploymentNotVerified() throws Exception {
    Object badDeploymentOutput = readTestFile(UNVERIFIABLE_DEPLOYMENT_OUTPUT);
    KubectlWrapper kubectl = Mockito.mock(KubectlWrapper.class);
    Mockito.when(kubectl.getObject("deployment", "nginx-deployment-unverifiable"))
        .thenReturn(badDeploymentOutput);

    Manifests.ManifestObject badDeployment = Mockito.mock(Manifests.ManifestObject.class);
    Mockito.when(badDeployment.getKind()).thenReturn("deployment");
    Mockito.when(badDeployment.getName())
        .thenReturn(Optional.<String>of("nginx-deployment-unverifiable"));
    Mockito.when(badDeployment.getApiVersion()).thenReturn("apps/v1");
    KubernetesVerifiers.VerificationResult result =
        KubernetesVerifiers.verify(kubectl, badDeployment);
    assertFalse(result.isVerified());

    Integer desiredReplicas = JsonPath.read(badDeploymentOutput, "spec.replicas");
    Integer updatedReplicas = JsonPath.read(badDeploymentOutput, "status.updatedReplicas");
    Integer availableReplicas = 0;
    String shouldBeInLog =
        String.format(
            "AvailableReplicas = %s, UpdatedReplicas = %s, DesiredReplicas = %s",
            availableReplicas, updatedReplicas, desiredReplicas);
    String verificationLog = result.toString();
    assertTrue(verificationLog.contains(shouldBeInLog));
  }

  private static Object readTestFile(String name) throws IOException {
    String jsonString = Resources.toString(Resources.getResource(name), StandardCharsets.UTF_8);
    return Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
  }
}
