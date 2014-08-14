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

import com.google.dart.server.Element;
import com.google.dart.server.SearchResultKind;
import com.google.dart.server.generated.types.Location;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;

public class SearchResultImplTest extends TestCase {
  public void test_access() throws Exception {
    SearchResultKind kind = SearchResultKind.REFERENCE;
    Element[] path = new Element[0];
    Location location = mock(Location.class);
    SearchResultImpl searchResult = new SearchResultImpl(path, kind, location, false);
    assertSame(path, searchResult.getPath());
    assertSame(kind, searchResult.getKind());
    assertSame(location, searchResult.getLocation());
    assertFalse(searchResult.isPotential());
    // toString()
    assertNotNull(searchResult.toString());
  }
}
