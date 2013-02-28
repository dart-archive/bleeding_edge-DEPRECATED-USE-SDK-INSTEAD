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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

public class ElementLocatorTest extends ResolverTestCase {

  private static final String SRC_FILE_NAME = "/test.dart";

  public void test_ClassElement() throws Exception {
    ASTNode id = findNodeIn("A", "class A { }");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(ClassElement.class, element);
  }

  /**
   * @param nodePattern the (unique) pattern used to identify the node of interest
   * @param lines the lines to be merged into a single source string
   * @return the matched node in the resolved AST for the given source lines
   * @throws Exception if source cannot be verified
   */
  private ASTNode findNodeIn(String nodePattern, String... lines) throws Exception {
    String contents = createSource(lines);
    CompilationUnit cu = resolve(contents);
    return new NodeLocator(contents.indexOf(nodePattern), nodePattern.length()).searchWithin(cu);
  }

  /**
   * Parse, resolve and verify the given source lines to produce a fully resolved AST.
   * 
   * @param lines the lines to be merged into a single source string
   * @return the result of resolving the AST structure representing the content of the source
   * @throws Exception if source cannot be verified
   */
  private CompilationUnit resolve(String... lines) throws Exception {
    Source source = addSource(SRC_FILE_NAME, createSource(lines));
    LibraryElement library = resolve(source);
    resolve(source);
    assertNoErrors();
    verify(source);
    return getAnalysisContext().resolve(source, library);
  }

}
