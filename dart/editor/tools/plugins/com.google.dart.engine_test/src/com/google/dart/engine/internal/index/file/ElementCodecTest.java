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
  private AnalysisContext context = mock(AnalysisContext.class);
  private StringCodec stringCodec = new StringCodec();
  private ElementCodec codec = new ElementCodec(stringCodec);

  public void test_localVariable() throws Exception {
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {"main", "foo@42"});
      when(context.getElement(location)).thenReturn(element);
      when(element.getLocation()).thenReturn(location);
      int id = codec.encode(element);
      assertEquals(element, codec.decode(context, id));
    }
    {
      Element element = mock(Element.class);
      ElementLocation location = new ElementLocationImpl(new String[] {"main", "foo@4200"});
      when(context.getElement(location)).thenReturn(element);
      when(element.getLocation()).thenReturn(location);
      int id = codec.encode(element);
      assertEquals(element, codec.decode(context, id));
    }
    // check strings, "foo" as a single string, no "foo@42" or "foo@4200"
    assertThat(stringCodec.getNameToIndex()).hasSize(2).includes(entry("main", 0), entry("foo", 1));
  }

  public void test_notLocal() throws Exception {
    Element element = mock(Element.class);
    ElementLocation location = new ElementLocationImpl(new String[] {"foo", "bar"});
    when(element.getLocation()).thenReturn(location);
    when(context.getElement(location)).thenReturn(element);
    int id = codec.encode(element);
    assertEquals(element, codec.decode(context, id));
    // check strings
    assertThat(stringCodec.getNameToIndex()).hasSize(2).includes(entry("foo", 0), entry("bar", 1));
  }
}
