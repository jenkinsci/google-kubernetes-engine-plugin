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
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unresolved]

 ### Security
 
 ### Added
  
 ### Changed
				
 ### Removed
				 
 ### Fixed

## [0.7.0] - 2019-09-18
### Added
- com.google.cloud.graphite:gcp-client:0.1.2 from
[gcp-plugin-core-java](https://github.com/GoogleCloudPlatform/gcp-plugin-core-java) for replacing
current clients.
- ClientUtil to generate ClientFactory from gcp-plugin-core-java.

### Changed
- org.jenkins-ci.plugins:credentials version changed: 2.1.16 to 2.2.0
- org.jenkins-ci.plugins:google-oauth-plugin version changed: 0.7 to 0.9
- com.google.api-client:api-client version changed: 1.25.0 to 1.24.1
- com.google.http-client:api-client version changed: 1.25.0 to 1.24.1
- com.google.guava version changed: 19.0 to 14.0.1
- Retrieving the default project ID of a service account is now handled in CredentialsUtil after
the removal of ClientFactory. 

### Removed
- com.google.apis:google-api-services-container, handled by gcp-plugin-core-java.
- com.google.apis:google-api-services-cloudresourcemanager, handled by gcp-plugin-core-java.
- ClientFactory, CloudResourceManagerClient, ContainerClient and associated tests: migrated to
gcp-plugin-core-java.
- StringJsonServiceAccountConfig: Now using JsonServiceAccountConfig from google-oauth-plugin
directly for test credentials.
 
## [0.6.3] - 2019-07-23
### Security
- Fixed security issue regarding temp KubeConfig files: https://issues.jenkins-ci.org/browse/SECURITY-1345
 
## [0.6.2] - 2019-07-09
### Removed
 - Removed the maven plugin findbugs in favor of spotbugs from the Jenkins parent plugin pom file.

### Fixed
 - Issue #85: To allow for customers using Google OAuth2 credentials from metadata, use
 `Credential` type rather than the more specific `GoogleCredential` when retrieving access token.

## [0.6.1] - 2019-06-06
### Changed
 - Issue #79: Viewing the Google OAuth2 Service Account credentials in the build step dropdown now
 only requires the Credentials:View permission. Previously the Item:Configure permission was
 required. 

## [0.6.0] - 2019-05-16
### Security
 - Issue #20: It is no longer required to enable legacy authorization.
 - Issue #20: Required IAM permissions reduced.

### Added
 - Issue #20: Helm chart for configuration of RBAC.
 - Issue #60: Support for regional clusters. Use the location field in the pipeline to specify
   either a compute region (e.g. us-central1) or zone (e.g. us-central1-a).

### Changed
 - Issue #20: Refactored authentication to use access token.
 - Issue #60: Cluster dropdown populates clusters from all regions and zones and the entries
   include both the cluster name and location.
 - Issue #67: The log output is less verbose during deployment.

### Removed
 - Issue #60: Removed dropdown for zone selection.

## [0.5.0] - 2019-05-07

 ### Security

 ### Added
  - Issue #65: Allow user to specify namespace for deployment. This is available in the UI and
    through the `namespace` field for this build step on the build pipeline.

 ### Changed
  - Issue #62: Update help text for verify deployments flag to include information about process.
  - When the namespace for deployment is not specified in the manifest or as a user entered value
    then it will always be `default` rather than picking up residual namespaces from the user's
    host machine.

 ### Removed

 ### Fixed
  - Issue #56: The build from source instructions now include the `compile` step explicitly because
    compilation is not performed by default for building the hpi file.

## [0.4.0] - 2019-04-29

 ### Security
 
 ### Added
  - Issue #42: Implemented logic to add usage metric label to deployments from plugin.
  - Issue #24: Added support for declarative Pipeline DSL.
  
 ### Changed
  - Updated to Jenkins LTS 2.164.2.
  - Fixed dependency version conflicts.
  				
 ### Removed
				 
 ### Fixed

## [0.3.0] - 2019-04-04

 ### Security
 
 ### Added
  - Issue #16: Migrated wiki from wiki.jenkins.io to this repository.
  - Issue #21: Added help text for all fields.
  - PRs #32, #40: Added tutorial and resources for running jenkins on GKE.
  - Issue #35: Added xml-format-maven-plugin for consistent formatting of pom file.
  - Issue #23: Deployment verification.
  
 ### Changed
  - Issue #21: Changed display name of build step to use verb form.
  - Issue #27: Removed shared mutable states from tests.
  - Issue #35: Made pom file indentation and spacing consistent.
  - Issue #38: Corrections clarification for usage documentation.
  - Enabled google-java-format for fmt-maven-plugin.
				
 ### Removed
				 
 ### Fixed
  - Issue #29: Fixed inaccuracy of verification messages when there are no resources available or
    when a previously configured value corresponds to a GCP resource that has been deleted.
  - Added missing license headers and updated license header format.
 
## [0.2.0] - 2019-03-01

 ### Security
  - Upgraded jenkins version to LTS version 2.107.1

 ### Added
  - WebUI QOL Features
    - Dropdowns for Project ID, Zone, and Cluster Name
    - Autofilling of Project ID, Zone, and Cluster Name based on other form entries.
    - Verification of Project ID, Zone, and Cluster Name
  - Light clients for GCE API and Cloud Resource Manager API

 ### Changed
  - Made the post-build action a build step

 ### Removed
  - Credentials verification no longer depends on Project ID

 ### Fixed
  - Upgraded google-oauth-plugin to version 0.7
  - Failing windows build
  - Saved value of Kubernetes manifest now persists when re-opening build configuration.
  - Compiler warnings

## [0.1.0] - 2019-01-17

 ### Security
 
 ### Added
  - Initial release.
  - Support for publishing deployments to clusters running in GKE.
  
 ### Changed
				
 ### Removed
				 
 ### Fixed
 
