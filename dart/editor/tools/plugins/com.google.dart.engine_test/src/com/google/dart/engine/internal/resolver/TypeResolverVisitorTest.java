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
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.AstFactory.catchClause;
import static com.google.dart.engine.ast.AstFactory.classDeclaration;
import static com.google.dart.engine.ast.AstFactory.classTypeAlias;
import static com.google.dart.engine.ast.AstFactory.extendsClause;
import static com.google.dart.engine.ast.AstFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.implementsClause;
import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.ast.AstFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationList;
import static com.google.dart.engine.ast.AstFactory.withClause;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.requiredParameter;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class TypeResolverVisitorTest extends EngineTestCase {
  /**
   * The error listener to which errors will be reported.
   */
  private GatheringErrorListener listener;

  /**
   * The object representing the information about the library in which the types are being
   * resolved.
   */
  private Library library;

  /**
   * The type provider used to access the types.
   */
  private TestTypeProvider typeProvider;

  /**
   * The visitor used to resolve types needed to form the type hierarchy.
   */
  private TypeResolverVisitor visitor;

  public void fail_visitConstructorDeclaration() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitFunctionDeclaration() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitFunctionTypeAlias() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitFunctionTypedFormalParameter() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitMethodDeclaration() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitVariableDeclaration() throws Exception {
    fail("Not yet tested");
    ClassElement type = classElement("A");
    VariableDeclaration node = variableDeclaration("a");
    variableDeclarationList(null, typeName(type), node);
    //resolve(node);
    assertSame(type.getType(), node.getName().getStaticType());
    listener.assertNoErrors();
  }

  @Override
  public void setUp() {
    listener = new GatheringErrorListener();
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.setSourceFactory(factory);
    Source librarySource = new FileBasedSource(createFile("/lib.dart"));
    library = new Library(context, listener, librarySource);
    LibraryElementImpl element = new LibraryElementImpl(context, libraryIdentifier("lib"));
    element.setDefiningCompilationUnit(new CompilationUnitElementImpl("lib.dart"));
    library.setLibraryElement(element);
    typeProvider = new TestTypeProvider();
    visitor = new TypeResolverVisitor(library, librarySource, typeProvider);
  }

  public void test_visitCatchClause_exception() throws Exception {
    // catch (e)
    CatchClause clause = catchClause("e");
    SimpleIdentifier exceptionParameter = clause.getExceptionParameter();
    exceptionParameter.setStaticElement(new LocalVariableElementImpl(exceptionParameter));
    resolveCatchClause(clause, typeProvider.getDynamicType(), null);
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_exception_stackTrace() throws Exception {
    // catch (e, s)
    CatchClause clause = catchClause("e", "s");
    SimpleIdentifier exceptionParameter = clause.getExceptionParameter();
    exceptionParameter.setStaticElement(new LocalVariableElementImpl(exceptionParameter));
    SimpleIdentifier stackTraceParameter = clause.getStackTraceParameter();
    stackTraceParameter.setStaticElement(new LocalVariableElementImpl(stackTraceParameter));
    resolveCatchClause(clause, typeProvider.getDynamicType(), typeProvider.getStackTraceType());
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_on_exception() throws Exception {
    // on E catch (e)
    ClassElement exceptionElement = classElement("E");
    TypeName exceptionType = typeName(exceptionElement);
    CatchClause clause = catchClause(exceptionType, "e");
    SimpleIdentifier exceptionParameter = clause.getExceptionParameter();
    exceptionParameter.setStaticElement(new LocalVariableElementImpl(exceptionParameter));
    resolveCatchClause(clause, exceptionElement.getType(), null, exceptionElement);
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_on_exception_stackTrace() throws Exception {
    // on E catch (e, s)
    ClassElement exceptionElement = classElement("E");
    TypeName exceptionType = typeName(exceptionElement);
    ((SimpleIdentifier) exceptionType.getName()).setStaticElement(exceptionElement);
    CatchClause clause = catchClause(exceptionType, "e", "s");
    SimpleIdentifier exceptionParameter = clause.getExceptionParameter();
    exceptionParameter.setStaticElement(new LocalVariableElementImpl(exceptionParameter));
    SimpleIdentifier stackTraceParameter = clause.getStackTraceParameter();
    stackTraceParameter.setStaticElement(new LocalVariableElementImpl(stackTraceParameter));
    resolveCatchClause(
        clause,
        exceptionElement.getType(),
        typeProvider.getStackTraceType(),
        exceptionElement);
    listener.assertNoErrors();
  }

  public void test_visitClassDeclaration() {
    // class A extends B with C implements D {}
    // class B {}
    // class C {}
    // class D {}
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
    declaration.getName().setStaticElement(elementA);

    resolveNode(declaration, elementA, elementB, elementC, elementD);
    assertSame(elementB.getType(), elementA.getSupertype());
    InterfaceType[] mixins = elementA.getMixins();
    assertLength(1, mixins);
    assertSame(elementC.getType(), mixins[0]);
    InterfaceType[] interfaces = elementA.getInterfaces();
    assertLength(1, interfaces);
    assertSame(elementD.getType(), interfaces[0]);
    listener.assertNoErrors();
  }

  public void test_visitClassDeclaration_instanceMemberCollidesWithClass() {
    // class A {}
    // class B extends A {
    //   void A() {}
    // }
    ClassElementImpl elementA = classElement("A");
    ClassElementImpl elementB = classElement("B");
    elementB.setMethods(new MethodElement[] {methodElement("A", VoidTypeImpl.getInstance())});

    ExtendsClause extendsClause = extendsClause(typeName(elementA));
    ClassDeclaration declaration = classDeclaration(null, "B", null, extendsClause, null, null);
    declaration.getName().setStaticElement(elementB);

    resolveNode(declaration, elementA, elementB);
    assertSame(elementA.getType(), elementB.getSupertype());
    listener.assertNoErrors();
  }

  public void test_visitClassTypeAlias() {
    // class A = B with C implements D;
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
    alias.getName().setStaticElement(elementA);

    resolveNode(alias, elementA, elementB, elementC, elementD);
    assertSame(elementB.getType(), elementA.getSupertype());
    InterfaceType[] mixins = elementA.getMixins();
    assertLength(1, mixins);
    assertSame(elementC.getType(), mixins[0]);
    InterfaceType[] interfaces = elementA.getInterfaces();
    assertLength(1, interfaces);
    assertSame(elementD.getType(), interfaces[0]);
    listener.assertNoErrors();
  }

  public void test_visitFieldFormalParameter_functionType() throws Exception {
    InterfaceType intType = typeProvider.getIntType();
    TypeName intTypeName = typeName("int");
    String innerParameterName = "a";
    SimpleFormalParameter parameter = simpleFormalParameter(innerParameterName);
    parameter.getIdentifier().setStaticElement(requiredParameter(innerParameterName));
    String outerParameterName = "p";
    FormalParameter node = fieldFormalParameter(
        null,
        intTypeName,
        outerParameterName,
        formalParameterList(parameter));
    node.getIdentifier().setStaticElement(requiredParameter(outerParameterName));
    Type parameterType = resolveFormalParameter(node, intType.getElement());
    assertInstanceOf(FunctionType.class, parameterType);
    FunctionType functionType = (FunctionType) parameterType;
    assertSame(intType, functionType.getReturnType());
    assertLength(1, functionType.getParameters());
    listener.assertNoErrors();
  }

  public void test_visitFieldFormalParameter_noType() throws Exception {
    String parameterName = "p";
    FormalParameter node = fieldFormalParameter(Keyword.VAR, null, parameterName);
    node.getIdentifier().setStaticElement(requiredParameter(parameterName));
    assertSame(typeProvider.getDynamicType(), resolveFormalParameter(node));
    listener.assertNoErrors();
  }

  public void test_visitFieldFormalParameter_type() throws Exception {
    InterfaceType intType = typeProvider.getIntType();
    TypeName intTypeName = typeName("int");
    String parameterName = "p";
    FormalParameter node = fieldFormalParameter(null, intTypeName, parameterName);
    node.getIdentifier().setStaticElement(requiredParameter(parameterName));
    assertSame(intType, resolveFormalParameter(node, intType.getElement()));
    listener.assertNoErrors();
  }

  public void test_visitSimpleFormalParameter_noType() throws Exception {
    // p
    FormalParameter node = simpleFormalParameter("p");
    node.getIdentifier().setStaticElement(new ParameterElementImpl(identifier("p")));
    assertSame(typeProvider.getDynamicType(), resolveFormalParameter(node));
    listener.assertNoErrors();
  }

  public void test_visitSimpleFormalParameter_type() throws Exception {
    // int p
    InterfaceType intType = typeProvider.getIntType();
    ClassElement intElement = intType.getElement();
    FormalParameter node = simpleFormalParameter(typeName(intElement), "p");
    SimpleIdentifier identifier = node.getIdentifier();
    ParameterElementImpl element = new ParameterElementImpl(identifier);
    identifier.setStaticElement(element);
    assertSame(intType, resolveFormalParameter(node, intElement));
    listener.assertNoErrors();
  }

  public void test_visitTypeName_noParameters_noArguments() throws Exception {
    ClassElement classA = classElement("A");
    TypeName typeName = typeName(classA);
    typeName.setType(null); // The factory method sets the type, but we want the resolver to do so.
    resolveNode(typeName, classA);
    assertSame(classA.getType(), typeName.getType());
    listener.assertNoErrors();
  }

  public void test_visitTypeName_parameters_arguments() throws Exception {
    ClassElement classA = classElement("A", "E");
    ClassElement classB = classElement("B");
    TypeName typeName = typeName(classA, typeName(classB));
    typeName.setType(null); // The factory method sets the type, but we want the resolver to do so.
    resolveNode(typeName, classA, classB);
    InterfaceType resultType = (InterfaceType) typeName.getType();
    assertSame(classA, resultType.getElement());
    Type[] resultArguments = resultType.getTypeArguments();
    assertLength(1, resultArguments);
    assertSame(classB.getType(), resultArguments[0]);
    listener.assertNoErrors();
  }

  public void test_visitTypeName_parameters_noArguments() throws Exception {
    ClassElement classA = classElement("A", "E");
    TypeName typeName = typeName(classA);
    typeName.setType(null); // The factory method sets the type, but we want the resolver to do so.
    resolveNode(typeName, classA);
    InterfaceType resultType = (InterfaceType) typeName.getType();
    assertSame(classA, resultType.getElement());
    Type[] resultArguments = resultType.getTypeArguments();
    assertLength(1, resultArguments);
    assertSame(DynamicTypeImpl.getInstance(), resultArguments[0]);
    listener.assertNoErrors();
  }

  public void test_visitTypeName_void() throws Exception {
    ClassElement classA = classElement("A");
    TypeName typeName = typeName("void");
    resolveNode(typeName, classA);
    assertSame(VoidTypeImpl.getInstance(), typeName.getType());
    listener.assertNoErrors();
  }

  /**
   * Analyze the given catch clause and assert that the types of the parameters have been set to the
   * given types. The types can be null if the catch clause does not have the corresponding
   * parameter.
   * 
   * @param node the catch clause to be analyzed
   * @param exceptionType the expected type of the exception parameter
   * @param stackTraceType the expected type of the stack trace parameter
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   */
  private void resolveCatchClause(CatchClause node, Type exceptionType,
      InterfaceType stackTraceType, Element... definedElements) {
    resolveNode(node, definedElements);
    SimpleIdentifier exceptionParameter = node.getExceptionParameter();
    if (exceptionParameter != null) {
      assertSame(exceptionType, exceptionParameter.getStaticType());
    }
    SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
    if (stackTraceParameter != null) {
      assertSame(stackTraceType, stackTraceParameter.getStaticType());
    }
  }

  /**
   * Return the type associated with the given parameter after the static type analyzer has computed
   * a type for it.
   * 
   * @param node the parameter with which the type is associated
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the type associated with the parameter
   */
  private Type resolveFormalParameter(FormalParameter node, Element... definedElements) {
    resolveNode(node, definedElements);
    return ((ParameterElement) node.getIdentifier().getStaticElement()).getType();
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
  private void resolveNode(AstNode node, Element... definedElements) {
    for (Element element : definedElements) {
      library.getLibraryScope().define(element);
    }
    node.accept(visitor);
  }
}
