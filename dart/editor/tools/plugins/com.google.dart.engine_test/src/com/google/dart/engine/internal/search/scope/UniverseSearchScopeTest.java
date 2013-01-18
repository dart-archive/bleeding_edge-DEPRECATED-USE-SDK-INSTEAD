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
package com.google.dart.engine.internal.search.scope;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchScope;

import static org.mockito.Mockito.mock;

public class UniverseSearchScopeTest extends EngineTestCase {
  private final SearchScope scope = new UniverseSearchScope();
  private final Element element = mock(Element.class);

  public void test_anyElement() throws Exception {
    assertTrue(scope.encloses(element));
  }

  public void test_nullElement() throws Exception {
    assertTrue(scope.encloses(null));
  }
}
