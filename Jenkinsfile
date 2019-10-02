/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
    agent none

    environment {
        GOOGLE_PROJECT_ZONE = "${GKE_IT_ZONE}"
        GOOGLE_PROJECT_ID = "${GKE_IT_PROJECT_ID}"
        GOOGLE_GKE_CLUSTER = "${GKE_IT_CLUSTER}"
    }

    stages {
        stage("Build and test") {
	    agent {
    	    	kubernetes {
      		    cloud 'kubernetes'
      		    label 'maven-kubectl-pod'
      		    yamlFile 'jenkins/maven-kubectl-pod.yaml'
		}
	    }
	    steps {
	    	container('maven-kubectl') {
                    withCredentials([[$class: 'StringBinding', credentialsId: env.GKE_IT_CRED_ID, variable: 'GOOGLE_CREDENTIALS']]) {
		        // build
	    	        sh "mvn clean package -ntp"

		        // run tests
		        sh "mvn verify -ntp"
                    }
                }
            }
        }
    }
}
