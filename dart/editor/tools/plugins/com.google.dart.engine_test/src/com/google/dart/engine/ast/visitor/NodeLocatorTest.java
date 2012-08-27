/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.parser.ParserTestCase;

public class NodeLocatorTest extends ParserTestCase {
  public void test_offset() throws Exception {
    CompilationUnit unit = parseCompilationUnit("library myLib;");
    assertLocate(unit, 10, SimpleIdentifier.class);
  }

  public void test_range() throws Exception {
    CompilationUnit unit = parseCompilationUnit("library myLib;");
    assertLocate(unit, 4, 10, LibraryDirective.class);
  }

  private void assertLocate(CompilationUnit unit, int offset, Class<?> expectedClass)
      throws Exception {
    assertLocate(unit, offset, offset, expectedClass);
  }

  private void assertLocate(CompilationUnit unit, int start, int end, Class<?> expectedClass)
      throws Exception {
    NodeLocator locator = new NodeLocator(start, end);
    ASTNode node = locator.searchWithin(unit);
    assertNotNull(node);
    assertTrue("Node starts after range", node.getOffset() <= start);
    assertTrue("Node ends before range", node.getOffset() + node.getLength() > end);
    assertInstanceOf(expectedClass, node);
  }
}
