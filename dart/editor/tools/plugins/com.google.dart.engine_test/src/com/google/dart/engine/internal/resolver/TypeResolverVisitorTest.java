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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.ast.ASTFactory.classDeclaration;
import static com.google.dart.engine.ast.ASTFactory.classTypeAlias;
import static com.google.dart.engine.ast.ASTFactory.extendsClause;
import static com.google.dart.engine.ast.ASTFactory.implementsClause;
import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;
import static com.google.dart.engine.ast.ASTFactory.typeName;
import static com.google.dart.engine.ast.ASTFactory.withClause;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class TypeResolverVisitorTest extends EngineTestCase {
  /**
   * The error listener to which errors will be reported.
   */
  private GatheringErrorListener listener;

  private Library library;

  /**
   * The visitor used to resolve types needed to form the type hierarchy.
   */
  private TypeResolverVisitor visitor;

  @Override
  public void setUp() {
    listener = new GatheringErrorListener();
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.setSourceFactory(factory);
    Source librarySource = factory.forFile(createFile("/lib.dart"));
    library = new Library(context, listener, librarySource);
    LibraryElementImpl element = new LibraryElementImpl(context, libraryIdentifier("lib"));
    element.setDefiningCompilationUnit(new CompilationUnitElementImpl("lib.dart"));
    library.setLibraryElement(element);
    visitor = new TypeResolverVisitor(library, librarySource, new TestTypeProvider());
  }

  public void test_visitClassDeclaration() {
    // class A extends B with C implements D {}
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B");
    ClassElement elementC = classElement("C");
    ClassElement elementD = classElement("D");
    ExtendsClause extendsClause = extendsClause(typeName(elementB));
    WithClause withClause = withClause(typeName(elementC));
    ImplementsClause implementsClause = implementsClause(typeName(elementD));
    ClassDeclaration declaration = classDeclaration(
        null,
        "A",
        null,
        extendsClause,
        withClause,
        implementsClause);
    declaration.getName().setElement(elementA);

    resolveNode(declaration, elementA, elementB, elementC, elementD);
    assertSame(elementB.getType(), elementA.getSupertype());
    InterfaceType[] mixins = elementA.getMixins();
    assertLength(1, mixins);
    assertSame(elementC.getType(), mixins[0]);
    InterfaceType[] interfaces = elementA.getInterfaces();
    assertLength(1, interfaces);
    assertSame(elementD.getType(), interfaces[0]);
  }

  public void test_visitClassTypeAlias() {
    // typedef A = B with C implements D;
    ClassElement elementA = classElement("A");
    ClassElement elementB = classElement("B");
    ClassElement elementC = classElement("C");
    ClassElement elementD = classElement("D");
    WithClause withClause = withClause(typeName(elementC));
    ImplementsClause implementsClause = implementsClause(typeName(elementD));
    ClassTypeAlias alias = classTypeAlias(
        "A",
        null,
        null,
        typeName(elementB),
        withClause,
        implementsClause);
    alias.getName().setElement(elementA);

    resolveNode(alias, elementA, elementB, elementC, elementD);
    assertSame(elementB.getType(), elementA.getSupertype());
    InterfaceType[] mixins = elementA.getMixins();
    assertLength(1, mixins);
    assertSame(elementC.getType(), mixins[0]);
    InterfaceType[] interfaces = elementA.getInterfaces();
    assertLength(1, interfaces);
    assertSame(elementD.getType(), interfaces[0]);
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   * 
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  private void resolveNode(ASTNode node, Element... definedElements) {
    for (Element element : definedElements) {
      library.getLibraryScope().define(element);
    }
    node.accept(visitor);
  }
}
