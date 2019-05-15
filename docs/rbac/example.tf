# Set up project ID variable
# Replace {{YOUR_GCP_PROJECT_ID}} with your own project id.
variable project {
    type    = "string"
    default = "{{YOUR_GCP_PROJECT_ID}}"
}

# Set up region
# Replace {{YOUR_PROJECT_REGION}} with your project region.
variable region {
    type    = "string"
    default = "{{YOUR_PROJECT_REGION}}"g
}

# Create the Google Cloud terraform provider
provider "google" {
  project = "${var.project}"
  region  = "us-central1"
  zone    = "us-central1-c"
}

# Create a custom IAM role to bind to our GCP service account

# Declare a special IAM role
resource "google_project_iam_custom_role" "minimal-k8s-role" {
  role_id     = "Minimalk8sIamRole"
  title       = "Minimal IAM role for GKE access"
  description = "Bare minimum permissions to access the kubernetes API for using the Jenkins GKE plugin."
  project     = "${var.project}"

  permissions = [
    "compute.zones.list",
    "container.apiServices.get",
    "container.apiServices.list",
    "container.clusters.get",
    "container.clusters.getCredentials",
    "container.clusters.list",
    "resourcemanager.projects.get",
  ]
}

# Create our service account called jenkins-gke-deployer.
# More information: https://www.terraform.io/docs/providers/google/r/google_service_account.html
resource "google_service_account" "jenkins-gke-deployer" {
  account_id   = "jenkins-gke-deployer"
  display_name = "jenkins-gke-deployer"
}

# Assign the special IAM role to the service account
resource "google_project_iam_member" "jenkins-deployer-gke-access" {
  project = "${var.project}"
  role    = "projects/${var.project}/roles/Minimalk8sIamRole"
  member  = "serviceAccount:${google_service_account.jenkins-gke-deployer.email}"
}

# Store the service account key in Terraform so that we can access it
resource "google_service_account_key" "jenkins-gke-deployer" {
  service_account_id = "${google_service_account.jenkins-gke-deployer.id}"
}
