package com.google.jenkins.plugins.k8sengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.jenkins.plugins.k8sengine.KubernetesEngineBuilder.DescriptorImpl;
import hudson.util.FormValidation;
import org.junit.Test;

/** Tests for handling of namespaces in {@link KubernetesEngineBuilder}. */
public class KubernetesEngineBuilderNamespaceTest {

  @Test(expected = NullPointerException.class)
  public void testDoCheckNamespaceNPEWithNull() {
    DescriptorImpl descriptor = new DescriptorImpl();
    descriptor.doCheckNamespace(null);
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
