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
package com.google.dart.engine.internal.index.file;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.element.ElementLocationImpl;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RelationKeyDataTest extends TestCase {
  private AnalysisContext context = mock(AnalysisContext.class);
  private StringCodec stringCodec = new StringCodec();
  private ElementCodec elementCodec = new ElementCodec(stringCodec);
  private RelationshipCodec relationshipCodec = new RelationshipCodec(stringCodec);

  public void test_newFromData() throws Exception {
    RelationKeyData keyData = new RelationKeyData(1, 2);
    // equals
    assertFalse(keyData.equals(null));
    assertTrue(keyData.equals(keyData));
  }

  public void test_newFromObjects() throws Exception {
    Relationship relationship = Relationship.getRelationship("my-relationship");
    // prepare Element
    Element element;
    {
      element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {"foo", "bar"});
      when(element.getLocation()).thenReturn(location);
      when(context.getElement(location)).thenReturn(element);
    }
    // create RelationKeyData
    RelationKeyData keyData = new RelationKeyData(
        elementCodec,
        relationshipCodec,
        element,
        relationship);
    // touch
    keyData.hashCode();
    // equals
    assertFalse(keyData.equals(null));
    assertTrue(keyData.equals(keyData));
  }
}
