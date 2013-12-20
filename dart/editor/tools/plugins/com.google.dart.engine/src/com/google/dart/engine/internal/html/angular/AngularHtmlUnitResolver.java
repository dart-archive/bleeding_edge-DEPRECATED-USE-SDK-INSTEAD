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

package com.google.dart.engine.internal.html.angular;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.ProxyConditionalAnalysisError;
import com.google.dart.engine.internal.resolver.ResolverVisitor;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * Instances of the class {@link AngularHtmlUnitResolver} resolve Angular specific expressions.
 */
public class AngularHtmlUnitResolver extends RecursiveXmlVisitor<Void> {
  private static final String NG_APP = "ng-app";
  private static final String NG_BOOTSTRAP = "ngBootstrap";
  private static final String NG_CONTROLLER = "NgController";
  private static final String ATTR_SELECTOR = "selector";
  private static final String ATTR_PUBLISH_AS = "publishAs";

  /**
   * Returns {@link InjectSelector} for the given CSS selector string.
   */
  @VisibleForTesting
  public static InjectSelector createInjectSelector(String text) {
    if (text.startsWith("[") && text.endsWith("]")) {
      return new AttributeInjectSelector(text.substring(1, text.length() - 1));
    }
    return null;
  }

  /**
   * @return {@code true} if the given {@link HtmlUnit} has <code>ng-app</code> annotation.
   */
  public static boolean hasAngularAnnotation(HtmlUnit htmlUnit) {
    class FoundAppError extends Error {
    }
    try {
      htmlUnit.accept(new RecursiveXmlVisitor<Void>() {
        @Override
        public Void visitXmlTagNode(XmlTagNode node) {
          if (node.getAttribute(NG_APP) != null) {
            throw new FoundAppError();
          }
          return super.visitXmlTagNode(node);
        }
      });
    } catch (FoundAppError e) {
      return true;
    }
    return false;
  }

  private static SimpleIdentifier createIdentifier(String name) {
    StringToken token = createStringToken(name);
    return new SimpleIdentifier(token);
  }

  private static StringToken createStringToken(String name) {
    return new StringToken(TokenType.IDENTIFIER, name, 0);
  }

  /**
   * If given {@link Expression} is a {@link NamedExpression} and its name is the same as given,
   * then returns its "value" {@link Expression}. Otherwise returns {@code null}.
   */
  private static Expression getNamedExpression(Expression expression, String name) {
    if (expression instanceof NamedExpression) {
      NamedExpression namedExpression = (NamedExpression) expression;
      String actualName = namedExpression.getName().getLabel().getName();
      if (actualName.equals(name)) {
        return namedExpression.getExpression();
      }
    }
    return null;
  }

  /**
   * Returns {@code true} if given {@link Annotation} is <code>NgController</code>.
   */
  private static boolean isNgController(Annotation annotation) {
    return annotation.getName().getName().equals(NG_CONTROLLER);
  }

  /**
   * Returns the existing {@link Expression} or new one from the {@link NamedExpression} with the
   * given name.
   */
  private static Expression updateNamedArgument(Expression existing, Expression argument,
      String name) {
    if (existing != null) {
      return existing;
    }
    return getNamedExpression(argument, name);
  }

  private final InternalAnalysisContext context;
  private final TypeProvider typeProvider;
  private final AnalysisErrorListener errorListener;
  private final Source source;
  private final List<NgAnnotation> controllers = Lists.newArrayList();
  private CompilationUnitElementImpl unitElement;
  private LibraryElementImpl libraryElement;
  private ResolverVisitor resolver;
  private List<TopLevelVariableElementImpl> declaredVariables = Lists.newArrayList();
  private boolean isAngular = false;

  private AnalysisException thrownException;

  public AngularHtmlUnitResolver(InternalAnalysisContext context,
      AnalysisErrorListener errorListener, Source source) throws AnalysisException {
    this.context = context;
    this.typeProvider = context.getTypeProvider();
    this.errorListener = errorListener;
    this.source = source;
  }

