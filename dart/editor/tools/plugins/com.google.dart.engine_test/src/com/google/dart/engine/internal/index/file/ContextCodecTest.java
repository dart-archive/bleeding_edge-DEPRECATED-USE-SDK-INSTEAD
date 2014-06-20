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

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;

public class ContextCodecTest extends TestCase {
  private ContextCodec codec = new ContextCodec();

  public void test_encode_decode() throws Exception {
    AnalysisContext contextA = mock(AnalysisContext.class);
    AnalysisContext contextB = mock(AnalysisContext.class);
    int idA = codec.encode(contextA);
    int idB = codec.encode(contextB);
    assertEquals(idA, codec.encode(contextA));
    assertEquals(idB, codec.encode(contextB));
    assertSame(contextA, codec.decode(idA));
    assertSame(contextB, codec.decode(idB));
  }

  public void test_remove() throws Exception {
    // encode
    {
      AnalysisContext context = mock(AnalysisContext.class);
      int id = codec.encode(context);
      assertEquals(0, id);
      assertSame(context, codec.decode(id));
      // remove
      codec.removeContext(context);
      assertNull(codec.decode(id));
    }
    // encode again
    {
      AnalysisContext context = mock(AnalysisContext.class);
      int id = codec.encode(context);
      assertEquals(1, id);
      assertSame(context, codec.decode(id));
    }
  }
}
