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
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlExpression;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.builder.ElementBuilder;
import com.google.dart.engine.internal.builder.ElementHolder;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
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
      resolver.reportErrorForOffset(AngularCode.INVALID_REPEAT_SYNTAX, offset, spec.length() - 2);
      return;
    }
    String lhsSpec = syntaxMatcher.group(1);
    String iterableSpec = syntaxMatcher.group(2);
    String idSpec = syntaxMatcher.group(4);
    int lhsOffset = offset + syntaxMatcher.start(1);
    int iterableOffset = offset + syntaxMatcher.start(2);
    int idOffset = offset + syntaxMatcher.start(4);
    List<XmlExpression> expressions = Lists.newArrayList();
    // check LHS syntax
    Matcher lhsMatcher = LHS_PATTERN.matcher(lhsSpec);
    if (!lhsMatcher.matches()) {
      resolver.reportErrorForOffset(
          AngularCode.INVALID_REPEAT_ITEM_SYNTAX,
          lhsOffset,
          lhsSpec.length());
      return;
    }
    // parse item name
    Expression varExpression = resolver.parseDartExpression(lhsSpec, 0, lhsSpec.length(), lhsOffset);
    SimpleIdentifier varName = (SimpleIdentifier) varExpression;
    // parse iterable
    AngularExpression iterableExpr = resolver.parseAngularExpression(
        iterableSpec,
        0,
        iterableSpec.length(),
        iterableOffset);
    // resolve as: for (name in iterable) {}
    DeclaredIdentifier loopVariable = new DeclaredIdentifier(null, null, null, null, varName);
    Block loopBody = new Block(null, new ArrayList<Statement>(), null);
    ForEachStatement loopStatement = new ForEachStatement(
        null,
        null,
        null,
        loopVariable,
        null,
        iterableExpr.getExpression(),
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
    // resolve formatters
    resolveFormatters(resolver, iterableExpr, itemType);
    // remember expressions
    expressions.add(newRawXmlExpression(varExpression));
    expressions.add(newAngularRawXmlExpression(iterableExpr));
    if (idSpec != null) {
      AngularExpression idExpression = resolver.parseAngularExpression(
          idSpec,
          0,
          idSpec.length(),
          idOffset);
      expressions.add(newAngularRawXmlExpression(idExpression));
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
    LocalVariableElementImpl variable = resolver.createLocalVariableWithName(type, name);
    resolver.defineVariable(variable);
  }

  private void defineLocalVariable_int(AngularHtmlUnitResolver resolver, String name) {
    InterfaceType type = resolver.getTypeProvider().getIntType();
    LocalVariableElementImpl variable = resolver.createLocalVariableWithName(type, name);
    resolver.defineVariable(variable);
  }

  /**
   * Resolves an argument for "filter" formatter.
   */
  private void resolveFormatterArgument_filter(AngularHtmlUnitResolver resolver, Type itemType,
      AngularFormatterArgument argument, int argIndex) {
    Expression arg = argument.getExpression();
    // only first argument is special for "filter"
    if (argIndex != 0) {
      resolver.resolveNode(arg);
      return;
    }
    // Map
    if (arg instanceof MapLiteral) {
      List<Expression> expressions = Lists.newArrayList();
      List<MapLiteralEntry> entries = ((MapLiteral) arg).getEntries();
      for (MapLiteralEntry mapEntry : entries) {
        Expression keyExpr = mapEntry.getKey();
        if (keyExpr instanceof SimpleIdentifier) {
          SimpleIdentifier propertyNode = (SimpleIdentifier) keyExpr;
          resolvePropertyNode(resolver, expressions, itemType, propertyNode);
        }
      }
      // set resolved sub-expressions
      argument.setSubExpressions(expressions.toArray(new Expression[expressions.size()]));
    }
  }

  /**
   * Resolves an argument for "orderBy" formatter.
   */
  private void resolveFormatterArgument_orderBy(AngularHtmlUnitResolver resolver,
      List<Expression> expressions, Type itemType, Expression arg, int argIndex) {
    // List of properties
    if (arg instanceof ListLiteral) {
      List<Expression> subArgs = ((ListLiteral) arg).getElements();
      for (Expression subArg : subArgs) {
        resolveFormatterArgument_orderBy(resolver, expressions, itemType, subArg, 0);
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
      arg = resolver.parseDartExpression(exprSource, 0, exprSource.length(), argOffset);
      if (arg instanceof SimpleIdentifier) {
        SimpleIdentifier propertyNode = (SimpleIdentifier) arg;
        resolvePropertyNode(resolver, expressions, itemType, propertyNode);
      }
    }
  }

  /**
   * Resolves an argument for "orderBy" formatter.
   */
  private void resolveFormatterArgument_orderByWithFilter(AngularHtmlUnitResolver resolver,
      Type itemType, AngularFormatterArgument argument, int argIndex) {
    Expression arg = argument.getExpression();
    // only first argument is special for "orderBy"
    if (argIndex != 0) {
      resolver.resolveNode(arg);
      return;
    }
    //
    List<Expression> expressions = Lists.newArrayList();
    resolveFormatterArgument_orderBy(resolver, expressions, itemType, arg, 0);
    // set resolved sub-expressions
    argument.setSubExpressions(expressions.toArray(new Expression[expressions.size()]));
  }

  private void resolveFormatterArguments(AngularHtmlUnitResolver resolver, Type itemType,
      String formatterName, List<AngularFormatterArgument> arguments) {
    int index = 0;
    for (AngularFormatterArgument argument : arguments) {
      if ("filter".equals(formatterName)) {
        resolveFormatterArgument_filter(resolver, itemType, argument, index);
      }
      if ("orderBy".equals(formatterName)) {
        resolveFormatterArgument_orderByWithFilter(resolver, itemType, argument, index);
      }
      index++;
    }
  }

  /**
   * Resolves sequence of formatters.
   */
  private void resolveFormatters(AngularHtmlUnitResolver resolver,
      AngularExpression angularExpression, Type itemType) {
    for (AngularFormatterNode formatter : angularExpression.getFormatters()) {
      SimpleIdentifier formatterNameNode = formatter.getName();
      String formatterName = formatterNameNode.getName();
      // resolve formatter name
      formatterNameNode.setStaticElement(resolver.findAngularElement(formatterName));
      // resolve formatter arguments
      resolveFormatterArguments(resolver, itemType, formatterName, formatter.getArguments());
    }
  }

  private void resolvePropertyNode(AngularHtmlUnitResolver resolver, List<Expression> expressions,
      Type itemType, SimpleIdentifier propertyNode) {
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
    expressions.add(propertyNode);
  }
}
