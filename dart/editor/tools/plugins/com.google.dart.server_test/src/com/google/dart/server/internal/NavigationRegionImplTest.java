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

package com.google.dart.server.internal;

import com.google.dart.server.internal.ElementImpl;
import com.google.dart.server.internal.NavigationRegionImpl;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationRegionImplTest extends TestCase {
  private ElementImpl targetA = mock(ElementImpl.class);
  private ElementImpl targetB = mock(ElementImpl.class);
  private ElementImpl[] targets = {targetA, targetB};

  public void test_access() throws Exception {
    NavigationRegionImpl region = new NavigationRegionImpl(10, 20, targets);
    assertEquals(10, region.getOffset());
    assertEquals(20, region.getLength());
    assertSame(targets, region.getTargets());
  }

  public void test_toString() throws Exception {
    NavigationRegionImpl region = new NavigationRegionImpl(10, 20, targets);
    assertEquals("[offset=10, length=20] -> [targetA, targetB]", region.toString());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(targetA.toString()).thenReturn("targetA");
    when(targetB.toString()).thenReturn("targetB");
  }
}
