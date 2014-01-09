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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.html.ast.EmbeddedExpression;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.internal.resolver.ResolutionVerifier;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import junit.framework.AssertionFailedError;

import static org.fest.assertions.Assertions.assertThat;

abstract public class AngularTest extends EngineTestCase {
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
   * @return the offset of given <code>search</code> string in <code>content</code>. Fails test if
   *         not found.
   */
  protected static int findOffset(String content, String search) {
    int offset = content.indexOf(search);
    assertThat(offset).describedAs(content).isNotEqualTo(-1);
    return offset;
  }

  /**
   * Finds an {@link Element} with the given names inside of the given root {@link Element}.
   * <p>
   * TODO(scheglov) maybe move this method to Element
   * 
   * @param root the root {@link Element} to start searching from
   * @param name the name of an {@link Element} to find
   * @return the found {@link Element} or {@code null} if not found
   */
  private static Element findElement(Element root, final String name) {
    final Element[] result = {null};
    root.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        if (name.equals(element.getName())) {
          result[0] = element;
        }
        return super.visitElement(element);
      }
    });
    return result[0];
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
  protected CompilationUnitElement indexDartUnit;

  /**
   * Fills {@link #mainContent} and {@link #mainSource}.
   */
  protected final void addMainSource(String content) {
    mainContent = content;
    mainSource = contextHelper.addSource("/main.dart", content);
  }

  protected final void addMyController() throws Exception {
    mainSource = contextHelper.addSource("/main.dart", createSource("",//
        "import 'angular.dart';",
        "",
        "@NgController(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  String field;",
        "  List<String> names;",
        "}",
        "",
        "class MyModule extends Module {",
        "  MyModule() {",
        "    type(MyController);",
        "  }",
        "}",
        "",
        "main() {",
        "  ngBootstrap(module: new MyModule());",
        "}"));
    mainUnit = contextHelper.resolveDefiningUnit(mainSource);
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
    for (AnalysisError error : context.computeErrors(source)) {
      errorListener.onError(error);
    }
    errorListener.assertErrors(expectedErrorCodes);
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
   * @return {@link ASTNode} which has required offset and type.
   */
  protected final <E extends ASTNode> E findExpression(int offset, Class<E> clazz) {
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, offset);
    return expression != null ? expression.getAncestor(clazz) : null;
  }

  /**
   * Returns the {@link SimpleIdentifier} at the given search pattern. Fails if not found.
   */
  protected final SimpleIdentifier findIdentifier(String search) {
    SimpleIdentifier identifier = findIdentifierMaybe(search);
    assertNotNull(search + " in " + indexContent, identifier);
    return identifier;
  }

  /**
   * Returns the {@link SimpleIdentifier} at the given search pattern, or {@code null} if not found.
   */
  protected final SimpleIdentifier findIdentifierMaybe(String search) {
    return findExpression(findOffset(search), SimpleIdentifier.class);
  }

  /**
   * Returns {@link Element} from {@link #indexDartUnit}.
   */
  protected final Element findIndexElement(String name) throws AnalysisException {
    return findElement(indexDartUnit, name);
  }

  /**
   * Returns {@link Element} from {@link #mainSource}.
   */
  protected final Element findMainElement(String name) throws AnalysisException {
    CompilationUnit unit = context.resolveCompilationUnit(mainSource, mainSource);
    CompilationUnitElement unitElement = unit.getElement();
    return findElement(unitElement, name);
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

  protected final void resolveIndex(String... lines) throws Exception {
    indexContent = createSource(lines);
    indexSource = contextHelper.addSource("/index.html", indexContent);
    indexUnit = context.resolveHtmlUnit(indexSource);
    indexDartUnit = indexUnit.getCompilationUnitElement();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    configureForAngular(contextHelper);
    context = contextHelper.context;
  }

  /**
   * Verify that all of the identifiers in the compilation units associated with the given sources
   * have been resolved.
   * 
   * @param resolvedElementMap a table mapping the AST nodes that have been resolved to the element
   *          to which they were resolved
   * @param sources the sources identifying the compilation units to be verified
   * @throws Exception if the contents of the compilation unit cannot be accessed
   */
  protected final void verify(Source... sources) throws Exception {
    final ResolutionVerifier verifier = new ResolutionVerifier();
    for (Source source : sources) {
      HtmlUnit htmlUnit = context.getResolvedHtmlUnit(source);
      htmlUnit.accept(new RecursiveXmlVisitor<Void>() {
        @Override
        public Void visitXmlTagNode(XmlTagNode node) {
          for (EmbeddedExpression embeddedExpression : node.getExpressions()) {
            Expression expression = embeddedExpression.getExpression();
            expression.accept(verifier);
          }
          return super.visitXmlTagNode(node);
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