  /**
   * Resolves Angular specific expressions and elements.
   * 
   * @param unit the {@link HtmlUnit} to resolve
   */
  public void resolve(HtmlUnit unit) throws AnalysisException {
    // check if Angular at all
    if (!hasAngularAnnotation(unit)) {
      return;
    }
    // prepare injects
    boolean success = prepareInjects(unit);
    if (!success) {
      return;
    }
    // prepare Dart library
    createLibraryElement();
    unit.setCompilationUnitElement(libraryElement.getDefiningCompilationUnit());
    // prepare Dart resolver
    createResolver();
    unitElement.setTopLevelVariables(declaredVariables.toArray(new TopLevelVariableElementImpl[declaredVariables.size()]));
    // run this HTML visitor
    unit.accept(this);
    // push conditional errors 
    for (ProxyConditionalAnalysisError conditionalCode : resolver.getProxyConditionalAnalysisErrors()) {
      resolver.reportError(conditionalCode.getAnalysisError());
    }
    // check for recorded exception
    if (thrownException != null) {
      throw thrownException;
    }
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    boolean wasAngular = isAngular;
    try {
      isAngular |= node.getAttribute(NG_APP) != null;
      // not Angular
      if (!isAngular) {
        return super.visitXmlTagNode(node);
      }
      // process children
      Scope nameScope = null;
      try {
        // inject controllers
        for (NgAnnotation controller : controllers) {
          if (controller.getSelector().apply(node)) {
            VariableElement controllerVar = createTopLevelVariable(
                controller.getElement(),
                controller.getName());
            if (nameScope == null) {
              nameScope = resolver.pushNameScope();
            }
            nameScope.define(controllerVar);
          }
        }
        // resolve expressions
        for (EmbeddedExpression embeddedExpression : node.getExpressions()) {
          Expression expression = embeddedExpression.getExpression();
          resolveExpression(expression);
        }
        // process children
        return super.visitXmlTagNode(node);
      } finally {
        if (nameScope != null) {
          resolver.popNameScope();
        }
      }
    } finally {
      isAngular = wasAngular;
    }
  }

