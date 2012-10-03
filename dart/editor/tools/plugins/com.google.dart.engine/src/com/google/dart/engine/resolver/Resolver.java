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
package com.google.dart.engine.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.resolver.scope.LibraryScope;
import com.google.dart.engine.source.Source;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code Resolver} resolve references within the AST structure to the
 * elements being referenced. The requirements for the resolver are
 * <ul>
 * <li>Every {@link SimpleIdentifier} should be resolved to the element to which it refers.
 * Specifically:
 * <ul>
 * <li>An identifier within the declaration of that name should resolve to the element being
 * declared.</li>
 * <li>An identifier denoting a prefix should resolve to the element representing the prefix.</li>
 * <li>An identifier denoting a variable (including parameters) should resolve to the element
 * representing the variable.</li>
 * <li>An identifier denoting a field should resolve to the element representing the getter or
 * setter being invoked.</li>
 * <li>An identifier denoting the name of a method or function being invoked should resolve to the
 * element representing the method or function.</li>
 * <li>An identifier denoting a label should resolve to the element representing the label.</li>
 * </ul>
 * The identifiers within directives are exceptions to this rule and are covered below.</li>
 * <li>Every node containing a token representing an operator that can be overridden (
 * {@link BinaryExpression}, {@link PrefixExpression}, {@link PostfixExpression}) should resolve to
 * the element representing the method invoked by that operator.</li>
 * <li>Every node representing the invocation of a method or function, including unnamed functions,
 * should resolve to the element representing the method or function being invoked. This will be the
 * same element as that to which the name is resolved if the method or function has a name, but is
 * provided for those cases where an unnamed function is being invoked.</li>
 * <li>Every {@link LibraryDirective} and {@link PartOfDirective} should resolve to the element
 * representing the library being specified by the directive.</li>
 * <li>The {@link StringLiteral} representing the URI of a library in an {@link ImportDirective}
 * should resolve to the element representing the library being specified by the string.</li>
 * <li>The identifier representing the prefix in an {@link ImportDirective} should resolve to the
 * element representing the prefix.</li>
 * <li>The identifiers in the hide and show combinators in an {@link ImportDirective} should resolve
 * to the elements that are being hidden or shown, respectively.</li>
 * <li>The {@link StringLiteral} representing the URI of a part in a {@link PartDirective} should
 * resolve to the element representing the compilation unit being specified by the string.</li>
 * </ul>
 * Note that AST nodes that would represent elements that have not been defined are not resolved to
 * anything. This includes such things as references to undeclared variables (which is an error) and
 * names in a hide combinator that are not defined in the imported library (which is not an error).
 */
public class Resolver {
  /**
   * The element for the library containing the compilation units that can be resolved.
   */
  private LibraryElement definingLibrary;

  /**
   * The name scope for the library containing the compilation units that can be resolved.
   */
  private LibraryScope libraryScope;

  /**
   * A table mapping the identifiers of declared elements to the element that was declared.
   */
  private Map<ASTNode, Element> declaredElementMap;

  /**
   * A table mapping the AST nodes that have been resolved to the element to which they were
   * resolved.
   */
  private Map<ASTNode, Element> resolvedElementMap;

  /**
   * @param definingLibrary the element for the library within which resolution is being performed
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   * @param declaredElementMap a table mapping the identifiers of declared elements to the element
   *          that was declared
   */
  public Resolver(LibraryElement definingLibrary, AnalysisErrorListener errorListener,
      Map<ASTNode, Element> declaredElementMap) {
    this.definingLibrary = definingLibrary;
    libraryScope = new LibraryScope(definingLibrary, errorListener);
    this.declaredElementMap = declaredElementMap;
    resolvedElementMap = new HashMap<ASTNode, Element>();
  }

  /**
   * Return a table mapping the AST nodes that have been resolved to the element to which they were
   * resolved.
   * 
   * @return a table mapping the AST nodes that have been resolved to the element to which they were
   *         resolved
   */
  public Map<ASTNode, Element> getResolvedElementMap() {
    return resolvedElementMap;
  }

  /**
   * Resolve the given compilation unit. The compilation unit is assumed to be defined in the
   * library associated with this resolver.
   * 
   * @param source the source representing the compilation unit being visited
   * @param unit the compilation unit to be resolved
   */
  public void resolve(Source source, CompilationUnit unit) {
    ResolverVisitor visitor = new ResolverVisitor(
        definingLibrary,
        source,
        libraryScope,
        declaredElementMap,
        resolvedElementMap);
    unit.accept(visitor);
  }
}
