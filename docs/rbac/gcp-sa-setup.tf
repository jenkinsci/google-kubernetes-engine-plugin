variable project {
  type        = string
  description = "The GCP project to be configured."
}

variable region {
  type        = string
  description = "The GCP region."
}

variable sa_name {
  type        = string
  description = "The name of the service account to be created."
}

# Create the Google Cloud terraform provider
provider "google" {
  project = "${var.project}"
  region  = "${var.region}"
}

# Create a custom IAM role to bind to our GCP service account

# Declare a special IAM role
resource "google_project_iam_custom_role" "gke-deployer" {
  role_id     = "gke_deployer"
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
  account_id   = "${var.sa_name}"
  display_name = "${var.sa_name}"
}

# Assign the special IAM role to the service account
resource "google_project_iam_member" "jenkins-deployer-gke-access" {
  project = "${var.project}"
  role    = "projects/${var.project}/roles/${google_project_iam_custom_role.gke-deployer.role_id}"
  member  = "serviceAccount:${google_service_account.jenkins-gke-deployer.email}"
}
