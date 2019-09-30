<!--
 Copyright 2019 Google LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 compliance with the License. You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under the License
 is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied. See the License for the specific language governing permissions and limitations under the
 License.
-->

# GKE Jenkins Configuration

This contains information related to the Jenkins configuration for the GKE plugin
repository.

# Maven/Kubectl Container Image

This project depends on the container [gcr.io/jenkins-gke-plugin/maven-kubectl](maven-kubectl/Dockerfile)
for executing integration tests. See the [GCR Getting Started](https://cloud.google.com/container-registry/docs/quickstart)
guide for information on building and publishing a container image to GCR.
