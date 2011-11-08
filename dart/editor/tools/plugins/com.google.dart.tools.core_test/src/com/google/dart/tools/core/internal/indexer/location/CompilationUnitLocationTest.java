/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.indexer.location;

import com.google.dart.tools.core.model.CompilationUnit;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyCompilationUnit;

import junit.framework.TestCase;

import java.net.URI;

public class CompilationUnitLocationTest extends TestCase {
  public void test_CompilationUnitLocation_getCompilationUnit() throws Exception {
    CompilationUnit unit = getMoneyCompilationUnit("money.dart");
    CompilationUnitLocation location = new CompilationUnitLocation(unit, unit.getSourceRange());
    assertEquals(unit, location.getCompilationUnit());
  }

  public void test_CompilationUnitLocation_getContainingUri() throws Exception {
    String compilationUnitName = "money.dart";
    CompilationUnit unit = getMoneyCompilationUnit(compilationUnitName);
    CompilationUnitLocation location = new CompilationUnitLocation(unit, unit.getSourceRange());
    URI uri = location.getContainingUri();
    assertNotNull(uri);
    assertTrue(uri.isAbsolute());
    assertTrue(uri.toString().endsWith("/" + compilationUnitName));
  }
}
