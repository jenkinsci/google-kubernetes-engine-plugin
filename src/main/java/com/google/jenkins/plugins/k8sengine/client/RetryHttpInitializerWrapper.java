/*
 * Copyright 2019 Google LLC
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

package com.google.jenkins.plugins.k8sengine.client;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * RetryHttpInitializerWrapper will automatically retry upon RPC failures, preserving the
 * auto-refresh behavior of the Google Credentials.
 *
 * <p>Note: original source from <a
 * href="https://github.com/GoogleCloudPlatform/java-docs-samples/blob/master/storage/storage-transfer/src/main/java/com/google/cloud/storage/storagetransfer/samples/RetryHttpInitializerWrapper.java">RetryHttpInitializerWrapper.java</a>
 */
public class RetryHttpInitializerWrapper implements HttpRequestInitializer {

    private static final Logger LOG = Logger.getLogger(RetryHttpInitializerWrapper.class.getName());
    private static final int MILLIS_PER_MINUTE = 60 * 1000;
    private final Credential wrappedCredential;
    private final Sleeper sleeper;

    /**
     * A constructor using the default Sleeper.
     *
     * @param wrappedCredential the credential used to authenticate with a Google Cloud Platform
     *     project
     */
    public RetryHttpInitializerWrapper(Credential wrappedCredential) {
        this(wrappedCredential, Sleeper.DEFAULT);
    }

    /**
     * A constructor used only for testing.
     *
     * @param wrappedCredential the credential used to authenticate with a Google Cloud Platform
     *     project
     * @param sleeper a user-supplied Sleeper
     */
    RetryHttpInitializerWrapper(Credential wrappedCredential, Sleeper sleeper) {
        this.wrappedCredential = Preconditions.checkNotNull(wrappedCredential);
        this.sleeper = sleeper;
    }

    /**
     * Initialize an HttpRequest.
     *
     * @param request an HttpRequest that should be initialized
     */
    public void initialize(HttpRequest request) {
        request.setReadTimeout(2 * MILLIS_PER_MINUTE); // 2 minutes read timeout
        final HttpUnsuccessfulResponseHandler backoffHandler =
                new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()).setSleeper(sleeper);
        request.setInterceptor(wrappedCredential);
        request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
            public boolean handleResponse(
                    final HttpRequest request, final HttpResponse response, final boolean supportsRetry)
                    throws IOException {
                if (wrappedCredential.handleResponse(request, response, supportsRetry)) {
                    // If credential decides it can handle it, the return code or message indicated
                    // something specific to authentication, and no backoff is desired.
                    return true;
                } else if (backoffHandler.handleResponse(request, response, supportsRetry)) {
                    // Otherwise, we defer to the judgement of our internal backoff handler.
                    LOG.info("Retrying " + request.getUrl().toString());
                    return true;
                } else {
                    return false;
                }
            }
        });
        request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(new ExponentialBackOff()).setSleeper(sleeper));
    }
}
