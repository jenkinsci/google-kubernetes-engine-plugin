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
1. Create the following [yaml file](rbac/IAMrole.yaml) for a custom IAM role.
1. Run the following command:
    ```bash
    gcloud iam roles create gke_deployer --project $PROJECT --file \
    PATH_TO_YOUR_IAM_ROLE_YAML
	```
1. Grant the IAM role to your GCP service account:
    ```bash
    gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role projects/$PROJECT/roles/gke_deployer $PROJECT
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
    export LOCATION=us-central1-c
    gcloud container clusters create --zone $LOCATION $CLUSTER
    ```
1. Get credentials for the cluster:
    ```bash
    gcloud container clusters get-credentials --zone $LOCATION $CLUSTER
    ```

### Configure Kubernetes Cluster Permissions

Your GCP service account will have limited IAM permissions. Use RBAC in kubernetes to configure permissions suited to your use case.

Grant your GCP login account cluster-admin permissions before creating the following roles/role bindings:
   ```bash
   kubectl create clusterrolebinding gcp-cluster-admin-binding --clusterrole=cluster-admin --user=YOUR_GCP_ACCOUNT_EMAIL
   ```

#### Less Restrictive Permissions

The following permissions will grant you full read and write permissions to your cluster.

1. Add the cluster-admin role to the service account associated with your Kubernetes cluster:
    ```bash
    kubectl create clusterrolebinding cluster-admin-binding \
    --clusterrole cluster-admin \
    --user jenkins-gke@YOUR-PROJECT.iam.gserviceaccount.com
    ```

#### More Restrictive Permissions
The following permissions will grant you enough permissions to deploy to your cluster.
1. Create the [ClusterRole yaml](rbac/robot-deployer.yaml) file.
1. Create the ClusterRole in kubernetes:
    ```bash
    kubectl create -f PATH_TO_YOUR_ROLE_YAML
    ```
1. Create the [RoleBinding yaml](rbac/restricted-bindings.yaml) file.
1. Create the RoleBinding in kubernetes:
    ```bash
    kubectl create -f PATH_TO_YOUR_ROLE_BINDING_YAML
    ```

##### References:
* [Google Container Engine RBAC docs](https://cloud.google.com/kubernetes-engine/docs/how-to/role-based-access-control)
* [Configuring RBAC for GKE deployment](https://codeascraft.com/2018/06/05/deploying-to-google-kubernetes-engine/)

### Google Kubernetes Engine Build Step Configuration

Each GKE Build Step configuration can point to a different GKE cluster. Follow the steps below to
create one.

#### GKE Build Step Parameters

The GKE Build Step has the following parameters:

1. credentialsId(string): The ID of the credentials that you uploaded earlier.
1. projectId(string): The Project ID housing the GKE cluster to be published to.
1. zone(string): [**Deprecated**] The Zone housing the GKE cluster to be published to.
1. location(string): The Zone or Region housing the GKE cluster to be published to.
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
        LOCATION = '<YOUR_CLUSTER_LOCATION>'
        CREDENTIALS_ID = '<YOUR_CREDENTIAS_ID>'
    }
    stages {
        stage('Deploy to GKE') {
            steps{
                step([$class: 'KubernetesEngineBuilder', projectId: env.PROJECT_ID, clusterName: env.CLUSTER_NAME, location: env.LOCATION, manifestPattern: 'manifest.yaml', credentialsId: env.CREDENTIALS_ID, verifyDeployments: true])
            }
        }
    }
}
```

### Jenkins Environment Configuration

<!--- TODO(stephenshank): Link to an image that adds kubectl to the existing jenkins agent image: https://hub.docker.com/r/jenkinsci/jnlp-slave/ --->

The GKE Jenkins plugin requires the [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
binary to be installed within the Jenkins agent environment.
