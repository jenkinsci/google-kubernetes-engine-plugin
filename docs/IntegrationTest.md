# Integration Tests

## Jenkins on GKE with helm

[Start Cloud Shell](https://cloud.google.com/shell/docs/quickstart#start_cloud_shell) from the
GCP console. All commands listed below are to be run from the Cloud Shell CLI

### Google Cloud Platform Prerequisites
1. If you are performing these steps in a different CLI session than the one you used for the
[usage](Home.md#usage) tutorial then make sure the `$PROJECT` environment variable is exported:
    ```bash
    export PROJECT=$(gcloud info --format='value(config.project)')
    ```

1. Make sure that `kubectl` is installed:
    ```bash
    kubectl version
    ```

    You can install through the [Google Cloud SDK](
    https://kubernetes.io/docs/tasks/tools/install-kubectl/#download-as-part-of-the-google-cloud-sdk)
    (which should also be installed on Cloud Shell):    
    ```bash
    gcloud components install kubectl
    ```

    If not, choose one of the other installation methods
    [here](https://kubernetes.io/docs/tasks/tools/install-kubectl/).

1. Create a GKE cluster similar to step 1 of [Configure GKE Cluster](Home.md#configure-gke-cluster):
    ```bash
    export CLUSTER=jenkins-master
    export ZONE=us-central1-c
    gcloud container clusters create --zone $ZONE $CLUSTER
    ```

1. Create a [domain](https://domains.google/) for hosting your jenkins instance. Export that now:
    ```bash
    export DOMAIN=example.com
    ```

1. Create a global [static external IP address](
 https://cloud.google.com/compute/docs/ip-addresses/reserve-static-external-ip-address#reserve_new_static):
    ```bash
    export ADDRESS_NAME=jenkins-address
    gcloud compute addresses create $ADDRESS_NAME --global
    gcloud compute addresses describe $ADDRESS_NAME --global
    ```

    The A record for the domain you created should point to the IP address that the describe command
    outputs.

1. Create a Google-managed SSL certificate resource as described [here](
https://cloud.google.com/load-balancing/docs/ssl-certificates#create-managed-ssl-cert-resource):
    ```bash
    export CERTIFICATE_NAME=jenkins-host-certificate
    gcloud beta compute ssl-certificates create $CERTIFICATE_NAME --domains $DOMAIN
    ```

    Don't worry about any of the other steps, as the helm installation process will take care of
    that.
    
1. If you haven't already, clone the google-kubernetes-engine-plugin repository on your Cloud Shell
instance. This contains some of the resources needed.
    ```bash
    git clone https://github.com/jenkinsci/google-kubernetes-engine-plugin.git
    ```

    <!--TODO(stephenshank): Create publically available docker image -->
1.  Create a Jenkins agent image, using the [Dockerfile](resources/helm/Dockerfile) from the
documentation resources:
    ```bash
    cp docs/resources/helm/Dockerfile Dockerfile
    docker build -t us.gcr.io/$PROJECT/jenkinsagent
    docker push us.gcr.io/$PROJECT/jenkinsagent
    ```

    More detailed instructions are available
    [here](https://cloud.google.com/container-registry/docs/pushing-and-pulling).

### Helm Setup

1. Choose a helm version and export it as `$HELM_VERSION`:
    ```$bash
    export HELM_VERSION=2.13.0
    ```

1. Run the following commands from the Cloud Shell CLI to install the
[helm](https://helm.sh/docs/using_helm/#from-the-binary-releases) client:
    ```bash
    wget "https://storage.googleapis.com/kubernetes-helm/helm-v$HELM_VERSION-linux-amd64.tar.gz"
    tar -zxvf helm-v$HELM_VERSION-linux-amd64.tar.gz
    sudo cp linux-amd64/helm /usr/local/bin/helm 
    ```

   If you've previously followed these steps and are running Cloud Shell from the same GCP account
   you can simply run:
    ```bash
    sudo cp linux-amd64/helm /usr/local/bin/helm
    ```

    Note: Your home directory for Cloud Shell will persist between sessions, but `/usr/local/bin`
    will not. That is why you should use `cp` to install rather than `mv` if you want to avoid
    downloading it again.

1. Create an RBAC service account called `tiller` as described
[here](https://helm.sh/docs/using_helm/#example-service-account-with-cluster-admin-role).

1. Install [tiller](https://helm.sh/docs/using_helm/#installing-tiller) on GKE:
    ```bash
    helm init --service-account tiller
    ```

1. Move the values.yaml file to your working directory and replace the placeholder values with the
environment variables that you have configured.
    ```bash
    cp google-kubernetes-engine-plugin/docs/resources/helm/values.yaml values.yaml
    sed -i s/\$DOMAIN/$DOMAIN/g values.yaml
    sed -i s/\$ADDRESS_NAME/$ADDRESS_NAME/g values.yaml
    sed -i s/\$CERTIFICATE_NAME/$CERTIFICATE_NAME/g values.yaml
    sed -i s/\$PROJECT/$PROJECT/g values.yaml
    ```

1. Install the jenkins helm chart using your custom config:
    ```bash
    export JENKINS_NAME=jenkins-test-host
    helm install --name=$JENKINS_NAME -f values.yaml stable/jenkins 
    ```

1. Run the command provided in the output to print your jenkins password to the Cloud Shell console.
You will need this to log in, and a new password is created whenever you run `helm install` or
`helm upgrade`.

### Checking Jenkins status
1. Run the following to check the [status of your certificate](
https://cloud.google.com/load-balancing/docs/ssl-certificates#certificate-resource-status):
    ```bash
    gcloud beta compute ssl-certificates describe $CERTIFICATE_NAME
    ```

    You should see the status as `ACTIVE`. This may take up to an hour after the first helm install.

1. Run the following to check the status of the Kubernetes objects involved with running Jenkins on
your cluster.
    ```bash
    kubectl describe deployment $JENKINS_NAME
    kubectl describe ingress $JENKINS_NAME
    ```

    Here's what that should look like (`#` can be any number, including hex digits):
    
    ```
    $ user@cloudshell:~ (exampleproject)$ kubectl describe deployment $JENKINS_NAME
    Name:               jenkins-test-host
    Namespace:          default
    CreationTimestamp:  ...
    Labels:             chart=jenkins-0.32.9
                        component=jenkins-test-host-jenkins-master
                        heritage=Tiller
                        release=jenkins-test-host
    ...
    ...
    ...
    
    $ user@cloudshell:~ (exampleproject)$ kubectl describe ingress $JENKINS_NAME
    Name:             jenkins-test-host
    Namespace:        default
    Address:          #.#.#.#
    Default backend:  default-http-backend:80 (#.#.#.#:8080)
    Rules:
      Host                              Path  Backends
      ----                              ----  --------
      example.com
                                           jenkins-test-host:8080 (<none>)
    Annotations:
      ingress.kubernetes.io/url-map:                k8s-um-default-jenkins-test-host--7f##############
      kubernetes.io/ingress.allow-http:             false
      kubernetes.io/ingress.global-static-ip-name:  jenkins-address
      ingress.gcp.kubernetes.io/pre-shared-cert:    jenkins-host-certificate
      ingress.kubernetes.io/backends:               {"k8s-be-#####--7f##############":"HEALTHY","k8s-be-#####--7f##############":"HEALTHY"}
      ingress.kubernetes.io/https-forwarding-rule:  k8s-fws-default-jenkins-test-host--7f##############
      ingress.kubernetes.io/https-target-proxy:     k8s-tps-default-jenkins-test-host--7f##############
      ingress.kubernetes.io/ssl-cert:               jenkins-host-certificate
    ```

### Testing the plugin on Jenkins
1. Make sure you have another cluster and service account set up for testing deployments as
described in the GKE Plugin [usage documentation](Home.md#usage). You will need to repeat steps 3-8 
for the [IAM Credentials](Home.md#iam-credentials) section on this new Jenkins instance. This is the
reason that a certificate is needed, as you will need to ensure a secure encrypted connection when
uploading the private key file to Jenkins.

1. In your browser, go to `https://example.com` (replace with your domain) and you should see the
Jenkins login page.

1. Enter "admin" as the username, and enter the jenkins password retrieved above, then log in.

1. By default, this jenkins master will have the latest release of the GKE Plugin. See [Source Build
Installation](SourceBuildInstallation.md) for instructions on uploading a different version of the
plugin.

1. From the main jenkins page click **New Item**, then enter a name and choose **Freestyle project**.

1. Under **Source Code Management**, select Git and enter this repository:
https://github.com/craigatgoogle/testthrowaway.git

1. Follow the instructions at
[GKE Build Step Configuration](Home.md#google-kubernetes-engine-build-step-configuration) to test.
You will enter `manifest.yml` in the Kubernetes Manifests field.
