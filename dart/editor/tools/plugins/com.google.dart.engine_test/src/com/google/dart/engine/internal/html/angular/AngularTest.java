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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.internal.resolver.ResolutionVerifier;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import junit.framework.AssertionFailedError;

import static org.fest.assertions.Assertions.assertThat;

abstract public class AngularTest extends EngineTestCase {
  /**
   * Creates an HTML content that has Angular marker and script with "main.dart" reference.
   */
  protected static String createHtmlWithAngular(String... lines) {
    String source = createSource(//
        "<html ng-app>",
        "  <body>");
    source += createSource(lines);
    source += createSource(//
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    return source;
  }

  /**
   * Creates an HTML content that has Angular marker, script with "main.dart" reference and
   * "MyController" injected.
   */
  protected static String createHtmlWithMyController(String... lines) {
    String source = createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div my-controller>");
    source += createSource(lines);
    source += createSource(//
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    return source;
  }

  /**
   * Finds an {@link Element} with the given names inside of the given root {@link Element}.
   * <p>
   * TODO(scheglov) maybe move this method to Element
   * 
   * @param root the root {@link Element} to start searching from
   * @param kind the kind of the {@link Element} to find, if {@code null} then any kind
   * @param name the name of an {@link Element} to find
   * @return the found {@link Element} or {@code null} if not found
   */
  @SuppressWarnings("unchecked")
  protected static <T extends Element> T findElement(Element root, final ElementKind kind,
      final String name) {
    final Element[] result = {null};
    root.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        if ((kind == null || element.getKind() == kind) && name.equals(element.getName())) {
          result[0] = element;
        }
        return super.visitElement(element);
      }
    });
    return (T) result[0];
  }

  /**
   * Finds an {@link Element} with the given names inside of the given root {@link Element}.
   * 
   * @param root the root {@link Element} to start searching from
   * @param name the name of an {@link Element} to find
   * @return the found {@link Element} or {@code null} if not found
   */
  protected static <T extends Element> T findElement(Element root, String name) {
    return findElement(root, null, name);
  }

  /**
   * @return the offset of given <code>search</code> string in <code>content</code>. Fails test if
   *         not found.
   */
  protected static int findOffset(String content, String search) {
    int offset = content.indexOf(search);
    assertThat(offset).describedAs(content).isNotEqualTo(-1);
    return offset;
  }

  protected final AnalysisContextHelper contextHelper = new AnalysisContextHelper();

  protected AnalysisContext context;
  protected String mainContent;
  protected Source mainSource;
  protected CompilationUnit mainUnit;

  protected CompilationUnitElement mainUnitElement;
  protected String indexContent;
  protected Source indexSource;
  protected HtmlUnit indexUnit;
  protected HtmlElement indexHtmlUnit;

  protected CompilationUnitElement indexDartUnitElement;

  /**
   * Fills {@link #indexContent} and {@link #indexSource}.
   */
  protected final void addIndexSource(String content) {
    addIndexSource("/index.html", content);
  }

  /**
   * Fills {@link #indexContent} and {@link #indexSource}.
   */
  protected final void addIndexSource(String name, String content) {
    indexContent = content;
    indexSource = contextHelper.addSource(name, indexContent);
  }

  /**
   * Fills {@link #mainContent} and {@link #mainSource}.
   */
  protected final void addMainSource(String content) {
    mainContent = content;
    mainSource = contextHelper.addSource("/main.dart", content);
  }

  protected final void addMyController() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "class Item {",
        "  String name;",
        "  bool done;",
        "}",
        "",
        "@NgController(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  String field;",
        "  List<String> names;",
        "  List<Item> items;",
        "  var untypedItems;",
        "  doSomething(event) {}",
        "}"));
  }

  /**
   * Assert that the number of errors reported against the given source matches the number of errors
   * that are given and that they have the expected error codes. The order in which the errors were
   * gathered is ignored.
   * 
   * @param source the source against which the errors should have been reported
   * @param expectedErrorCodes the error codes of the errors that should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if a different number of errors have been reported than were
   *           expected
   */
  protected final void assertErrors(Source source, ErrorCode... expectedErrorCodes)
      throws AnalysisException {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    AnalysisErrorInfo errorsInfo = context.getErrors(source);
    for (AnalysisError error : errorsInfo.getErrors()) {
      errorListener.onError(error);
    }
    errorListener.assertErrorsWithCodes(expectedErrorCodes);
  }

  protected final void assertMainErrors(ErrorCode... expectedErrorCodes) throws AnalysisException {
    assertErrors(mainSource, expectedErrorCodes);
  }

  /**
   * Assert that no errors have been reported against the given source.
   * 
   * @param source the source against which no errors should have been reported
   * @throws AnalysisException if the reported errors could not be computed
   * @throws AssertionFailedError if any errors have been reported
   */
  protected final void assertNoErrors() throws AnalysisException {
    assertErrors(indexSource);
  }

  protected final void assertNoErrors(Source source) throws AnalysisException {
    assertErrors(source);
  }

  /**
   * Checks that {@link #indexHtmlUnit} has {@link SimpleIdentifier} with given name, resolved to
   * not {@code null} {@link Element}.
   */
  protected final Element assertResolvedIdentifier(String name) {
    SimpleIdentifier identifier = findIdentifier(name);
    // check Element
    Element element = identifier.getBestElement();
    assertNotNull(element);
    // return Element for further analysis
    return element;
  }

  protected final Element assertResolvedIdentifier(String name, String expectedTypeName) {
    SimpleIdentifier identifier = findIdentifier(name);
    // check Element
    Element element = identifier.getBestElement();
    assertNotNull(element);
    // check Type
    Type type = identifier.getBestType();
    assertNotNull(type);
    assertEquals(expectedTypeName, type.toString());
    // return Element for further analysis
    return element;
  }

  /**
   * @return {@link AstNode} which has required offset and type.
   */
  protected final <E extends AstNode> E findExpression(int offset, Class<E> clazz) {
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, offset);
    return expression != null ? expression.getAncestor(clazz) : null;
  }

  /**
   * Returns the {@link SimpleIdentifier} at the given search pattern. Fails if not found.
   */
  protected final SimpleIdentifier findIdentifier(String search) {
    SimpleIdentifier identifier = findIdentifierMaybe(search);
    assertNotNull(search + " in " + indexContent, identifier);
    // check that offset/length of the identifier is valid
    {
      int offset = identifier.getOffset();
      int end = identifier.getEnd();
      String contentStr = indexContent.substring(offset, end);
      assertEquals(identifier.getName(), contentStr);
    }
    // done
    return identifier;
  }

  /**
   * Returns the {@link SimpleIdentifier} at the given search pattern, or {@code null} if not found.
   */
  protected final SimpleIdentifier findIdentifierMaybe(String search) {
    return findExpression(findOffset(search), SimpleIdentifier.class);
  }

  /**
   * Returns {@link Element} from {@link #indexDartUnitElement}.
   */
  protected final <T extends Element> T findIndexElement(String name) throws AnalysisException {
    return findElement(indexDartUnitElement, name);
  }

  /**
   * Returns {@link Element} from {@link #mainUnitElement}.
   */
  protected final <T extends Element> T findMainElement(ElementKind kind, String name)
      throws AnalysisException {
    return findElement(mainUnitElement, kind, name);
  }

  /**
   * Returns {@link Element} from {@link #mainUnitElement}.
   */
  protected final <T extends Element> T findMainElement(String name) throws AnalysisException {
    return findElement(mainUnitElement, name);
  }

  /**
   * @return the offset of given <code>search</code> string in {@link #mainContent}. Fails test if
   *         not found.
   */
  protected final int findMainOffset(String search) {
    return findOffset(mainContent, search);
  }

  /**
   * @return the offset of given <code>search</code> string in {@link #indexContent}. Fails test if
   *         not found.
   */
  protected final int findOffset(String search) {
    return findOffset(indexContent, search);
  }

  /**
   * Resolves {@link #indexSource}.
   */
  protected final void resolveIndex() throws AnalysisException {
    indexUnit = context.resolveHtmlUnit(indexSource);
    indexHtmlUnit = indexUnit.getElement();
    indexDartUnitElement = indexHtmlUnit.getAngularCompilationUnit();
  }

  protected final void resolveIndex(String content) throws Exception {
    addIndexSource(content);
    contextHelper.runTasks();
    resolveIndex();
  }

  /**
   * Resolves {@link #mainSource}.
   */
  protected final void resolveMain() throws Exception {
    mainUnit = contextHelper.resolveDefiningUnit(mainSource);
    mainUnitElement = mainUnit.getElement();
  }

  /**
   * Resolves {@link #mainSource}.
   */
  protected final void resolveMainNoErrors() throws Exception {
    resolveMain();
    assertNoErrors(mainSource);
  }

  protected final void resolveMainSource(String content) throws Exception {
    addMainSource(content);
    resolveMain();
  }

  protected final void resolveMainSourceNoErrors(String content) throws Exception {
    resolveMainSource(content);
    assertNoErrors(mainSource);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    configureForAngular(contextHelper);
    context = contextHelper.context;
  }

  /**
   * Verify that all of the identifiers in the HTML units associated with the given sources have
   * been resolved.
   * 
   * @param sources the sources identifying the compilation units to be verified
   * @throws Exception if the contents of the compilation unit cannot be accessed
   */
  protected final void verify(Source... sources) throws Exception {
    final ResolutionVerifier verifier = new ResolutionVerifier();
    for (Source source : sources) {
      HtmlUnit htmlUnit = context.getResolvedHtmlUnit(source);
      htmlUnit.accept(new ExpressionVisitor() {
        @Override
        public void visitExpression(Expression expression) {
          expression.accept(verifier);
        }
      });
    }
    verifier.assertResolved();
  }

  private void configureForAngular(AnalysisContextHelper contextHelper) {
    contextHelper.addSource(
        "/angular.dart",
        createSource(
            "library angular;",
            "",
            "class Scope {",
            "  Map context;",
            "}",
            "",
            "class NgFilter {",
            "  final String name;",
            "  const NgFilter({this.name});",
            "}",
            "",
            "abstract class NgAnnotation {",
            "  const NgAnnotation({",
            "    selector,",
            "    children,",
            "    visibility,",
            "    publishAs,",
            "    publishTypes,",
            "    map,",
            "    exportedExpressions,",
            "    exportedExpressionAttrs",
            "  });",
            "}",
            "",
            "class NgDirective extends NgAnnotation {",
            "  const NgDirective({",
            "    children,",
            "    publishAs,",
            "    map,",
            "    selector,",
            "    visibility,",
            "    publishTypes,",
            "    exportedExpressions,",
            "    exportedExpressionAttrs",
            "  }) : super(selector: selector, children: children, visibility: visibility,",
            "             publishTypes: publishTypes, publishAs: publishAs, map: map,",
            "             exportedExpressions: exportedExpressions,",
            "             exportedExpressionAttrs: exportedExpressionAttrs);",
            "}",
            "",
            "class NgController extends NgDirective {",
            "  const NgController({",
            "    children,",
            "    publishAs,",
            "    map,",
            "    selector,",
            "    visibility,",
            "    publishTypes,",
            "    exportedExpressions,",
            "    exportedExpressionAttrs",
            "  }) : super(selector: selector, children: children, visibility: visibility,",
            "             publishTypes: publishTypes, publishAs: publishAs, map: map,",
            "             exportedExpressions: exportedExpressions,",
            "             exportedExpressionAttrs: exportedExpressionAttrs);",
            "}",
            "",
            "class NgAttr {",
            "  const NgAttr(String name);",
            "}",
            "class NgCallback {",
            "  const NgCallback(String name);",
            "}",
            "class NgOneWay {",
            "  const NgOneWay(String name);",
            "}",
            "class NgOneWayOneTime {",
            "  const NgOneWayOneTime(String name);",
            "}",
            "class NgTwoWay {",
            "  const NgTwoWay(String name);",
            "}",
            "",
            "class NgComponent extends NgDirective {",
            "  const NgComponent({",
            "    this.template,",
            "    this.templateUrl,",
            "    this.cssUrl,",
            "    this.cssUrls,",
            "    this.applyAuthorStyles,",
            "    this.resetStyleInheritance,",
            "    publishAs,",
            "    map,",
            "    selector,",
            "    visibility,",
            "    publishTypes /*: const <Type>[]*/,",
            "    exportExpressions,",
            "    exportExpressionAttrs",
            "  }) : super(selector: selector,",
            "             /*children: NgAnnotation.COMPILE_CHILDREN,*/",
            "             visibility: visibility,",
            "             publishTypes: publishTypes,",
            "             publishAs: publishAs,",
            "             map: map,",
            "             exportExpressions: exportExpressions,",
            "             exportExpressionAttrs: exportExpressionAttrs);",
            "}",
            "",
            "@NgDirective(selector: '[ng-click]', map: const {'ng-click': '&onEvent'})",
            "@NgDirective(selector: '[ng-mouseout]', map: const {'ng-mouseout': '&onEvent'})",
            "class NgEventDirective {",
            "  set onEvent(value) {}",
            "}",
            "",
            "@NgDirective(selector: '[ng-if]', map: const {'ng-if': '=>condition'})",
            "class NgIfDirective {",
            "  set condition(value) {}",
            "}",
            "",
            "@NgDirective(selector: '[ng-show]', map: const {'ng-show': '=>show'})",
            "class NgShowDirective {",
            "  set show(value) {}",
            "}",
            "",
            "@NgFilter(name: 'filter')",
            "class FilterFilter {}",
            "",
            "@NgFilter(name: 'orderBy')",
            "class OrderByFilter {}",
            "",
            "@NgFilter(name: 'uppercase')",
            "class UppercaseFilter {}",
            "",
            "class ViewFactory {",
            "  call(String templateUrl) => null;",
            "}",
            "",
            "class Module {",
            "  install(Module m) {}",
            "  type(Type t) {}",
            "  value(Type t, value) {}",
            "}",
            "",
            "class Injector {}",
            "",
            "Injector ngBootstrap({",
            "        Module module: null,",
            "        List<Module> modules: null,",
            "        /*dom.Element*/ element: null,",
            "        String selector: '[ng-app]',",
            "        /*Injector*/ injectorFactory/*(List<Module> modules): _defaultInjectorFactory*/}) {}",
            ""));
  }
}
