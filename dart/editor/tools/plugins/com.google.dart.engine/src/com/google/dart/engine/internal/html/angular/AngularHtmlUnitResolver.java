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
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularModuleElement;
import com.google.dart.engine.element.angular.AngularSelector;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.angular.HasAttributeSelector;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.ProxyConditionalAnalysisError;
import com.google.dart.engine.internal.resolver.ResolverVisitor;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class {@link AngularHtmlUnitResolver} resolve Angular specific expressions.
 */
public class AngularHtmlUnitResolver extends RecursiveXmlVisitor<Void> {
  private static final String OPENING_DELIMITER = "{{";
  private static final String CLOSING_DELIMITER = "}}";
  private static final int OPENING_DELIMITER_LENGTH = OPENING_DELIMITER.length();
  private static final int CLOSING_DELIMITER_LENGTH = CLOSING_DELIMITER.length();

  private static final String NG_APP = "ng-app";
  private static final String NG_BOOTSTRAP = "ngBootstrap";
  private static final String NG_CONTROLLER = "NgController";
  private static final String NG_DIRECTIVE = "NgDirective";
  private static final String ATTR_SELECTOR = "selector";
  private static final String ATTR_PUBLISH_AS = "publishAs";

  /**
   * Returns {@link AngularSelector} for the given CSS selector string.
   */
  @VisibleForTesting
  public static AngularSelector createInjectSelector(String text) {
    if (text.startsWith("[") && text.endsWith("]")) {
      return new HasAttributeSelector(text.substring(1, text.length() - 1));
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

  static SimpleIdentifier createIdentifier(String name) {
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
   * Returns {@code true} if given {@link Annotation} is <code>NgDirective</code>.
   */
  private static boolean isNgDirective(Annotation annotation) {
    return annotation.getName().getName().equals(NG_DIRECTIVE);
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
  private final LineInfo lineInfo;
  private final List<NgAnnotation> controllers = Lists.newArrayList();

  private LibraryElementImpl libraryElement;
  private CompilationUnitElementImpl unitElement;
  private FunctionElementImpl functionElement;
  private ResolverVisitor resolver;

  private boolean isAngular = false;
  private List<LocalVariableElementImpl> definedVariables = Lists.newArrayList();
  private Set<LibraryElement> injectedLibraries = Sets.newHashSet();
  private Scope topNameScope;
  private Scope nameScope;

  private AnalysisException thrownException;

  public AngularHtmlUnitResolver(InternalAnalysisContext context,
      AnalysisErrorListener errorListener, Source source, LineInfo lineInfo)
      throws AnalysisException {
    this.context = context;
    this.lineInfo = lineInfo;
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
    // add built-in injects
    controllers.add(NgModelDirective.INSTANCE);
    controllers.add(NgRepeatDirective.INSTANCE);
    // prepare Dart library
    createLibraryElement();
    unit.setCompilationUnitElement(libraryElement.getDefiningCompilationUnit());
    // prepare Dart resolver
    createResolver();
    // run this HTML visitor
    unit.accept(this);
    functionElement.setLocalVariables(definedVariables.toArray(new LocalVariableElementImpl[definedVariables.size()]));
    // simulate imports for injects
    {
      List<ImportElement> imports = Lists.newArrayList();
      for (LibraryElement injectedLibrary : injectedLibraries) {
        ImportElementImpl importElement = new ImportElementImpl(-1);
        importElement.setImportedLibrary(injectedLibrary);
        imports.add(importElement);
      }
      libraryElement.setImports(imports.toArray(new ImportElement[imports.size()]));
    }
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
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    if (!isProcessedByDirective(node)) {
      parseEmbeddedExpressions(node);
      resolveExpressions(node.getExpressions());
    }
    return super.visitXmlAttributeNode(node);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    boolean wasAngular = isAngular;
    try {
      // new Angular context
      if (node.getAttribute(NG_APP) != null) {
        isAngular = true;
        visitModelDirectives(node);
      }
      // not Angular
      if (!isAngular) {
        return super.visitXmlTagNode(node);
      }
      // process node in separate name scope
      nameScope = resolver.pushNameScope();
      try {
        parseEmbeddedExpressions(node);
        // inject controllers
        for (NgAnnotation controller : controllers) {
          if (controller.getSelector().apply(node)) {
            controller.apply(this, node);
          }
        }
        // resolve expressions
        resolveExpressions(node.getExpressions());
        // process children
        return super.visitXmlTagNode(node);
      } finally {
        resolver.popNameScope();
      }
    } finally {
      isAngular = wasAngular;
    }
  }

  /**
   * Creates new {@link LocalVariableElementImpl} with given type and identifier.
   * 
   * @param type the {@link Type} of the variable
   * @param identifier the identifier to create variable for
   * @return the new {@link LocalVariableElementImpl}
   */
  LocalVariableElementImpl createLocalVariable(Type type, SimpleIdentifier identifier) {
    LocalVariableElementImpl variable = new LocalVariableElementImpl(identifier);
    definedVariables.add(variable);
    variable.setType(type);
    return variable;
  }

  /**
   * Creates new {@link LocalVariableElementImpl} with given name and type.
   * 
   * @param type the {@link Type} of the variable
   * @param name the name of the variable
   * @return the new {@link LocalVariableElementImpl}
   */
  LocalVariableElementImpl createLocalVariable(Type type, String name) {
    SimpleIdentifier identifier = createIdentifier(name);
    return createLocalVariable(type, identifier);
  }

  /**
   * Declares the given {@link LocalVariableElementImpl} in the {@link #topNameScope}.
   */
  void defineTopVariable(LocalVariableElementImpl variable) {
    definedVariables.add(variable);
    topNameScope.define(variable);
    recordTypeLibraryInjected(variable);
  }

  /**
   * Declares the given {@link LocalVariableElementImpl} in the current {@link #nameScope}.
   */
  void defineVariable(LocalVariableElementImpl variable) {
    definedVariables.add(variable);
    nameScope.define(variable);
    recordTypeLibraryInjected(variable);
  }

  /**
   * @return the {@link TypeProvider} of the {@link AnalysisContext}.
   */
  TypeProvider getTypeProvider() {
    return typeProvider;
  }

  Expression parseExpression(com.google.dart.engine.scanner.Token token) {
    return HtmlParser.parseEmbeddedExpression(source, token, errorListener);
  }

  Expression parseExpression(String contents, int startIndex, int endIndex, int offset) {
    com.google.dart.engine.scanner.Token token = scanDart(contents, startIndex, endIndex, offset);
    return parseExpression(token);
  }

  /**
   * Reports given {@link ErrorCode} at the given {@link ASTNode}.
   */
  void reportError(ASTNode node, ErrorCode errorCode, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        node.getOffset(),
        node.getLength(),
        errorCode,
        arguments));
  }

  /**
   * Resolves given {@link ASTNode} using {@link #resolver}.
   */
  void resolveNode(ASTNode node) {
    node.accept(resolver);
  }

  com.google.dart.engine.scanner.Token scanDart(String contents, int startIndex, int endIndex,
      int offset) {
    return HtmlParser.scanDartSource(
        source,
        lineInfo,
        contents.substring(startIndex, endIndex),
        offset + startIndex,
        errorListener);
  }

  private NgController createController(ClassDeclaration injectedClass) {
    List<Annotation> annotations = injectedClass.getMetadata();
    for (Annotation annotation : annotations) {
      // TODO(scheglov) by some reason Angular To-Do sample uses @NgDirective for controller
      if (isNgController(annotation) || isNgDirective(annotation)) {
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
            AngularSelector selector = createInjectSelector(selectorText);
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
    // create FunctionElementImpl
    functionElement = new FunctionElementImpl(0);
    unitElement.setFunctions(new FunctionElement[] {functionElement});
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
    topNameScope = resolver.pushNameScope();
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
   * Returns {@link ClassDeclaration} of classes injected into the given module.
   * 
   * @param element a Dart {@link Element} used as an argument for <code>bootstrap</code> invocation
   *          - class or variable.
   */
  private List<ClassDeclaration> getInjectedClasses(Element element) throws AnalysisException {
    List<ClassDeclaration> injected = Lists.newArrayList();
    ToolkitObjectElement[] toolkitObjects = ToolkitObjectElement.EMPTY_ARRAY;
    // ClassElement
    if (element instanceof ClassElement) {
      ClassElement classElement = (ClassElement) element;
      toolkitObjects = classElement.getToolkitObjects();
    }
    // LocalVariableElement
    if (element instanceof LocalVariableElement) {
      LocalVariableElement variableElement = (LocalVariableElement) element;
      toolkitObjects = variableElement.getToolkitObjects();
    }
    // process toolkit elements
    for (ToolkitObjectElement toolkitObject : toolkitObjects) {
      if (toolkitObject instanceof AngularModuleElement) {
        AngularModuleElement moduleElement = (AngularModuleElement) toolkitObject;
        for (ClassElement typeElement : moduleElement.getKeyTypes()) {
          try {
            ClassDeclaration injectedClass = typeElement.getNode();
            injected.add(injectedClass);
          } catch (AnalysisException e) {
            thrownException = e;
          }
        }
      }
    }
    // done
    return injected;
  }

  /**
   * Finds all Angular modules references by the given <code>ngBootstrap</code> invocation.
   * 
   * @param bootInvocation the <code>ngBootstrap</code> invocation
   * @return the module {@link Element}s - classes or local variables
   */
  private List<Element> getModules(MethodInvocation bootInvocation) throws AnalysisException {
    List<Element> modules = Lists.newArrayList();
    List<Expression> arguments = bootInvocation.getArgumentList().getArguments();
    for (Expression argument : arguments) {
      // TODO(scheglov) limitation - only one module, add support for "modules"
      {
        Expression moduleExpression = getNamedExpression(argument, "module");
        if (moduleExpression != null) {
          Element element = null;
          if (moduleExpression instanceof InstanceCreationExpression) {
            element = moduleExpression.getBestType().getElement();
          } else if (moduleExpression instanceof SimpleIdentifier) {
            element = ((SimpleIdentifier) moduleExpression).getBestElement();
          }
          if (element != null) {
            modules.add(element);
          }
        }
      }
    }
    return modules;
  }

  /**
   * Checks if the given {@link XmlAttributeNode} has been already processed by one of the
   * directives, so we should not try to find expressions and resolve it.
   */
  private boolean isProcessedByDirective(XmlAttributeNode node) {
    // TODO(scheglov) We need some way to mark attribute as handled by directive.
    // For now, just exclude all ng- attributes.
    // TODO(scheglov) May be replace getName() with getNameToken() and return String from getName().
    return node.getName().getLexeme().startsWith("ng-");
  }

  /**
   * Parse the value of the given token for embedded expressions, and add any embedded expressions
   * that are found to the given list of expressions.
   * 
   * @param expressions the list to which embedded expressions are to be added
   * @param token the token whose value is to be parsed
   */
  private void parseEmbeddedExpressions(ArrayList<EmbeddedExpression> expressions, Token token) {
    // prepare Token information
    String lexeme = token.getLexeme();
    int offset = token.getOffset();
    // find expressions between {{ and }}
    int startIndex = lexeme.indexOf(OPENING_DELIMITER);
    while (startIndex >= 0) {
      int endIndex = lexeme.indexOf(CLOSING_DELIMITER, startIndex + OPENING_DELIMITER_LENGTH);
      if (endIndex < 0) {
        // TODO(brianwilkerson) Should we report this error or will it be reported by something else?
        return;
      } else if (startIndex + OPENING_DELIMITER_LENGTH < endIndex) {
        startIndex += OPENING_DELIMITER_LENGTH;
        Expression expression = parseExpression(lexeme, startIndex, endIndex, offset);
        expressions.add(new EmbeddedExpression(startIndex, expression, endIndex));
      }
      startIndex = lexeme.indexOf(OPENING_DELIMITER, endIndex + CLOSING_DELIMITER_LENGTH);
    }
  }

  private void parseEmbeddedExpressions(XmlAttributeNode node) {
    ArrayList<EmbeddedExpression> expressions = new ArrayList<EmbeddedExpression>();
    parseEmbeddedExpressions(expressions, node.getValue());
    node.setExpressions(expressions.toArray(new EmbeddedExpression[expressions.size()]));
  }

  private void parseEmbeddedExpressions(XmlTagNode node) {
    ArrayList<EmbeddedExpression> expressions = new ArrayList<EmbeddedExpression>();
    Token token = node.getAttributeEnd();
    Token endToken = node.getEndToken();
    boolean inChild = false;
    while (token != endToken) {
      for (XmlTagNode child : node.getTagNodes()) {
        if (token == child.getBeginToken()) {
          inChild = true;
          break;
        }
        if (token == child.getEndToken()) {
          inChild = false;
          break;
        }
      }
      if (!inChild && token.getType() == com.google.dart.engine.html.scanner.TokenType.TEXT) {
        parseEmbeddedExpressions(expressions, token);
      }
      token = token.getNext();
    }
    node.setExpressions(expressions.toArray(new EmbeddedExpression[expressions.size()]));
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
    List<Element> modules = getModules(bootInvocation);
    for (Element module : modules) {
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
   * When we inject variable, we give access to the library of its type.
   */
  private void recordTypeLibraryInjected(LocalVariableElementImpl variable) {
    LibraryElement typeLibrary = variable.getType().getElement().getLibrary();
    injectedLibraries.add(typeLibrary);
  }

  private void resolveExpressions(EmbeddedExpression[] expressions) {
    for (EmbeddedExpression embeddedExpression : expressions) {
      Expression expression = embeddedExpression.getExpression();
      resolveNode(expression);
    }
  }

  /**
   * The "ng-model" directive is special, it contributes to the top-level name scope. These models
   * can be used before actual "ng-model" attribute in HTML. So, we need to define them once we
   * found {@link #NG_APP} context.
   */
  private void visitModelDirectives(XmlTagNode appNode) {
    appNode.accept(new RecursiveXmlVisitor<Void>() {
      @Override
      public Void visitXmlTagNode(XmlTagNode node) {
        NgModelDirective directive = NgModelDirective.INSTANCE;
        if (directive.getSelector().apply(node)) {
          directive.applyTopDeclarations(AngularHtmlUnitResolver.this, node);
        }
        return super.visitXmlTagNode(node);
      }
    });
  }
}
