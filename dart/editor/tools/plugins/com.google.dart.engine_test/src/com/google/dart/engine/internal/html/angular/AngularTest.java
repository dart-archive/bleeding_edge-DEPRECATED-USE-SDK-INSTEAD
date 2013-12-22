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
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisContextHelper;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
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
  protected final AnalysisContextHelper contextHelper = new AnalysisContextHelper();

  protected AnalysisContext context;
  protected Source mainSource;
  protected String indexContent;
  protected Source indexSource;
  protected HtmlUnit indexUnit;
  protected CompilationUnitElement indexDartUnit;

  protected final void addMyController() {
    mainSource = contextHelper.addSource("/main.dart", createSource("",//
        "import 'angular.dart';",
        "",
        "@NgController(",
        "    selector: '[my-marker]',",
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

  protected final void assertResolvedIdentifier(String name, String expectedTypeName) {
    SimpleIdentifier identifier = findIdentifier(name);
    // check Element
    Element element = identifier.getBestElement();
    assertNotNull(element);
    // check Type
    Type type = identifier.getBestType();
    assertNotNull(type);
    assertEquals(expectedTypeName, type.toString());
  }

  /**
   * @return {@link ASTNode} which has required offset and type.
   */
  protected final <E extends ASTNode> E findExpression(int offset, Class<E> clazz) {
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, offset);
    return expression != null ? expression.getAncestor(clazz) : null;
  }

  /**
   * @return the {@link SimpleIdentifier} at the given search pattern.
   */
  protected final SimpleIdentifier findIdentifier(String search) {
    SimpleIdentifier identifier = findExpression(findOffset(search), SimpleIdentifier.class);
    assertNotNull(search + " in " + indexContent, identifier);
    return identifier;
  }

  /**
   * @return the offset of given <code>search</code> string in {@link #indexContent}. Fails test if
   *         not found.
   */
  protected final int findOffset(String search) {
    int offset = indexContent.indexOf(search);
    assertThat(offset).describedAs(indexContent).isNotEqualTo(-1);
    return offset;
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
            "abstract class NgAnnotation {",
            "  NgAnnotation({",
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
            "  NgDirective({",
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
            "             exportExpressions: exportExpressions",
            "             exportExpressionAttrs: exportExpressionAttrs);",
            "}",
            "",
            "class NgController extends NgDirective {",
            "  NgDirective({",
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
            "             exportExpressions: exportExpressions",
            "             exportExpressionAttrs: exportExpressionAttrs);",
            "}",
            "",
            "class Module {}",
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