  private NgController createController(ClassDeclaration injectedClass) {
    List<Annotation> annotations = injectedClass.getMetadata();
    for (Annotation annotation : annotations) {
      if (isNgController(annotation)) {
        ArgumentList argumentList = annotation.getArguments();
        if (argumentList != null) {
          Expression selectorExpression = null;
          Expression nameExpression = null;
          for (Expression argument : argumentList.getArguments()) {
            selectorExpression = updateNamedArgument(selectorExpression, argument, ATTR_SELECTOR);
            nameExpression = updateNamedArgument(nameExpression, argument, ATTR_PUBLISH_AS);
          }
          if (selectorExpression instanceof SimpleStringLiteral
              && nameExpression instanceof SimpleStringLiteral) {
            String selectorText = ((SimpleStringLiteral) selectorExpression).getValue();
            InjectSelector selector = createInjectSelector(selectorText);
            String name = ((SimpleStringLiteral) nameExpression).getValue();
            if (selector != null && name != null) {
              return new NgController(injectedClass.getElement(), selector, name);
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Puts into {@link #libraryElement} an artificial {@link LibraryElementImpl} for this HTML
   * {@link Source}.
   */
  private void createLibraryElement() {
    // create CompilationUnitElementImpl
    String unitName = source.getShortName();
    unitElement = new CompilationUnitElementImpl(unitName);
    unitElement.setSource(source);
    // create LibraryElementImpl
    libraryElement = new LibraryElementImpl(context, null);
    libraryElement.setDefiningCompilationUnit(unitElement);
  }

  /**
   * Puts into {@link #resolver} an {@link ResolverVisitor} to resolve {@link Expression}s in
   * {@link #source}.
   */
  private void createResolver() {
    InheritanceManager inheritanceManager = new InheritanceManager(libraryElement);
    resolver = new ResolverVisitor(
        libraryElement,
        source,
        typeProvider,
        inheritanceManager,
        errorListener);
  }

  /**
   * Creates new {@link TopLevelVariableElementImpl} with given name and type.
   * 
   * @param classElement the {@link ClassElement} to create type
   * @param name the name of the variable
   * @return the new {@link TopLevelVariableElementImpl}
   */
  private TopLevelVariableElementImpl createTopLevelVariable(ClassElement classElement, String name) {
    SimpleIdentifier identifier = createIdentifier(name);
    TopLevelVariableElementImpl variable = new TopLevelVariableElementImpl(identifier);
    declaredVariables.add(variable);
    variable.setType(classElement.getType());
    return variable;
  }

  /**
   * Returns the only invocation of Angular's <code>ngBootstrap</code>.
   */
  private MethodInvocation getBootstrapInvocation(CompilationUnit dartUnit) {
    final MethodInvocation[] result = {null};
    dartUnit.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        if (node.getMethodName().getName().equals(NG_BOOTSTRAP)) {
          result[0] = node;
        }
        return null;
      }
    });
    return result[0];
  }

  /**
   * Returns the external Dart {@link CompilationUnit} referenced by the given {@link HtmlUnit}.
   */
  private CompilationUnit getDartUnit(HtmlUnit unit) throws AnalysisException {
    for (HtmlScriptElement script : unit.getElement().getScripts()) {
      if (script instanceof ExternalHtmlScriptElement) {
        Source scriptSource = ((ExternalHtmlScriptElement) script).getScriptSource();
        if (scriptSource != null) {
          return context.resolveCompilationUnit(scriptSource, scriptSource);
        }
      }
    }
    return null;
  }

  /**
   * Returns {@link ClassDeclaration} of classes injected into given modules.
   */
  private List<ClassDeclaration> getInjectedClasses(ClassDeclaration module) {
    final List<ClassDeclaration> injected = Lists.newArrayList();
    module.accept(new RecursiveASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        List<Expression> arguments = node.getArgumentList().getArguments();
        if (node.getMethodName().getName().equals("type") && arguments.size() == 1) {
          Expression argument = arguments.get(0);
          if (argument instanceof Identifier) {
            Element injectElement = ((Identifier) argument).getStaticElement();
            if (injectElement instanceof ClassElement) {
              try {
                ClassDeclaration injectedClass = ((ClassElement) injectElement).getNode();
                injected.add(injectedClass);
              } catch (AnalysisException e) {
                thrownException = e;
              }
            }
          }
        }
        return super.visitMethodInvocation(node);
      }
    });
    return injected;
  }

  /**
   * Finds all Angular modules references by the given <code>ngBootstrap</code> invocation.
   * 
   * @param bootInvocation the <code>ngBootstrap</code> invocation
   * @return the modules {@link ClassDeclaration}s
   */
  private List<ClassDeclaration> getModules(MethodInvocation bootInvocation)
      throws AnalysisException {
    List<ClassDeclaration> modules = Lists.newArrayList();
    List<Expression> arguments = bootInvocation.getArgumentList().getArguments();
    for (Expression argument : arguments) {
      // TODO(scheglov) limitation - only one module, add support for "modules"
      {
        Expression moduleExpression = getNamedExpression(argument, "module");
        Element element = moduleExpression.getBestType().getElement();
        if (element instanceof ClassElement) {
          ClassElement moduleElement = (ClassElement) element;
          ClassDeclaration moduleClass = moduleElement.getNode();
          if (moduleClass != null) {
            modules.add(moduleClass);
          }
        }
      }
    }
    return modules;
  }

  /**
   * Analyzes given {@link HtmlUnit} and fills {@link #controllers}.
   * 
   * @param unit the {@link HtmlUnit} to analyze
   * @return {@code true} if analysis for successful or {@code false} otherwise.
   */
  private boolean prepareInjects(HtmlUnit unit) throws AnalysisException {
    // prepare CompilationUnit
    CompilationUnit dartUnit = getDartUnit(unit);
    if (dartUnit == null) {
      return false;
    }
    // find "ngBootstrap" invocation
    MethodInvocation bootInvocation = getBootstrapInvocation(dartUnit);
    if (bootInvocation == null) {
      return false;
    }
    // prepare modules
    List<ClassDeclaration> modules = getModules(bootInvocation);
    for (ClassDeclaration module : modules) {
      // prepare injected classes
      List<ClassDeclaration> injectedClasses = getInjectedClasses(module);
      // prepare controllers
      for (ClassDeclaration injectedClass : injectedClasses) {
        NgController controller = createController(injectedClass);
        if (controller != null) {
          controllers.add(controller);
        }
      }
    }
    // OK
    return true;
  }

  /**
   * Resolves given {@link Expression} using {@link #resolver}.
   */
  private void resolveExpression(Expression expression) {
    expression.accept(resolver);
  }
}
