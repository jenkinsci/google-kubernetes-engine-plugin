# Copyright 2019 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/bin/bash

# This file contains a script for publishing the maven-kubectl image to GCR.
# It assumes that the docker and gcloud CLI tools are both installed and
# configured. For more information see: https://cloud.google.com/container-registry/docs/quickstart

# fail on error
set -e

export GCR_PROJECT_ID="jenkins-gke-plugin"
export IMAGE_NAME="maven-kubectl"

echo "Building image..."
gcloud config set project $GCR_PROJECT_ID
gcloud auth configure-docker
docker build -t $IMAGE_NAME .
docker tag maven-kubectl gcr.io/${GCR_PROJECT_ID}/${IMAGE_NAME}:latest
docker push gcr.io/${GCR_PROJECT_ID}/${IMAGE_NAME}:latest

