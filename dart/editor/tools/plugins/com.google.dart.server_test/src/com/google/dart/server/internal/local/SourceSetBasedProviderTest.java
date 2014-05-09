/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.server.internal.local;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.dart.engine.source.Source;
import com.google.dart.server.SourceSet;
import com.google.dart.server.SourceSetKind;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

public class SourceSetBasedProviderTest extends TestCase {
  private Set<Source> knownSources = Sets.newHashSet();
  private Set<Source> addedSources = Sets.newHashSet();
  private SourceSet sourceSet = mock(SourceSet.class);
  private SourceSet sourceSetA = mock(SourceSet.class);
  private SourceSet sourceSetB = mock(SourceSet.class);
  private Source source = mock(Source.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);

  public void test_apply_ALL() throws Exception {
    when(sourceSet.getKind()).thenReturn(SourceSetKind.ALL);
    SourceSetBasedProvider provider = newProvider();
    assertTrue(provider.apply(source));
  }

  public void test_apply_EXPLICITLY_ADDED() throws Exception {
    addedSources.add(sourceA);
    when(sourceSet.getKind()).thenReturn(SourceSetKind.EXPLICITLY_ADDED);
    SourceSetBasedProvider provider = newProvider();
    assertTrue(provider.apply(sourceA));
    assertFalse(provider.apply(sourceB));
  }

  public void test_apply_LIST() throws Exception {
    when(sourceSet.getKind()).thenReturn(SourceSetKind.LIST);
    when(sourceSet.getSources()).thenReturn(new Source[] {sourceA});
    SourceSetBasedProvider provider = newProvider();
    assertTrue(provider.apply(sourceA));
    assertFalse(provider.apply(sourceB));
  }

  public void test_apply_NON_SDK() throws Exception {
    when(sourceSet.getKind()).thenReturn(SourceSetKind.NON_SDK);
    when(sourceA.isInSystemLibrary()).thenReturn(true);
    when(sourceB.isInSystemLibrary()).thenReturn(false);
    SourceSetBasedProvider provider = newProvider();
    assertFalse(provider.apply(sourceA));
    assertTrue(provider.apply(sourceB));
  }

