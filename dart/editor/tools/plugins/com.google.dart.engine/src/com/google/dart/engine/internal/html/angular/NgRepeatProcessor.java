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
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.builder.ElementBuilder;
import com.google.dart.engine.internal.builder.ElementHolder;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link NgRepeatProcessor} describes built-in <code>NgRepeatDirective</code> directive.
 */
class NgRepeatProcessor extends NgDirectiveProcessor {
  private static final String NG_REPEAT = "ng-repeat";
  private static final Pattern SYNTAX_PATTERN = Pattern.compile("^\\s*(.+)\\s+in\\s+(.+?)\\s*(\\s+track\\s+by\\s+(.+)\\s*)?(\\s+lazily\\s*)?$");
  private static final Pattern LHS_PATTERN = Pattern.compile("^(?:([\\$\\w]+)|\\(([\\$\\w]+)\\s*,\\s*([\\$\\w]+)\\))$");
  private static final Pattern FILTER_PATTERN = Pattern.compile("^(.*)\\s*\\|\\s*(.+?)(:(.+))?\\s*$");

  public static final NgRepeatProcessor INSTANCE = new NgRepeatProcessor();

  private NgRepeatProcessor() {
  }

  @Override
  public void apply(AngularHtmlUnitResolver resolver, XmlTagNode node) {
    XmlAttributeNode attribute = node.getAttribute(NG_REPEAT);
    int offset = attribute.getValueToken().getOffset() + 1;
    String spec = attribute.getText();
    // check syntax
    Matcher syntaxMatcher = SYNTAX_PATTERN.matcher(spec);
    if (!syntaxMatcher.matches()) {
      resolver.reportError(offset, spec.length() - 2, AngularCode.INVALID_REPEAT_SYNTAX);
      return;
    }
    String lhsSpec = syntaxMatcher.group(1);
    String iterableSpec = syntaxMatcher.group(2);
    String idSpec = syntaxMatcher.group(4);
    int lhsOffset = offset + syntaxMatcher.start(1);
    int iterableOffset = offset + syntaxMatcher.start(2);
    int idOffset = offset + syntaxMatcher.start(4);
    List<Expression> expressions = Lists.newArrayList();
    // check LHS syntax
    Matcher lhsMatcher = LHS_PATTERN.matcher(lhsSpec);
    if (!lhsMatcher.matches()) {
      resolver.reportError(lhsOffset, lhsSpec.length(), AngularCode.INVALID_REPEAT_ITEM_SYNTAX);
      return;
    }
    // parse item name
    Expression varExpression = resolver.parseExpression(lhsSpec, lhsOffset);
    SimpleIdentifier varName = (SimpleIdentifier) varExpression;
    // cut off filters
    int barIndex = StringUtilities.indexOf1(iterableSpec, 0, '|');
    String filtersSpec = "";
    int filtersOffset = -1;
    if (barIndex != -1) {
      filtersSpec = iterableSpec.substring(barIndex);
      iterableSpec = iterableSpec.substring(0, barIndex);
      filtersOffset = iterableOffset + barIndex;
    }
    // parse iterable
    Expression iterableExpr = resolver.parseExpression(iterableSpec, iterableOffset);
    // resolve as: for (name in iterable) {}
    DeclaredIdentifier loopVariable = new DeclaredIdentifier(null, null, null, null, varName);
    Block loopBody = new Block(null, new ArrayList<Statement>(), null);
    ForEachStatement loopStatement = new ForEachStatement(
        null,
        null,
        loopVariable,
        null,
        iterableExpr,
        null,
        loopBody);
    new ElementBuilder(new ElementHolder()).visitDeclaredIdentifier(loopVariable);
    resolver.resolveNode(loopStatement);
    // define item variable
    Type itemType = varName.getBestType();
    {
      LocalVariableElementImpl variable = (LocalVariableElementImpl) varName.getStaticElement();
      variable.setType(itemType);
      resolver.defineVariable(variable);
    }
    // resolve filters
    resolveFilters(resolver, expressions, filtersSpec, filtersOffset, itemType);
    // remember expressions
    expressions.add(varExpression);
    expressions.add(iterableExpr);
    if (idSpec != null) {
      Expression idExpression = resolver.parseExpression(idSpec, idOffset);
      expressions.add(idExpression);
    }
    setExpressions(attribute, expressions);
    // define additional variables
    defineLocalVariable_int(resolver, "$index");
    defineLocalVariable_bool(resolver, "$first");
    defineLocalVariable_bool(resolver, "$middle");
    defineLocalVariable_bool(resolver, "$last");
    defineLocalVariable_bool(resolver, "$even");
    defineLocalVariable_bool(resolver, "$odd");
  }

  @Override
  public boolean canApply(XmlTagNode node) {
    return node.getAttribute(NG_REPEAT) != null;
  }

