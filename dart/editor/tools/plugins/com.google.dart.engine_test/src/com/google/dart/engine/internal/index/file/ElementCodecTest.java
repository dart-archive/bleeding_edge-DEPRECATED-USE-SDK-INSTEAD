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
import com.google.dart.engine.internal.element.ElementLocationImpl;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElementCodecTest extends TestCase {
  private static final String LIB = "file:/lib.dart";
  private static final String UNIT = "file:/unit.dart";

  private AnalysisContext context = mock(AnalysisContext.class);
  private StringCodec stringCodec = new StringCodec();
  private ElementCodec codec = new ElementCodec(stringCodec);

  public void test_encodeHash_local() throws Exception {
    int idA;
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "A", "foo@1"});
      when(element.getLocation()).thenReturn(location);
      when(context.getElement(location)).thenReturn(element);
      idA = codec.encodeHash(element);
    }
    int idB;
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "A", "foo@2"});
      when(element.getLocation()).thenReturn(location);
      when(context.getElement(location)).thenReturn(element);
      idB = codec.encodeHash(element);
    }
    // offset is simply ignored
    assertTrue(idA == idB);
    // check strings, "foo" as a single string, no "foo@1" or "foo@2"
    assertThat(stringCodec.getNameToIndex()).hasSize(2).includes(entry(LIB, 0), entry("foo", 1));
  }

  public void test_encodeHash_notLocal() throws Exception {
    int idA;
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "A"});
      when(element.getLocation()).thenReturn(location);
      when(context.getElement(location)).thenReturn(element);
      idA = codec.encodeHash(element);
    }
    int idB;
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "B"});
      when(element.getLocation()).thenReturn(location);
      when(context.getElement(location)).thenReturn(element);
      idB = codec.encodeHash(element);
    }
    assertFalse(idA == idB);
  }

  public void test_localLocalVariable() throws Exception {
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "foo@1", "bar@2"});
      when(context.getElement(location)).thenReturn(element);
      when(element.getLocation()).thenReturn(location);
      int id = codec.encode(element);
      assertEquals(element, codec.decode(context, id));
    }
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {
          LIB, UNIT, "foo@10", "bar@20"});
      when(context.getElement(location)).thenReturn(element);
      when(element.getLocation()).thenReturn(location);
      int id = codec.encode(element);
      assertEquals(element, codec.decode(context, id));
    }
    // check strings, "foo" as a single string, no "foo@1" or "foo@10"
    assertThat(stringCodec.getNameToIndex()).hasSize(4).includes(
        entry(LIB, 0),
        entry(UNIT, 1),
        entry("foo", 2),
        entry("bar", 3));
  }

  public void test_localVariable() throws Exception {
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "foo@42"});
      when(context.getElement(location)).thenReturn(element);
      when(element.getLocation()).thenReturn(location);
      int id = codec.encode(element);
      assertEquals(element, codec.decode(context, id));
    }
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "foo@4200"});
      when(context.getElement(location)).thenReturn(element);
      when(element.getLocation()).thenReturn(location);
      int id = codec.encode(element);
      assertEquals(element, codec.decode(context, id));
    }
    // check strings, "foo" as a single string, no "foo@42" or "foo@4200"
    assertThat(stringCodec.getNameToIndex()).hasSize(3).includes(
        entry(LIB, 0),
        entry(UNIT, 1),
        entry("foo", 2));
  }

  public void test_notLocal() throws Exception {
    Element element = mock(Element.class);
    ElementLocation location = new ElementLocationImpl(new String[] {LIB, UNIT, "bar"});
    when(element.getLocation()).thenReturn(location);
    when(context.getElement(location)).thenReturn(element);
    int id = codec.encode(element);
    assertEquals(element, codec.decode(context, id));
    // check strings
    assertThat(stringCodec.getNameToIndex()).hasSize(3).includes(
        entry(LIB, 0),
        entry(UNIT, 1),
        entry("bar", 2));
  }

  @Override
  protected void tearDown() throws Exception {
    stringCodec = null;
    codec = null;
    super.tearDown();
  }
}
