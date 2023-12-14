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

package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.DescriptorImpl;
import hudson.util.FormValidation;
import org.junit.Test;

/** Tests for handling of namespaces in {@link KubernetesEngineBuilder}. */
public class KubernetesEngineBuilderNamespaceTest {

    @Test
    public void testDoCheckNamespaceOKWithNull() {
        DescriptorImpl descriptor = new DescriptorImpl();
        FormValidation result = descriptor.doCheckNamespace(null);
        assertNotNull(result);
        assertEquals(FormValidation.ok().getMessage(), result.getMessage());
    }

    @Test
    public void testDoCheckNamespaceOKWithEmptyString() {
        DescriptorImpl descriptor = new DescriptorImpl();
        FormValidation result = descriptor.doCheckNamespace("");
        assertNotNull(result);
        assertEquals(FormValidation.ok().getMessage(), result.getMessage());
    }

    @Test
    public void testDoCheckNamespaceWithOKProperlyFormedString() {
        DescriptorImpl descriptor = new DescriptorImpl();
        FormValidation result = descriptor.doCheckNamespace("test-a-23-b");
        assertNotNull(result);
        assertEquals(FormValidation.ok().getMessage(), result.getMessage());
    }

    @Test
    public void testDoCheckNamespaceErrorWithValidCharactersMalformedString() {
        DescriptorImpl descriptor = new DescriptorImpl();
        FormValidation result = descriptor.doCheckNamespace("-test");
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_NamespaceInvalid(), result.getMessage());
    }

    @Test
    public void testDoCheckNamespaceErrorWithInvalidCharacters() {
        DescriptorImpl descriptor = new DescriptorImpl();
        FormValidation result = descriptor.doCheckNamespace("*");
        assertNotNull(result);
        assertEquals(Messages.KubernetesEngineBuilder_NamespaceInvalid(), result.getMessage());
    }
}
