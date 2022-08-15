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

import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.retry.Backoff;
import reactor.retry.Repeat;

/**
 * This a task that verifies the given {@link Manifests.ManifestObject} was applied to the
 * Kubernetes cluster.
 */
public class VerificationTask {
  private static final int VERIFICATION_DELAY = 5;

  private KubectlWrapper kubectl;
  private Manifests.ManifestObject manifestObject;
  private KubernetesVerifiers.VerificationResult currentResult;
  private PrintStream consoleLogger; // Jenkins console

  private static Logger LOGGER = Logger.getLogger(VerificationTask.class.getName());

  /**
   * Constructs new {@link VerificationTask}.
   *
   * @param kubectl The KubectlWrapper for issuing kubectl commands.
   * @param manifestObject The wrapper for the Kubernetes object to verify.
   * @param consoleLogger The console output {@link PrintStream}.
   */
  private VerificationTask(
      KubectlWrapper kubectl, Manifests.ManifestObject manifestObject, PrintStream consoleLogger) {
    this.kubectl = kubectl;
    this.manifestObject = manifestObject;
    this.consoleLogger = consoleLogger;
  }

  /**
   * Check whether there is a result and whether it indicates that verification succeeded.
   *
   * @return If this task has successfully verified the {@link Manifests.ManifestObject} was applied
   *     to the Kubernetes cluster.
   */
  private boolean isVerified() {
    return currentResult != null && currentResult.isVerified();
  }

  /** @return The {@link KubernetesVerifiers.VerificationResult}. */
  public KubernetesVerifiers.VerificationResult getVerificationResult() {
    return currentResult;
  }

  /**
   * Verifies its {@link Manifests.ManifestObject} using the kubectl cli.
   *
   * @return Self-reference after performing verify.
   */
  private VerificationTask verify() {
    consoleLogger.println(
        Messages.KubernetesEngineBuilder_VerifyingLogPrefix(manifestObject.describe()));
    currentResult = KubernetesVerifiers.verify(kubectl, manifestObject);
    if (isVerified()) {
      consoleLogger.println(currentResult.toString());
    }

    return this;
  }

  /**
   * The caller's entrypoint for verifying that a list of {@link Manifests.ManifestObject}'s were
   * applied to the Kubernetes cluster.
   *
   * @param kubectl KubectlWrapper object for issuing commands to Kubernetes cluster.
   * @param manifestObjects List of {@link Manifests.ManifestObject}'s to verify.
   * @param consoleLogger {@link PrintStream} for outputting results (intended to be user facing).
   * @param timeoutInMinutes Stop retrying verification after this many minutes.
   * @return If the {@link Manifests.ManifestObject}'s were successfully verified.
   */
  public static boolean verifyObjects(
      @Nonnull KubectlWrapper kubectl,
      @Nonnull List<Manifests.ManifestObject> manifestObjects,
      @Nonnull PrintStream consoleLogger,
      int timeoutInMinutes) {
    List<VerificationTask> verificationTasks =
        manifestObjects.stream()
            .map((manifestObject) -> new VerificationTask(kubectl, manifestObject, consoleLogger))
            .collect(Collectors.toList());

    Repeat.onlyIf(
            (ctx) ->
                !verificationTasks.stream()
                    .map((task) -> task.isVerified()) // only repeat if we aren't all done
                    .reduce(true, (acc, done) -> acc && done))
        .backoff(Backoff.fixed(Duration.ofSeconds(VERIFICATION_DELAY)))
        .timeout(Duration.ofMinutes(timeoutInMinutes))
        // apply this repeat to the list of  VerificationTask's
        .apply((Publisher<VerificationTask>) Flux.fromIterable(verificationTasks))
        .filter(
            (task) -> !task.isVerified()) // Don't try to verify objects that are already verified
        .map((task) -> task.verify())
        .subscribeOn(Schedulers.elastic()) // parallelize the verification
        .doOnError(
            (error) -> {
              LOGGER.log(Level.SEVERE, "Unexpected error in verifyObjects()", error);
              error.printStackTrace(consoleLogger); // report error
            })
        .blockLast(); // wait for all this to finish

    List<KubernetesVerifiers.VerificationResult> finalResults =
        verificationTasks.stream()
            .map((task) -> task.getVerificationResult())
            .collect(Collectors.toList());

    List<KubernetesVerifiers.VerificationResult> errorResults =
        finalResults.stream().filter((result) -> !result.isVerified()).collect(Collectors.toList());

    errorResults.forEach((it) -> consoleLogger.println(it.toString()));
    LOGGER.info(String.format("%d error results", errorResults.size()));

    return errorResults.size() == 0;
  }
}