  public void test_computeNewSources_ALL_2_any() throws Exception {
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.ALL);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.LIST);
    assertNewSourcesEqualTo();
  }

  public void test_computeNewSources_ALL_2_LIST() throws Exception {
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.ALL);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.LIST);
    // -> [A] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    assertNewSourcesEqualTo();
    // -> [A, B] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA, sourceB});
    assertNewSourcesEqualTo();
  }

  public void test_computeNewSources_LIST_2_ALL() throws Exception {
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.LIST);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.ALL);
    assertNewSourcesEqualTo(sourceA, sourceB);
  }

  public void test_computeNewSources_LIST_2_EXPLICITLY_ADDED() throws Exception {
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.LIST);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.EXPLICITLY_ADDED);
    // [A] -> [A] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    assertNewSourcesEqualTo();
    // [B] -> [A] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceB});
    assertNewSourcesEqualTo(sourceA);
  }

  public void test_computeNewSources_LIST_2_LIST() throws Exception {
    knownSources.clear();
    addedSources.clear();
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.LIST);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.LIST);
    // [A] -> [A] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    when(sourceSetB.getSources()).thenReturn(new Source[] {sourceA});
    assertNewSourcesEqualTo();
    // [A] -> [B] => [B]
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    when(sourceSetB.getSources()).thenReturn(new Source[] {sourceB});
    assertNewSourcesEqualTo(sourceB);
    // [A, B] -> [B] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA, sourceB});
    when(sourceSetB.getSources()).thenReturn(new Source[] {sourceB});
    assertNewSourcesEqualTo();
    // [B] -> [A, B] => []
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceB});
    when(sourceSetB.getSources()).thenReturn(new Source[] {sourceA, sourceB});
    assertNewSourcesEqualTo(sourceA);
  }

  public void test_computeNewSources_LIST_2_NON_SDK() throws Exception {
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.LIST);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.NON_SDK);
    // inSDK = [];  [A] => [B]
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    assertNewSourcesEqualTo(sourceB);
    // inSDK = [A];  [A] => [B]
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    when(sourceA.isInSystemLibrary()).thenReturn(true);
    assertNewSourcesEqualTo(sourceB);
    // inSDK = [B];  [A] => [B]
    when(sourceSetA.getSources()).thenReturn(new Source[] {sourceA});
    when(sourceB.isInSystemLibrary()).thenReturn(true);
    assertNewSourcesEqualTo();
  }

  public void test_computeNewSources_NON_SDK_2_ALL() throws Exception {
    when(sourceSetA.getKind()).thenReturn(SourceSetKind.NON_SDK);
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.ALL);
    // inSDK = [A];  [A, B] => [A]
    when(sourceA.isInSystemLibrary()).thenReturn(true);
    when(sourceB.isInSystemLibrary()).thenReturn(false);
    assertNewSourcesEqualTo(sourceA);
    // inSDK = [B];  [A, B] => [B]
    when(sourceA.isInSystemLibrary()).thenReturn(false);
    when(sourceB.isInSystemLibrary()).thenReturn(true);
    assertNewSourcesEqualTo(sourceB);
  }

  public void test_computeNewSources_null_2_LIST() throws Exception {
    when(sourceSetB.getKind()).thenReturn(SourceSetKind.LIST);
    // null -> [A] => [A]
    when(sourceSetB.getSources()).thenReturn(new Source[] {sourceA});
    assertSetIsEqualTo(newProviderB().computeNewSources(null), sourceA);
    // null -> [A, B] => [A, B]
    when(sourceSetB.getSources()).thenReturn(new Source[] {sourceA, sourceB});
    assertSetIsEqualTo(newProviderB().computeNewSources(null), sourceA, sourceB);
  }

  public void test_create_notList() throws Exception {
    when(sourceSet.getKind()).thenReturn(SourceSetKind.ALL);
    when(sourceSet.getSources()).thenThrow(new IllegalStateException("Should not be called"));
    newProvider();
  }

  public void test_toString() throws Exception {
    SourceSetBasedProvider provider = new SourceSetBasedProvider(
        SourceSet.EXPLICITLY_ADDED,
        null,
        null);
    assertEquals("EXPLICITLY_ADDED", provider.toString());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(source.toString()).thenReturn("source");
    when(sourceA.toString()).thenReturn("sourceA");
    when(sourceB.toString()).thenReturn("sourceB");
    when(sourceA.isInSystemLibrary()).thenReturn(false);
    when(sourceB.isInSystemLibrary()).thenReturn(false);
    when(sourceSet.getSources()).thenReturn(Source.EMPTY_ARRAY);
    when(sourceSetA.getSources()).thenReturn(Source.EMPTY_ARRAY);
    when(sourceSetB.getSources()).thenReturn(Source.EMPTY_ARRAY);
    Collections.addAll(knownSources, sourceA, sourceB);
    Collections.addAll(addedSources, sourceA);
  }

  private void assertNewSourcesEqualTo(Source... expected) {
    SourceSetBasedProvider providerA = newProviderA();
    SourceSetBasedProvider providerB = newProviderB();
    Set<Source> actualSet = providerB.computeNewSources(providerA);
    assertSetIsEqualTo(actualSet, expected);
  }

  private <T> void assertSetIsEqualTo(Set<T> actualSet, T... expected) {
    Set<T> expectedSet = ImmutableSet.copyOf(expected);
    assertThat(actualSet).isEqualTo(expectedSet);
  }

  private SourceSetBasedProvider newProvider() {
    return new SourceSetBasedProvider(sourceSet, knownSources, addedSources);
  }

  private SourceSetBasedProvider newProviderA() {
    return new SourceSetBasedProvider(sourceSetA, knownSources, addedSources);
  }

  private SourceSetBasedProvider newProviderB() {
    return new SourceSetBasedProvider(sourceSetB, knownSources, addedSources);
  }
}
