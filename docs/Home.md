# Google Kubernetes Engine Plugin Wiki

The [Google Kubernetes Engine](https://cloud.google.com/kubernetes-engine/) (GKE) Plugin allows you to publish deployments built within Jenkins to your Kubernetes clusters running within GKE.

_Plugin Information_

View Google Kubernetes Engine [on the plugin site](https://plugins.jenkins.io/google-kubernetes-engine) for more information.

## Usage

### IAM Credentials

1. Create a service account using the Google Cloud SDK.

        gcloud iam service-accounts create jenkins-gke

1. Add the KubernetesEngineAdmin and serviceAccountUser roles to the service account:

        export PROJECT=$(gcloud info --format='value(config.project)')
        export SA_EMAIL=$(gcloud iam service-accounts list --filter="name:jenkins-gke" --format='value(email)')
        gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/iam.serviceAccountUser $PROJECT
        gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/container.clusterAdmin $PROJECT
        gcloud projects add-iam-policy-binding --member serviceAccount:$SA_EMAIL --role roles/container.admin $PROJECT

1. Download a JSON Service Account key for your newly created service account. Take note of where the file was created, you will upload it to Jenkins in a subsequent step.

        gcloud iam service-accounts create jenkins-gke

1. In Jenkins, click the Credentials button on the left side of the screen. Then click System.

1. Click Global credentials then **Add credentials ** on the left.

1. In the Kind dropdown, select Google Service Account from private key.

1. Enter your project name then select your JSON key that was created in the preceding steps.

1. Click OK.

### Enable Cloud Resource Manager API

1. Go to the [Cloud Resource Manager API](https://console.developers.google.com/apis/api/cloudresourcemanager.googleapis.com/overview) overview in the GCP console.

1. Make sure that your current project is the same used above.

1. Click Enable. This will redirect you to the API & Services dashboard for Cloud Resource Manager.

1. Click credentials to ensure that  your jenkins-gke service account credentials are compatible.

### Configure GKE Cluster

1. Create GKE cluster (if not already exists).

        export CLUSTER=my-jenkins-cluster
        export ZONE=us-central1-c
        gcloud container clusters create --zone $ZONE $CLUSTER

1. Enable legacy authorization on the cluster.

        gcloud container clusters update --enable-legacy-authorization --zone $ZONE $CLUSTER

1. Get credentials for the cluster.

        gcloud container clusters get-credentials --zone $ZONE $CLUSTER

### Configure Kubernetes Cluster Permissions

1. Add the cluster-admin role to the service account group within your Kubernetes cluster:

        kubectl create clusterrolebinding serviceaccounts-cluster-admin --group=system:serviceaccounts --clusterrole=cluster-admin

### Google Kubernetes Engine Publisher configuration

Each GKE Publisher configuration can point to a different GKE cluster. Follow the steps below to create one.

1. Go to Jenkins home, and select the project to be published to GKE.
1. Click the "Configure" button on the left nav-bar.
1. At the bottom of the page there will be a button labeled "Add post-build action" , click the button then click Google Kubernetes Engine.
1. Enter the Project ID housing the GKE cluster to be published to.
1. Enter the Cluster to be published to.
1. Enter the file path of the Kubernetes [manifest](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) within your project to be used for deployment.
1. In the Service Account Credentials dropdown, select the credentials that you uploaded earlier.

### Jenkins environment configuration

The GKE Jenkins plugin requires the [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) binary to be installed within the Jenkins environment.

## Feature requests and bug reports

Please file feature requests and bug reports under the google-kubernetes-engine-plugin component of the [Jenkins CI JIRA](https://issues.jenkins-ci.org).

## Contributing
See [CONTRIBUTING.md](https://github.com/jenkinsci/google-kubernetes-engine-plugin/blob/master/CONTRIBUTING.md)

## License
See [LICENSE.md](https://github.com/jenkinsci/google-kubernetes-engine-plugin/blob/master/LICENSE)

