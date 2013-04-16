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

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CompositeChangeTest extends TestCase {
  public void test_access() throws Exception {
    CompositeChange change = new CompositeChange("myName");
    assertEquals("myName", change.getName());
  }

  public void test_children() throws Exception {
    Change changeA = mock(Change.class);
    Change changeB = mock(Change.class);
    // empty
    CompositeChange change = new CompositeChange("myName");
    assertThat(change.getChildren()).isEmpty();
    // add edits
    change.add(changeA, changeB);
    assertThat(change.getChildren()).containsExactly(changeA, changeB);
  }

  public void test_new_withChanges() throws Exception {
    Change changeA = mock(Change.class);
    Change changeB = mock(Change.class);
    // new CompositeChange
    CompositeChange change = new CompositeChange("myName", changeA, changeB);
    assertEquals("myName", change.getName());
    // has changes
    assertThat(change.getChildren()).containsExactly(changeA, changeB);
  }
}
