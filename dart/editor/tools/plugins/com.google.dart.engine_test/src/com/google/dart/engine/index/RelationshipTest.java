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
package com.google.dart.engine.index;

import com.google.dart.engine.EngineTestCase;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collection;

public class RelationshipTest extends EngineTestCase {

  public void test_getIdentifier() throws Exception {
    Relationship relationship = Relationship.getRelationship("test-id");
    assertEquals("test-id", relationship.getIdentifier());
  }

  public void test_getRelationship() throws Exception {
    Relationship relationship = Relationship.getRelationship("test-id");
    assertSame(relationship, Relationship.getRelationship("test-id"));
    assertNotSame(relationship, Relationship.getRelationship("test-id2"));
  }

  public void test_toString() throws Exception {
    Relationship relationship = Relationship.getRelationship("test-id");
    assertEquals("test-id", relationship.toString());
  }

  public void test_values() throws Exception {
    Relationship relationship = Relationship.getRelationship("test-id");
    Collection<Relationship> values = Relationship.values();
    assertThat(values).contains(relationship);
  }
}
