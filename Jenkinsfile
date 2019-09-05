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
	GOOGLE_PROJECT_ID = "${JENKINS_TEST_PROJECT}"
	GOOGLE_GKE_CLUSTER = "${JENKINS_TEST_CLUSTER}"
        GOOGLE_PROJECT_ZONE = "${JENKINS_TEST_PROJECT_ZONE}"
        GOOGLE_BUCKET = "${JENKINS_TEST_BUCKET}"
    }

    stages {
        stage("Build and test") {
	    agent {
    	    	kubernetes {
      		    cloud 'kubernetes'
      		    label 'mavenpod'
      		    yamlFile 'jenkins/maven-pod.yaml'
		}
	    }
	    steps {
	    	container('maven') {
		    // build
	    	    sh "mvn clean package"

		    // run tests
		    sh "mvn verify"
		}
	    }
	}
    }
}

