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
# Installing GKE plugin on Jenkins running in Kubernetes

This document provides guidance on how to configure Jenkins running within
Kubernetes in order to meet the plugin's pre-requisites, namely that the
`kubectl` binary be installed on the agent container.

## Installing on an existing Jenkins installation with Kubernetes Cloud

This section assumes that the user has an existing Jenkins installation configured to
use a Kubernetes cluster as the cloud via the
[kubernetes-plugin](https://github.com/jenkinsci/kubernetes-plugin).
Furthermore, it's assumed that the GKE plugin has been installed either through
the [Plugin Manager](https://jenkins.io/doc/book/managing/plugins/) or
using the [SourceBuildInstallation](SourceBuildInstallation.md) guide. This
content provides guidance on changing the existing Kubernetes cloud
configuration to be compatible with the GKE plugin.
*   Within your Jenkins installation's web console, navigate to "Manage
    Jenkins" > "Configure System" > "Cloud" > "Images" > "Container"

*   Modify the "Docker image" field to: `gcr.io/jenkins-gke-plugin/jenkinsagent`

*   Ensure the "Name" field is: `jnlp`

*   For more responsive build executions, we suggest setting the "Time in
    minutes to retain agent when idle" field to atleast: `5`

## Installing using the Jenkins Helm chart

The [Jenkins](https://github.com/helm/charts/tree/master/stable/jenkins) chart
enables users to install Jenkins into a Kubernetes cluster using
[Helm](https://github.com/helm/helm). Included in the chart's configuration
values is the option to specify a set of Jenkins plugins to be installed by
default. This section provides guidance on utilizing this chart to install the
GKE plugin.

*   If installing into a cluster running within GKE, we suggest following Google
    Cloud Solutions'
    [tutorial](https://cloud.google.com/solutions/jenkins-on-kubernetes-engine-tutorial)
    for setting up Jenkins on GKE. **Note**: The chart version specified in the
    tutorial is out of date, we suggest users find a version which best suites
    their use-case.

*   Modify the `values.yaml` to be used when running `helm install --values` to
    include the following configuration values:
    ```yaml 
        Agent:
            Image: gcr.io/jenkins-gke-plugin/jenkinsagent
            ImageTag: latest
        Master:
            InstallPlugins:
                - google-kubernetes-engine:0.2.0
    ```

See the [Dockerfile](../jenkinsagent/Dockerfile) for the
`gcr.io/jenkins-gke-plugin/jenkinsagent` image for more information.

<!--
TODO(stephenashank): Add another step for the CasC config when we are able to pre-load credentials
through the helm chart after refactoring the Google Oauth Plugin.
-->
