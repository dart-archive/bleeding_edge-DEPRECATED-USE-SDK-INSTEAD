/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.util;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyCompilationUnit;

import junit.framework.TestCase;

public class ASTCacheTest extends TestCase {
  public void test_ASTCache_flush() {
    ASTCache cache = new ASTCache();
    cache.flush();
  }

  public void xtest_ASTCache_getAST() throws Exception {
    CompilationUnit compilationUnit = getMoneyCompilationUnit("currency.dart");
    ASTCache cache = new ASTCache();
    DartUnit ast = cache.getAST(compilationUnit);
    assertNotNull(ast);
  }
}
