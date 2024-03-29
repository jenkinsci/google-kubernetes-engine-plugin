/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.k8sengine.client;

import com.google.api.services.container.ContainerScopes;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Defines the scope requirements for the GKE "Container" API. */
public class ContainerScopeRequirement extends GoogleOAuth2ScopeRequirement {
    @Override
    public Collection<String> getScopes() {
        List<String> scopes = new ArrayList<>();
        scopes.addAll(ContainerScopes.all());
        // E-mail scope for k8s to associate the GCP service account with the e-mail address
        scopes.add("https://www.googleapis.com/auth/userinfo.email");
        return scopes;
    }
}
