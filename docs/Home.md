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
# Google Kubernetes Engine Plugin Documentation

The [Google Kubernetes Engine](https://cloud.google.com/kubernetes-engine/) (GKE) Plugin allows you
to publish deployments built within Jenkins to your Kubernetes clusters running within GKE.

## Prerequisites

* Minimum Jenkins version: 2.164.2

* Jenkins plugin dependencies:
  
  **NOTE**: Unless otherwise specified, pre-installation of these plugins aren't required. Just
    note that if a conflicting version is present a class-loading error could occur.
    
  * google-oauth-plugin: 0.7 (pre-installation required)
  * workflow-step-api: 2.19
  * pipeline-model-definition: 1.3.8 (pre-installation required for Pipeline DSL support)
  * git: 3.9.3
  * junit: 1.3
  * structs: 1.17
  * credentials: 2.1.16

## Usage

### Enable Required APIs

1. Export project:
    ```bash
    export PROJECT=$(gcloud info --format='value(config.project)')
    ```
2. Enable required GCP service APIs:
    ```bash
    gcloud services enable --project $PROJECT compute.googleapis.com container.googleapis.com servicemanagement.googleapis.com cloudresourcemanager.googleapis.com
    ```

### IAM Credentials

1. Create a service account using the Google Cloud SDK:
    ```bash
    export SA=jenkins-gke
    gcloud iam service-accounts create $SA
    ```
1. Add required service account roles:
    ```bash
    export SA_EMAIL=$SA@$PROJECT.iam.gserviceaccount.com
    gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/iam.serviceAccountUser $PROJECT
    gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/container.developer $PROJECT
	gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/container.viewer $PROJECT
    gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/compute.networkViewer $PROJECT
    ```
1. Download a JSON Service Account key for your newly created service account. Take note of where
the file was created, you will upload it to Jenkins in a subsequent step:
    ```bash
    gcloud iam service-accounts keys create ~/jenkins-gke-key.json --iam-account $SA_EMAIL
    ```
1. In Jenkins, click the Credentials button on the left side of the screen. Then click System.
1. Click Global credentials then **Add credentials** on the left.
1. In the Kind dropdown, select **Google Service Account from private key**.
1. Enter your project name then select your JSON key that was created in the preceding steps.
1. Click OK.

### Configure GKE Cluster

1. Create a GKE cluster*:
    ```bash
    export CLUSTER=my-jenkins-cluster
    export ZONE=us-central1-c
    gcloud container clusters create --issue-client-certificate --enable-legacy-authorizaton --zone $ZONE $CLUSTER
    ```
1. If using an existing cluster, enable legacy authorization on the cluster:
    ```bash
    gcloud container clusters update --enable-legacy-authorization --zone $ZONE $CLUSTER
    ```
1. Get credentials for the cluster:
    ```bash
    gcloud container clusters get-credentials --zone $ZONE $CLUSTER
    ```

\***Note**: You can use an existing cluster but it must have been created with Client Certificates
enabled. If creating your cluster through Cloud Console, click **Advanced Options** and then
under **Security** make sure that "Issue a client certificate" is checked. This is a ***permanent***
property of the cluster. The "Your first cluster" template, for example, does not have this checked
by default so it won't be possible to use such a cluster. Also check "Enable legacy authorization",
which is always disabled by default. From Cloud Console you can also select an existing cluster,
click **EDIT** and change the **Legacy Authorization** dropdown to "Enabled".

### Configure Kubernetes Cluster Permissions

1. Add the cluster-admin role to the service account group within your Kubernetes cluster:
    ```bash
    kubectl create clusterrolebinding serviceaccounts-cluster-admin --group=system:serviceaccounts --clusterrole=cluster-admin
    ```

### Google Kubernetes Engine Build Step Configuration

Each GKE Build Step configuration can point to a different GKE cluster. Follow the steps below to
create one.

#### GKE Build Step Parameters

The GKE Build Step has the following parameters:

1. credentialsId(string): The ID of the credentials that you uploaded earlier.
1. projectId(string): The Project ID housing the GKE cluster to be published to.
1. zone(string): The Zone housing the GKE cluster to be published to.
1. clusterName(string): The name of the Cluster to be published to.
1. manifestPattern(string): The file pattern of the Kubernetes manifest to be deployed.
1. verifyDeployments(boolean): [Optional] Whether the plugin will verify deployments.

#### Jenkins Web UI

1. Go to Jenkins home, and select the project to be published to GKE.
1. Click the "Configure" button on the left nav-bar.
1. At the bottom of the page there will be a button labeled "Add build step", click the button then
click Google Kubernetes Engine.
1. In the Service Account Credentials dropdown, select the credentials that you uploaded earlier.
1. Select the Project ID housing the GKE cluster to be published to.
1. Select the Zone housing the GKE cluster to be published to.
1. Select the Cluster to be published to.
1. Enter the file path of the Kubernetes [manifest](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) within your project to be used for deployment.

#### Jenkins Declarative Pipeline

1. Create a file named "Jenkinsfile" in the root of your project.
1. Within your Jenkinsfile add a step which invokes the GKE plugin's build step class:
"KubernetesEngineBuilder". See the example code below:

```groovy
pipeline {
    agent any
    environment {
        PROJECT_ID = '<YOUR_PROJECT_ID>'
        CLUSTER_NAME = '<YOUR_CLUSTER_NAME>'
        ZONE = '<YOUR_CLUSTER_ZONE>'
        CREDENTIALS_ID = '<YOUR_CREDENTIAS_ID>'
    }
    stages {
        stage('Deploy to GKE') {
            steps{
                step([$class: 'KubernetesEngineBuilder', projectId: env.PROJECT_ID, clusterName: env.CLUSTER_NAME, zone: env.ZONE, manifestPattern: 'manifest.yaml', credentialsId: env.CREDENTIALS_ID, verifyDeployments: true])
            }
        }
    }
}
```

### Jenkins Environment Configuration

<!--- TODO(stephenshank): Link to an image that adds kubectl to the existing jenkins agent image: https://hub.docker.com/r/jenkinsci/jnlp-slave/ --->

The GKE Jenkins plugin requires the [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
binary to be installed within the Jenkins agent environment.
