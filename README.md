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

# Google Kubernetes Engine Plugin for Jenkins

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/google-kubernetes-engine.svg)](https://plugins.jenkins.io/google-kubernetes-engine)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/google-kubernetes-engine.svg?color=blue)](https://plugins.jenkins.io/google-kubernetes-engine)

The Google Kubernetes Engine (GKE) Plugin allows you to deploy build artifacts
to Kubernetes clusters running in GKE with Jenkins.

## Documentation

Please see the [Google Kubernetes Engine Plugin](docs/Home.md) docs for complete
documentation.

## Installation

1.  Go to **Manage Jenkins** then **Manage Plugins**.
1.  (Optional) Make sure the plugin manager has updated data by clicking the
    **Check now** button.
1.  In the Plugin Manager, click the **Available** tab and look for the "Google
    Kubernetes Engine Plugin".
1.  Check the box under the **Install** column and click the **Install without
    restart** button.
1.  If the plugin does not appear under **Available**, make sure it appears
    under **Installed** and is enabled.

## Plugin Source Build Installation

See [Plugin Source Build Installation](docs/SourceBuildInstallation.md) to build
and install from source.

## Usage

See the [Usage](docs/Home.md#usage) documentation for how to create a `Deploy to GKE` build step.

## Feature requests and bug reports

Please file feature requests and bug reports as
[github issues](https://github.com/jenkinsci/google-kubernetes-engine-plugin/issues).

**NOTE**: Starting with version 0.7 of this plugin, version 0.9 or higher of the
[Google OAuth Credentials plugin](https://github.com/jenkinsci/google-oauth-plugin) must be used.
Older versions of this plugin are still compatible with version 0.9 of the OAuth plugin.

## Community

The GCP Jenkins community uses the **#gcp-jenkins** slack channel on
[https://googlecloud-community.slack.com](https://googlecloud-community.slack.com)
to ask questions and share feedback. Invitation link available here: 
[gcp-slack](https://cloud.google.com/community#home-support).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

## License

See [LICENSE](LICENSE)
