/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.change;

import com.google.dart.engine.source.Source;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SourceChangeManager}.
 */
public class SourceChangeManagerTest extends TestCase {
  final SourceChangeManager manager = new SourceChangeManager();
  private final Source sourceA = mock(Source.class);
  private final Source sourceB = mock(Source.class);

  public void test_get() throws Exception {
    SourceChange changeA = manager.get(sourceA);
    // same SourceChange for same Source
    assertSame(changeA, manager.get(sourceA));
    // different SourceChange for different Source
    assertNotSame(changeA, manager.get(sourceB));
  }

  public void test_getChanges() throws Exception {
    SourceChange changeA = manager.get(sourceA);
    SourceChange changeB = manager.get(sourceB);
    assertThat(manager.getChanges()).containsOnly(changeA, changeB);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(sourceA.getShortName()).thenReturn("A");
    when(sourceB.getShortName()).thenReturn("B");
  }
}
