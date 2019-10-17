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
## Setup
<!--
TODO(stephenashank): Remove this once the credentials can be preloaded through the helm chart.
This depends on refactoring the Google Oauth Plugin.
-->
1. Make sure you have another cluster and service account set up for testing deployments as described in the GKE Plugin [usage documentation](Home.md#usage).

1. Ensure a GCP Service Account has been created for testing. If needed, follow the instruction in the [Automation Option](Home.md#automation-option) section.

1. Bootstrap the necessary cluster resources/roles for running integration tests:
```bash
export TEST_CLUSTER=<YOUR_TEST_CLUSTER_NAME>
export CLUSTER_ZONE=<YOUR_TEST_CLUSTER_ZONE>
export PROJECT_ID=<YOUR_TEST_GCP_PROJECT_ID>
export TEST_GCP_SA_ID=<YOUR_GCP_SA_ID>
gcloud config set project $PROJECT_ID
gcloud container clusters get-credentials $TEST_CLUSTER --zone $CLUSTER_ZONE
sed s/YOUR_GCP_SA_ID/$TEST_GCP_SA_ID/ docs/rbac/cluster-it-setup.yaml > /tmp/cluster-it-setup.yaml
kubectl apply -f /tmp/cluster-it-setup.yaml
rm /tmp/cluster-it-setup.yaml
```

## Testing the plugin on Jenkins
1. Follow the instructions at [Source Build Installation](SourceBuildInstallation.md) to upload the
plugin build that you will be testing.

1. From the main Jenkins page click **New Item**.

1. Enter a name and choose **Freestyle project**.

1. Click **OK**.

1. Under **Source Code Management**:
  * Select `Git`.

  * For **Repository URL** enter `https://github.com/jenkinsci/google-kubernetes-engine-plugin.git`

  * For the **Branch Specifier** enter `*/develop`

  * Click **Save**.

1. Follow the instructions at [GKE Build Step Configuration](Home.md#google-kubernetes-engine-build-step-configuration)
to test. Enter [`docs/resources/manifest.yaml`](resources/manifest.yaml) in the Kubernetes Manifests field.
