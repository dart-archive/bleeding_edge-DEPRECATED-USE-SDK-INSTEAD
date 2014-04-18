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
import com.google.dart.engine.source.Source;
import com.google.dart.server.ListSourceSet;
import com.google.dart.server.SourceSet;
import com.google.dart.server.SourceSetKind;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListSourceSetTest extends TestCase {
  public void test_create_array() throws Exception {
    Source sourceA = mock(Source.class);
    Source sourceB = mock(Source.class);
    SourceSet sourceSet = ListSourceSet.create(sourceA, sourceB);
    assertSame(SourceSetKind.LIST, sourceSet.getKind());
    assertThat(sourceSet.getSources()).containsOnly(sourceA, sourceB);
  }

  public void test_create_collection() throws Exception {
    Source sourceA = mock(Source.class);
    Source sourceB = mock(Source.class);
    SourceSet sourceSet = ListSourceSet.create(ImmutableSet.of(sourceA, sourceB));
    assertSame(SourceSetKind.LIST, sourceSet.getKind());
    assertThat(sourceSet.getSources()).containsOnly(sourceA, sourceB);
  }

  public void test_create_toString() throws Exception {
    Source sourceA = mock(Source.class);
    Source sourceB = mock(Source.class);
    when(sourceA.toString()).thenReturn("sourceA");
    when(sourceB.toString()).thenReturn("sourceB");
    SourceSet sourceSet = ListSourceSet.create(sourceA, sourceB);
    assertEquals("[sourceA, sourceB]", sourceSet.toString());
  }
}
