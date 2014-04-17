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

package com.google.dart.server.internal.local.computer;

import com.google.dart.engine.source.Source;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationTargetImplTest extends AbstractLocalServerTest {
  private Source source = mock(Source.class);

  public void test_access() throws Exception {
    NavigationTargetImpl target = new NavigationTargetImpl(source, "id", 10, 20);
    assertSame(source, target.getSource());
    assertEquals("id", target.getElementId());
    assertEquals(10, target.getOffset());
    assertEquals(20, target.getLength());
  }

  public void test_toString() throws Exception {
    NavigationTargetImpl target = new NavigationTargetImpl(source, "id", 10, 20);
    assertEquals("[offset=10, length=20, source=/my/test.dart, element=id]", target.toString());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(source.toString()).thenReturn("/my/test.dart");
  }
}
