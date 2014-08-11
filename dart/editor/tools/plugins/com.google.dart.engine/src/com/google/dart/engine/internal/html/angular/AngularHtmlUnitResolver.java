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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularHasTemplateElement;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlExpression;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.internal.element.ImportElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.internal.element.angular.AngularComponentElementImpl;
import com.google.dart.engine.internal.element.angular.AngularElementImpl;
import com.google.dart.engine.internal.element.angular.AngularViewElementImpl;
import com.google.dart.engine.internal.resolver.InheritanceManager;
import com.google.dart.engine.internal.resolver.ResolverVisitor;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.engine.utilities.source.LineInfo;

import static com.google.dart.engine.internal.html.angular.AngularMoustacheXmlExpression.CLOSING_DELIMITER_CHAR;
import static com.google.dart.engine.internal.html.angular.AngularMoustacheXmlExpression.CLOSING_DELIMITER_LENGTH;
import static com.google.dart.engine.internal.html.angular.AngularMoustacheXmlExpression.OPENING_DELIMITER_CHAR;
import static com.google.dart.engine.internal.html.angular.AngularMoustacheXmlExpression.OPENING_DELIMITER_LENGTH;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class {@link AngularHtmlUnitResolver} resolve Angular specific expressions.
 */
public class AngularHtmlUnitResolver extends RecursiveXmlVisitor<Void> {
  private static class FilteringAnalysisErrorListener implements AnalysisErrorListener {
    private final AnalysisErrorListener listener;

    public FilteringAnalysisErrorListener(AnalysisErrorListener listener) {
      this.listener = listener;
    }

    @Override
    public void onError(AnalysisError error) {
      ErrorCode errorCode = error.getErrorCode();
      if (errorCode == StaticWarningCode.UNDEFINED_GETTER
          || errorCode == StaticWarningCode.UNDEFINED_IDENTIFIER
          || errorCode == StaticTypeWarningCode.UNDEFINED_GETTER) {
        return;
      }
      listener.onError(error);
    }
  }

  private static class FoundAppError extends Error {
  }

  private static final String NG_APP = "ng-app";

  /**
   * Checks if given {@link Element} is an artificial local variable and returns corresponding
   * {@link AngularElement}, or {@code null} otherwise.
   */
  public static AngularElement getAngularElement(Element element) {
    // may be artificial local variable, replace with AngularElement
    if (element instanceof LocalVariableElement) {
      LocalVariableElement local = (LocalVariableElement) element;
      ToolkitObjectElement[] toolkitObjects = local.getToolkitObjects();
      if (toolkitObjects.length == 1 && toolkitObjects[0] instanceof AngularElement) {
        return (AngularElement) toolkitObjects[0];
      }
    }
    // not a special Element
    return null;
  }