  private void defineLocalVariable_bool(AngularHtmlUnitResolver resolver, String name) {
    InterfaceType type = resolver.getTypeProvider().getBoolType();
    LocalVariableElementImpl variable = resolver.createLocalVariable(type, name);
    resolver.defineVariable(variable);
  }

  private void defineLocalVariable_int(AngularHtmlUnitResolver resolver, String name) {
    InterfaceType type = resolver.getTypeProvider().getIntType();
    LocalVariableElementImpl variable = resolver.createLocalVariable(type, name);
    resolver.defineVariable(variable);
  }

  /**
   * Resolves an argument for "orderBy" filter.
   */
  private void resolveFilterArgument_orderBy(AngularHtmlUnitResolver resolver,
      List<Expression> expressions, Type itemType, Expression arg, int argIndex) {
    // only first argument is special for "orderBy"
    if (argIndex != 0) {
      expressions.add(arg);
      return;
    }
    // List of properties
    if (arg instanceof ListLiteral) {
      List<Expression> subArgs = ((ListLiteral) arg).getElements();
      for (Expression subArg : subArgs) {
        resolveFilterArgument_orderBy(resolver, expressions, itemType, subArg, 0);
      }
      return;
    }
    // property name in quotes
    if (arg instanceof SimpleStringLiteral) {
      SimpleStringLiteral argLiteral = (SimpleStringLiteral) arg;
      String exprSource = argLiteral.getStringValue();
      int argOffset = argLiteral.getValueOffset();
      // remove leading +/-
      if (StringUtilities.startsWithChar(exprSource, '+')) {
        exprSource = exprSource.substring(1);
        argOffset++;
      } else if (StringUtilities.startsWithChar(exprSource, '-')) {
        exprSource = exprSource.substring(1);
        argOffset++;
      }
      // empty string - use item itself, nothing to resolve
      if (exprSource.isEmpty()) {
        return;
      }
      // resolve property name
      arg = resolver.parseExpression(exprSource, argOffset);
      if (arg instanceof SimpleIdentifier) {
        SimpleIdentifier propertyNode = (SimpleIdentifier) arg;
        // if known type - resolve, otherwise keep it 'dynamic'
        if (itemType instanceof InterfaceType) {
          String propertyName = propertyNode.getName();
          Element propertyElement = ((InterfaceType) itemType).getGetter(propertyName);
          if (propertyElement != null) {
            propertyNode.setStaticElement(propertyElement);
          }
        } else {
          Type dynamicType = resolver.getTypeProvider().getDynamicType();
          propertyNode.setStaticElement(dynamicType.getElement());
          propertyNode.setStaticType(dynamicType);
        }
        // add argument
        expressions.add(arg);
      }
    }
  }

  private void resolveFilterArguments(AngularHtmlUnitResolver resolver,
      List<Expression> expressions, Type itemType, String filterName, String argsSpec,
      int argsOffset) {
    Token argsToken = resolver.scanDart(argsSpec, 0, argsSpec.length(), argsOffset);
    int argIndex = 0;
    while (argsToken != null && argsToken.getType() != TokenType.EOF) {
      Expression arg = resolver.parseExpression(argsToken);
      if ("orderBy".equals(filterName)) {
        resolveFilterArgument_orderBy(resolver, expressions, itemType, arg, argIndex);
      }
      // next argument
      argsToken = arg.getEndToken().getNext();
      argIndex++;
      // stop if EOF
      if (argsToken.getType() == TokenType.EOF) {
        break;
      }
      // ":" is expected
      if (argsToken.getType() == TokenType.COLON) {
        argsToken = argsToken.getNext();
      } else {
        resolver.reportError(
            argsToken.getOffset(),
            argsToken.getLength(),
            AngularCode.MISSING_FILTER_COLON);
      }
    }
  }

  /**
   * Resolves sequence of filters.
   * 
   * @param filtersSpec the string of format "| filterNameA:arg0[:arg1]| filterNameB:arg0[:arg1]"
   */
  private void resolveFilters(AngularHtmlUnitResolver resolver, List<Expression> expressions,
      String filtersSpec, int filtersOffset, Type itemType) {
    while (true) {
      Matcher filterMatcher = FILTER_PATTERN.matcher(filtersSpec);
      if (!filterMatcher.matches()) {
        break;
      }
      filtersSpec = filterMatcher.group(1);
      // resolve filter name
      String filterName = filterMatcher.group(2);
      {
        int filterOffset = filtersOffset + filterMatcher.start(2);
        SimpleIdentifier filterNode = AngularHtmlUnitResolver.createIdentifier(
            filterName,
            filterOffset);
        expressions.add(filterNode);
        filterNode.setStaticElement(resolver.findAngularElement(filterName));
      }
      // prepare filter arguments
      String argsSpec = filterMatcher.group(4);
      int argsOffset = filtersOffset + filterMatcher.start(4);
      if (argsSpec == null) {
        continue;
      }
      // resolve filter arguments
      resolveFilterArguments(resolver, expressions, itemType, filterName, argsSpec, argsOffset);
    }
  }
}
