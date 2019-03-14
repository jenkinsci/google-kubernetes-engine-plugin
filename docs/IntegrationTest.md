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
# Integration Tests

## Jenkins on GKE with helm
Follow Google Cloud Solutions'
[tutorial](https://cloud.google.com/solutions/jenkins-on-kubernetes-engine-tutorial) for setting up 
Jenkins on GKE.

<!--
TODO(stephenashank): Add another step for the CasC config when we are able to pre-load credentials
through the helm chart after refactoring the Google Oauth Plugin.
-->
Before running `helm install`, add the following modification to the file `jenkins/values.yaml`:

* Directly below the line which reads `Agent:`, add the additional lines shown here:
    ```yaml
    Agent:
        Image: gcr.io/jenkins-gke-plugin/jenkinsagent
        ImageTag: latest
    ```

See the [Dockerfile](../jenkinsagent/Dockerfile) for the `gcr.io/jenkins-gke-plugin/jenkinsagent`
image.

When running `helm install` remove the flag `--version` and its argument.

After logging in to the Jenkins instance, continue to the next section.

## Testing the plugin on Jenkins
1. Make sure you have another cluster and service account set up for testing deployments as
described in the GKE Plugin [usage documentation](Home.md#usage). 

<!--
TODO(stephenashank): Remove this once the credentials can be preloaded through the helm chart.
This depends on refactoring the Google Oauth Plugin.
-->
1. Repeat steps 3-8 for the [IAM Credentials](Home.md#iam-credentials) section on this new Jenkins
instance.

1. Follow the instructions at [Source Build Installation](SourceBuildInstallation.md) to upload the
plugin build that you will be testing.

1. From the main jenkins page click **New Item**, then enter a name and choose
**Freestyle project**.

1. Under **Source Code Management**, select Git and enter this repository.:
https://github.com/jenkinsci/google-kubernetes-engine-plugin.git

1. Enter Branch Specifier as develop

1. Follow the instructions at
[GKE Build Step Configuration](Home.md#google-kubernetes-engine-build-step-configuration) to test.
Enter [`docs/resources/manifest.yaml`](resources/manifest.yaml) in the Kubernetes Manifests field.