  /**
   * @return {@code true} if the given {@link HtmlUnit} has <code>ng-app</code> annotation.
   */
  public static boolean hasAngularAnnotation(HtmlUnit htmlUnit) {
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

  static SimpleIdentifier createIdentifier(String name, int offset) {
    StringToken token = createStringToken(name, offset);
    return new SimpleIdentifier(token);
  }

  /**
   * Adds {@link AngularElement} declared by the given top-level {@link Element}.
   * 
   * @param angularElements the list to fill with top-level {@link AngularElement}s
   * @param classElement the {@link ClassElement} to get {@link AngularElement}s from
   */
  private static void addAngularElementsFromClass(Set<AngularElement> angularElements,
      ClassElement classElement) {
    for (ToolkitObjectElement toolkitObject : classElement.getToolkitObjects()) {
      if (toolkitObject instanceof AngularElement) {
        angularElements.add((AngularElement) toolkitObject);
      }
    }
  }

  /**
   * Returns the array of all top-level Angular elements that could be used in this library.
   * 
   * @param libraryElement the {@link LibraryElement} to analyze
   * @return the array of all top-level Angular elements that could be used in this library
   */
  private static void addAngularElementsFromLibrary(Set<AngularElement> angularElements,
      LibraryElement library, Set<LibraryElement> visited) {
    if (library == null) {
      return;
    }
    if (!visited.add(library)) {
      return;
    }
    // add Angular elements from current library
    for (CompilationUnitElement unit : library.getUnits()) {
      Collections.addAll(angularElements, unit.getAngularViews());
      for (ClassElement type : unit.getTypes()) {
        addAngularElementsFromClass(angularElements, type);
      }
    }
    // handle imports
    for (ImportElement importElement : library.getImports()) {
      LibraryElement importedLibrary = importElement.getImportedLibrary();
      addAngularElementsFromLibrary(angularElements, importedLibrary, visited);
    }
  }

  // TODO(scheglov) rename to: createIdentifierToken
  private static StringToken createStringToken(String name, int offset) {
    return new StringToken(TokenType.IDENTIFIER, name, offset);
  }

  /**
   * Returns the array of all top-level Angular elements that could be used in this library.
   * 
   * @param libraryElement the {@link LibraryElement} to analyze
   * @return the array of all top-level Angular elements that could be used in this library
   */
  private static AngularElement[] getAngularElements(Set<LibraryElement> libraries,
      LibraryElement libraryElement) {
    Set<AngularElement> angularElements = Sets.newHashSet();
    addAngularElementsFromLibrary(angularElements, libraryElement, libraries);
    return angularElements.toArray(new AngularElement[angularElements.size()]);
  }

  /**
   * Returns the external Dart {@link CompilationUnit} referenced by the given {@link HtmlUnit}.
   */
  private static CompilationUnit getDartUnit(AnalysisContext context, HtmlUnit unit)
      throws AnalysisException {
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

  private static Set<Source> getLibrarySources(Set<LibraryElement> libraries) {
    Set<Source> sources = Sets.newHashSet();
    for (LibraryElement library : libraries) {
      sources.add(library.getSource());
    }
    return sources;
  }

  private final InternalAnalysisContext context;
  private final TypeProvider typeProvider;
  private final FilteringAnalysisErrorListener errorListener;

  private final Source source;
  private final LineInfo lineInfo;
  private final HtmlUnit unit;
  private AngularElement[] angularElements;
  private final List<NgProcessor> processors = Lists.newArrayList();

  private LibraryElementImpl libraryElement;
  private CompilationUnitElementImpl unitElement;
  private FunctionElementImpl functionElement;
  private ResolverVisitor resolver;
  private boolean isAngular = false;

  private List<LocalVariableElementImpl> definedVariables = Lists.newArrayList();

  private Set<LibraryElement> injectedLibraries = Sets.newHashSet();

  private Scope topNameScope;

  private Scope nameScope;

  public AngularHtmlUnitResolver(InternalAnalysisContext context,
      AnalysisErrorListener errorListener, Source source, LineInfo lineInfo, HtmlUnit unit)
      throws AnalysisException {
    this.context = context;
    this.typeProvider = context.getTypeProvider();
    this.errorListener = new FilteringAnalysisErrorListener(errorListener);
    this.source = source;
    this.lineInfo = lineInfo;
    this.unit = unit;
  }

  /**
   * The {@link AngularApplication} for the Web application with this entry point, may be
   * {@code null} if not an entry point.
   */
  public AngularApplication calculateAngularApplication() throws AnalysisException {
    // check if Angular at all
    if (!hasAngularAnnotation(unit)) {
      return null;
    }
    // prepare resolved Dart unit
    CompilationUnit dartUnit = getDartUnit(context, unit);
    if (dartUnit == null) {
      return null;
    }
    // prepare accessible Angular elements
    LibraryElement libraryElement = dartUnit.getElement().getLibrary();
    Set<LibraryElement> libraries = Sets.newHashSet();
    AngularElement[] angularElements = getAngularElements(libraries, libraryElement);
    // resolve AngularComponentElement template URIs
    // TODO(scheglov) resolve to HtmlElement to allow F3 ?
    Set<Source> angularElementsSources = Sets.newHashSet();
    for (AngularElement angularElement : angularElements) {
      if (angularElement instanceof AngularHasTemplateElement) {
        AngularHasTemplateElement hasTemplate = (AngularHasTemplateElement) angularElement;
        angularElementsSources.add(angularElement.getSource());
        String templateUri = hasTemplate.getTemplateUri();
        if (templateUri == null) {
          continue;
        }
        try {
          Source templateSource = context.getSourceFactory().forUri(
              source.resolveRelativeUri(new URI(templateUri)));
          if (!context.exists(templateSource)) {
            templateSource = context.getSourceFactory().resolveUri(source, "package:" + templateUri);
            if (!context.exists(templateSource)) {
              errorListener.onError(new AnalysisError(
                  angularElement.getSource(),
                  hasTemplate.getTemplateUriOffset(),
                  templateUri.length(),
                  AngularCode.URI_DOES_NOT_EXIST,
                  templateUri));
              continue;
            }
          }
          if (!AnalysisEngine.isHtmlFileName(templateUri)) {
            continue;
          }
          if (hasTemplate instanceof AngularComponentElementImpl) {
            ((AngularComponentElementImpl) hasTemplate).setTemplateSource(templateSource);
          }
          if (hasTemplate instanceof AngularViewElementImpl) {
            ((AngularViewElementImpl) hasTemplate).setTemplateSource(templateSource);
          }
        } catch (URISyntaxException exception) {
          errorListener.onError(new AnalysisError(
              angularElement.getSource(),
              hasTemplate.getTemplateUriOffset(),
              templateUri.length(),
              AngularCode.INVALID_URI,
              templateUri));
        }
      }
    }
    // create AngularApplication
    AngularApplication application = new AngularApplication(
        source,
        getLibrarySources(libraries),
        angularElements,
        angularElementsSources.toArray(new Source[angularElementsSources.size()]));
    // set AngularApplication for each AngularElement
    for (AngularElement angularElement : angularElements) {
      ((AngularElementImpl) angularElement).setApplication(application);
    }
    // done
    return application;
  }

  /**
   * Resolves {@link #source} as an {@link AngularComponentElement} template file.
   * 
   * @param application the Angular application we are resolving for
   * @param component the {@link AngularComponentElement} to resolve template for, not {@code null}
   */
  public void resolveComponentTemplate(AngularApplication application,
      AngularComponentElement component) throws AnalysisException {
    isAngular = true;
    resolveInternal(application.getElements(), component);
  }

  /**
   * Resolves {@link #source} as an Angular application entry point.
   */
  public void resolveEntryPoint(AngularApplication application) throws AnalysisException {
    resolveInternal(application.getElements(), null);
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    parseEmbeddedExpressionsInAttribute(node);
    resolveExpressions(node.getExpressions());
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
      pushNameScope();
      try {
        parseEmbeddedExpressionsInTag(node);
        // apply processors
        for (NgProcessor processor : processors) {
          if (processor.canApply(node)) {
            processor.apply(this, node);
          }
        }
        // resolve expressions
        resolveExpressions(node.getExpressions());
        // process children
        return super.visitXmlTagNode(node);
      } finally {
        popNameScope();
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
  LocalVariableElementImpl createLocalVariableFromIdentifier(Type type, SimpleIdentifier identifier) {
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
  LocalVariableElementImpl createLocalVariableWithName(Type type, String name) {
    SimpleIdentifier identifier = createIdentifier(name, 0);
    return createLocalVariableFromIdentifier(type, identifier);
  }

  /**
   * Declares the given {@link LocalVariableElementImpl} in the {@link #topNameScope}.
   */
  void defineTopVariable(LocalVariableElementImpl variable) {
    recordDefinedVariable(variable);
    topNameScope.define(variable);
    recordTypeLibraryInjected(variable);
  }

  /**
   * Declares the given {@link LocalVariableElementImpl} in the current {@link #nameScope}.
   */
  void defineVariable(LocalVariableElementImpl variable) {
    recordDefinedVariable(variable);
    nameScope.define(variable);
    recordTypeLibraryInjected(variable);
  }

  /**
   * @return the {@link AngularElement} with the given name, maybe {@code null}.
   */
  AngularElement findAngularElement(String name) {
    for (AngularElement element : angularElements) {
      if (name.equals(element.getName())) {
        return element;
      }
    }
    return null;
  }

  /**
   * @return the {@link TypeProvider} of the {@link AnalysisContext}.
   */
  TypeProvider getTypeProvider() {
    return typeProvider;
  }

  /**
   * Parses given {@link String} as an {@link AngularExpression} at the given offset.
   */
  AngularExpression parseAngularExpression(String contents, int startIndex, int endIndex, int offset) {
    Token token = scanDart(contents, startIndex, endIndex, offset);
    return parseAngularExpressionInToken(token);
  }

  AngularExpression parseAngularExpressionInToken(Token token) {
    List<Token> tokens = splitAtBar(token);
    Expression mainExpression = parseDartExpressionInToken(tokens.get(0));
    // parse formatters
    List<AngularFormatterNode> formatters = Lists.newArrayList();
    for (int i = 1; i < tokens.size(); i++) {
      Token formatterToken = tokens.get(i);
      Token barToken = formatterToken;
      formatterToken = formatterToken.getNext();
      // parse name
      Expression nameExpression = parseDartExpressionInToken(formatterToken);
      if (!(nameExpression instanceof SimpleIdentifier)) {
        reportErrorForNode(AngularCode.INVALID_FORMATTER_NAME, nameExpression);
        continue;
      }
      SimpleIdentifier name = (SimpleIdentifier) nameExpression;
      formatterToken = name.getEndToken().getNext();
      // parse arguments
      List<AngularFormatterArgument> arguments = Lists.newArrayList();
      while (formatterToken.getType() != TokenType.EOF) {
        // skip ":"
        Token colonToken = formatterToken;
        if (colonToken.getType() == TokenType.COLON) {
          formatterToken = formatterToken.getNext();
        } else {
          reportErrorForToken(AngularCode.MISSING_FORMATTER_COLON, colonToken);
        }
        // parse argument
        Expression argument = parseDartExpressionInToken(formatterToken);
        arguments.add(new AngularFormatterArgument(colonToken, argument));
        // next token
        formatterToken = argument.getEndToken().getNext();
      }
      formatters.add(new AngularFormatterNode(barToken, name, arguments));
    }
    // done
    return new AngularExpression(mainExpression, formatters);
  }

  /**
   * Parses given {@link String} as an {@link Expression} at the given offset.
   */
  Expression parseDartExpression(String contents, int startIndex, int endIndex, int offset) {
    Token token = scanDart(contents, startIndex, endIndex, offset);
    return parseDartExpressionInToken(token);
  }

  Expression parseDartExpressionInToken(Token token) {
    Parser parser = new Parser(source, errorListener);
    return parser.parseExpression(token);
  }

  void popNameScope() {
    nameScope = resolver.popNameScope();
  }

  void pushNameScope() {
    nameScope = resolver.pushNameScope();
  }

  /**
   * Reports given {@link ErrorCode} at the given {@link AstNode}.
   */
  void reportErrorForNode(ErrorCode errorCode, AstNode node, Object... arguments) {
    reportErrorForOffset(errorCode, node.getOffset(), node.getLength(), arguments);
  }

  /**
   * Reports given {@link ErrorCode} at the given position.
   */
  void reportErrorForOffset(ErrorCode errorCode, int offset, int length, Object... arguments) {
    errorListener.onError(new AnalysisError(source, offset, length, errorCode, arguments));
  }

  /**
   * Reports given {@link ErrorCode} at the given {@link Token}.
   */
  void reportErrorForToken(ErrorCode errorCode, Token token, Object... arguments) {
    reportErrorForOffset(errorCode, token.getOffset(), token.getLength(), arguments);
  }

  void resolveExpression(AngularExpression angularExpression) {
    List<Expression> dartExpressions = angularExpression.getExpressions();
    for (Expression dartExpression : dartExpressions) {
      resolveNode(dartExpression);
    }
  }

  /**
   * Resolves given {@link AstNode} using {@link #resolver}.
   */
  void resolveNode(AstNode node) {
    node.accept(resolver);
  }

  Token scanDart(String contents, int startIndex, int endIndex, int offset) {
    return HtmlParser.scanDartSource(
        source,
        lineInfo,
        contents.substring(startIndex, endIndex),
        offset + startIndex,
        errorListener);
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
    libraryElement.setAngularHtml(true);
    injectedLibraries.add(libraryElement);
    // create FunctionElementImpl
    functionElement = new FunctionElementImpl(0);
    unitElement.setFunctions(new FunctionElement[] {functionElement});
  }

  /**
   * Creates new {@link NgProcessor} for the given {@link AngularElement}, maybe {@code null} if not
   * supported.
   */
  private NgProcessor createProcessor(AngularElement element) {
    if (element instanceof AngularComponentElement) {
      AngularComponentElement component = (AngularComponentElement) element;
      return new NgComponentElementProcessor(component);
    }
    if (element instanceof AngularControllerElement) {
      AngularControllerElement controller = (AngularControllerElement) element;
      return new NgControllerElementProcessor(controller);
    }
    if (element instanceof AngularDecoratorElement) {
      AngularDecoratorElement directive = (AngularDecoratorElement) element;
      return new NgDecoratorElementProcessor(directive);
    }
    return null;
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
    // add Scope variables - no type, no location, just to avoid warnings
    {
      Type type = typeProvider.getDynamicType();
      topNameScope.define(createLocalVariableWithName(type, "$id"));
      topNameScope.define(createLocalVariableWithName(type, "$parent"));
      topNameScope.define(createLocalVariableWithName(type, "$root"));
    }
  }

  /**
   * Defines variable for the given {@link AngularElement} with type of the enclosing
   * {@link ClassElement}.
   */
  private void defineTopVariable_forClassElement(AngularElement element) {
    ClassElement classElement = (ClassElement) element.getEnclosingElement();
    InterfaceType type = classElement.getType();
    LocalVariableElementImpl variable = createLocalVariableWithName(type, element.getName());
    defineTopVariable(variable);
    variable.setToolkitObjects(new AngularElement[] {element});
  }

  /**
   * Defines variable for the given {@link AngularScopePropertyElement}.
   */
  private void defineTopVariable_forScopeProperty(AngularScopePropertyElement element) {
    Type type = element.getType();
    LocalVariableElementImpl variable = createLocalVariableWithName(type, element.getName());
    defineTopVariable(variable);
    variable.setToolkitObjects(new AngularElement[] {element});
  }

  /**
   * Parse the value of the given token for embedded expressions, and add any embedded expressions
   * that are found to the given list of expressions.
   * 
   * @param expressions the list to which embedded expressions are to be added
   * @param token the token whose value is to be parsed
   */
  private void parseEmbeddedExpressions(List<AngularMoustacheXmlExpression> expressions,
      com.google.dart.engine.html.scanner.Token token) {
    // prepare Token information
    String lexeme = token.getLexeme();
    int offset = token.getOffset();
    // find expressions between {{ and }}
    int startIndex = StringUtilities.indexOf2(
        lexeme,
        0,
        OPENING_DELIMITER_CHAR,
        OPENING_DELIMITER_CHAR);
    while (startIndex >= 0) {
      int endIndex = StringUtilities.indexOf2(
          lexeme,
          startIndex + OPENING_DELIMITER_LENGTH,
          CLOSING_DELIMITER_CHAR,
          CLOSING_DELIMITER_CHAR);
      if (endIndex < 0) {
        // TODO(brianwilkerson) Should we report this error or will it be reported by something else?
        return;
      } else if (startIndex + OPENING_DELIMITER_LENGTH < endIndex) {
        startIndex += OPENING_DELIMITER_LENGTH;
        AngularExpression expression = parseAngularExpression(lexeme, startIndex, endIndex, offset);
        expressions.add(new AngularMoustacheXmlExpression(startIndex, endIndex, expression));
      }
      startIndex = StringUtilities.indexOf2(
          lexeme,
          endIndex + CLOSING_DELIMITER_LENGTH,
          OPENING_DELIMITER_CHAR,
          OPENING_DELIMITER_CHAR);
    }
  }

  private void parseEmbeddedExpressionsInAttribute(XmlAttributeNode node) {
    List<AngularMoustacheXmlExpression> expressions = Lists.newArrayList();
    parseEmbeddedExpressions(expressions, node.getValueToken());
    if (!expressions.isEmpty()) {
      node.setExpressions(expressions.toArray(new AngularMoustacheXmlExpression[expressions.size()]));
    }
  }

  private void parseEmbeddedExpressionsInTag(XmlTagNode node) {
    List<AngularMoustacheXmlExpression> expressions = Lists.newArrayList();
    com.google.dart.engine.html.scanner.Token token = node.getAttributeEnd();
    com.google.dart.engine.html.scanner.Token endToken = node.getEndToken();
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
    node.setExpressions(expressions.toArray(new AngularMoustacheXmlExpression[expressions.size()]));
  }

  private void recordDefinedVariable(LocalVariableElementImpl variable) {
    definedVariables.add(variable);
    functionElement.setLocalVariables(definedVariables.toArray(new LocalVariableElementImpl[definedVariables.size()]));
  }

  /**
   * When we inject variable, we give access to the library of its type.
   */
  private void recordTypeLibraryInjected(LocalVariableElementImpl variable) {
    LibraryElement typeLibrary = variable.getType().getElement().getLibrary();
    injectedLibraries.add(typeLibrary);
  }

  private void resolveExpressions(XmlExpression[] expressions) {
    for (XmlExpression xmlExpression : expressions) {
      if (xmlExpression instanceof AngularXmlExpression) {
        AngularXmlExpression angularXmlExpression = (AngularXmlExpression) xmlExpression;
        resolveXmlExpression(angularXmlExpression);
      }
    }
  }

  /**
   * Resolves Angular specific expressions and elements in the {@link #source}.
   * 
   * @param angularElements the {@link AngularElement}s accessible in the component's library, not
   *          {@code null}
   * @param component the {@link AngularComponentElement} to resolve template for, maybe
   *          {@code null} if not a component template
   */
  private void resolveInternal(AngularElement[] angularElements, AngularComponentElement component)
      throws AnalysisException {
    this.angularElements = angularElements;
    // add built-in processors
    processors.add(NgModelProcessor.INSTANCE);
    processors.add(NgRepeatProcessor.INSTANCE);
    // add element's libraries
    for (AngularElement angularElement : angularElements) {
      injectedLibraries.add(angularElement.getLibrary());
    }
    // prepare Dart library
    createLibraryElement();
    ((HtmlElementImpl) unit.getElement()).setAngularCompilationUnit(unitElement);
    // prepare Dart resolver
    createResolver();
    // maybe resolving component template
    if (component != null) {
      defineTopVariable_forClassElement(component);
      for (AngularScopePropertyElement scopeProperty : component.getScopeProperties()) {
        defineTopVariable_forScopeProperty(scopeProperty);
      }
    }
    // add processors
    for (AngularElement angularElement : angularElements) {
      NgProcessor processor = createProcessor(angularElement);
      if (processor != null) {
        processors.add(processor);
      }
    }
    // define formatters
    for (AngularElement angularElement : angularElements) {
      if (angularElement instanceof AngularFormatterElement) {
        defineTopVariable_forClassElement(angularElement);
      }
    }
    // run this HTML visitor
    unit.accept(this);
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
  }

  private void resolveXmlExpression(AngularXmlExpression angularXmlExpression) {
    AngularExpression angularExpression = angularXmlExpression.getExpression();
    resolveExpression(angularExpression);
  }

  // XXX
  private List<Token> splitAtBar(Token token) {
    List<Token> tokens = Lists.newArrayList();
    tokens.add(token);
    while (token.getType() != TokenType.EOF) {
      if (token.getType() == TokenType.BAR) {
        tokens.add(token);
        Token eofToken = new Token(TokenType.EOF, 0);
        token.getPrevious().setNext(eofToken);
      }
      token = token.getNext();
    }
    return tokens;
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
        NgModelProcessor directive = NgModelProcessor.INSTANCE;
        if (directive.canApply(node)) {
          directive.applyTopDeclarations(AngularHtmlUnitResolver.this, node);
        }
        return super.visitXmlTagNode(node);
      }
    });
  }
}
