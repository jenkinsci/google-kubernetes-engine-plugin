variable project {
  type        = "string"
  description = "The GCP project to be configured."
}

variable region {
  type        = "string"
  description = "The GCP region."
}

variable sa_name {
  type        = "string"
  description = "The name of the service account to be created."
}

# Create the Google Cloud terraform provider
provider "google" {
  project = "${var.project}"
  region  = "${var.region}"
}

# Create our service account called jenkins-gke-deployer.
# More information: https://www.terraform.io/docs/providers/google/r/google_service_account.html
resource "google_service_account" "jenkins-gke-deployer" {
  account_id   = "${var.sa_name}"
  display_name = "${var.sa_name}"
}

# Assign Kubernetes Engine Cluster Viewer IAM role to the service account
resource "google_project_iam_member" "jenkins-deployer-gke-access" {
  project = "${var.project}"
  role    = "roles/container.clusterViewer"
  member  = "serviceAccount:${google_service_account.jenkins-gke-deployer.email}"
}
