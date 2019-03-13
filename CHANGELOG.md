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
 
