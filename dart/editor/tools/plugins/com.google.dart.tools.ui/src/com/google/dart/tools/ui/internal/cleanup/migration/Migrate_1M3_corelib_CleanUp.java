/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.common.base.Objects;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.ConstructorElement;
import com.google.dart.compiler.resolver.Elements;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp.MethodSpec;

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_ITERABLE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_VARIABLE_VALUE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;
import static com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp.convertMethodToGetter;
import static com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp.isSubType;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.List;

/**
 * In 1.0 M3 many change to the <code>Iterator</code>, <code>Iterable</code> and <code>Future</code>
 * will be done.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M3_corelib_CleanUp extends AbstractMigrateCleanUp {
  private static final MethodSpec[] METHOD_TO_GETTER_LIST = new MethodSpec[] {
      new MethodSpec("dart://uri/uri.dart", "Uri", "isAbsolute"),
      new MethodSpec("dart://uri/uri.dart", "Uri", "hasAuthority")};

  /**
   * @return <code>E</code> for method <code>where(bool f(E element))</code>, may be
   *         <code>null</code> if other code structure given.
   */
  private static String getCollectionTypeFromFunctionParameter(DartMethodDefinition node) {
    String result = null;
    List<DartParameter> nodeParameters = node.getFunction().getParameters();
    if (nodeParameters.size() == 1) {
      List<DartParameter> parameters2 = nodeParameters.get(0).getFunctionParameters();
      if (parameters2 != null && parameters2.size() == 1) {
        DartParameter parameter2 = parameters2.get(0);
        if (parameter2.getTypeNode() != null && parameter2.getTypeNode().getType() != null
            && parameter2.getTypeNode().getType().getElement() != null) {
          result = parameter2.getTypeNode().getType().getElement().getName();
        }
      }
    }
    return result;
  }

  /**
   * @return the Dart source of type arguments of the given {@link InterfaceType}, may be empty, but
   *         not <code>null</code>.
   */
  private static String getTypeArgsSource(Type type) {
    if (TypeKind.of(type) == TypeKind.INTERFACE) {
      List<Type> typeArgs = ((InterfaceType) type).getArguments();
      for (Type argType : typeArgs) {
        if (TypeKind.of(argType) != TypeKind.DYNAMIC) {
          return "<" + StringUtils.join(typeArgs, ", ") + ">";
        }
      }
    }
    return "";
  }

  private static boolean isArgInterfaceType(List<DartExpression> args, int index, String typeName) {
    if (index < args.size()) {
      DartExpression arg = args.get(index);
      Type type = arg.getType();
      if (type instanceof InterfaceType) {
        InterfaceType interfaceType = (InterfaceType) type;
        return Objects.equal(interfaceType.getElement().getName(), typeName);
      }
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link DartNode} is used as abstract <code>Iterable</code>,
   *         so we don't need to materialize it as <code>List</code> or <code>Set</code>.
   */
  private static boolean isUsedAsIterable(DartNode node) {
    StructuralPropertyDescriptor locationInParent = getLocationInParent(node);
    if (locationInParent == DART_VARIABLE_VALUE) {
      final DartVariable variable = (DartVariable) node.getParent();
      // may be assigned to the specific type
      if (variable.getParent() instanceof DartVariableStatement) {
        DartVariableStatement statement = (DartVariableStatement) variable.getParent();
        if (statement.getTypeNode() != null) {
          return false;
        }
      }
      // check if variable is used only as Iterable
      final VariableElement variableElement = variable.getElement();
      final boolean result[] = new boolean[] {true};
      node.getRoot().accept(new ASTVisitor<Void>() {
        @Override
        public Void visitIdentifier(DartIdentifier node) {
          if (node.getElement() == variableElement && node != variable.getName()) {
            result[0] &= isUsedAsIterable(node);
          }
          return super.visitIdentifier(node);
        }
      });
      return result[0];
    }
    // used as iterable in for-in statement
    if (locationInParent == DART_FOR_IN_STATEMENT_ITERABLE) {
      return true;
    }
    if (locationInParent == DART_METHOD_INVOCATION_TARGET) {
      DartMethodInvocation invocation = (DartMethodInvocation) node.getParent();
      String name = invocation.getFunctionNameString();
      return "map".equals(name) || "where".equals(name) || "filter".equals(name)
          || "contains".equals(name) || "forEach".equals(name) || "reduce".equals(name)
          || "every".equals(name) || "some".equals(name);
    }
    return false;
  }

  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitClass(DartClass node) {
        for (DartTypeNode intNode : node.getInterfaces()) {
          Type intType = intNode.getType();
          if (TypeKind.of(intType) == TypeKind.INTERFACE && intType.getElement() != null) {
            // Iterator
            if (intType.getElement().getName().equals("Iterator")) {
              convertIteratorImplementation(node, intType);
            }
            // Iterable
            if (intType.getElement().getName().equals("Iterable") && node.getSuperclass() == null) {
              addReplaceEdit(
                  SourceRangeFactory.forStartLength(
                      node.getImplementsOffset(),
                      "implements".length()),
                  "extends");
            }
          }
        }
        return super.visitClass(node);
      }

      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        DartExpression nameNode = node.getName();
        // iterator()  --->  get iterator()
        if (Elements.isIdentifierName(nameNode, "iterator") && !node.getModifiers().isGetter()) {
          addReplaceEdit(SourceRangeFactory.forStartLength(nameNode, 0), "get ");
          addReplaceEdit(SourceRangeFactory.forEndEnd(
              nameNode,
              node.getFunction().getParametersCloseParen() + 1), "");
        }
        // change implementations
        boolean isFilterMethod = Elements.isIdentifierName(nameNode, "filter");
        boolean isMapMethod = Elements.isIdentifierName(nameNode, "map");
        if ((isFilterMethod || isMapMethod) && node.getElement() != null
            && node.getElement().getEnclosingElement() instanceof ClassElement) {
          ClassElement enclosingClass = (ClassElement) node.getElement().getEnclosingElement();
          String typeVarName = getCollectionTypeFromFunctionParameter(node);
          if (typeVarName != null) {
            boolean isList = isSubType(enclosingClass.getType(), "List", "dart://core/core.dart");
            boolean isSet = isSubType(enclosingClass.getType(), "Set", "dart://core/core.dart");
            if (isList || isSet) {
              // filter()  --->  where()
              if (isFilterMethod) {
                String src = MessageFormat.format(
                    "Iterable<{0}> where(bool f({0} element)) => new WhereIterable<{0}>(this, f);",
                    typeVarName);
                addReplaceEdit(SourceRangeFactory.create(node), src);
              }
              // map()  --->  MappedIterable
              if (isMapMethod && isSet) {
                String src = MessageFormat.format(
                    "Iterable map(f({0} element)) => new MappedIterable<{0}, dynamic>(this, f);",
                    typeVarName);
                addReplaceEdit(SourceRangeFactory.create(node), src);
              }
              if (isMapMethod && isList) {
                String src = MessageFormat.format(
                    "Iterable map(f({0} element)) => new MappedIterable<{0}, dynamic>(this, f);",
                    typeVarName);
                addReplaceEdit(SourceRangeFactory.create(node), src);
              }
            }
          }
        }
        // default
        return super.visitMethodDefinition(node);
      }

      @Override
      public Void visitMethodInvocation(DartMethodInvocation node) {
        DartIdentifier nameNode = node.getFunctionName();
        List<DartExpression> args = node.getArguments();
        // Iterator  --->  HasNextIterator
        replaceIteratorType(node, nameNode);
        // filter -> where
        {
          boolean wasFilter = Elements.isIdentifierName(nameNode, "filter");
          boolean wasMap = Elements.isIdentifierName(nameNode, "map");
          if (wasFilter || wasMap) {
            if (wasFilter) {
              addReplaceEdit(SourceRangeFactory.create(nameNode), "where");
            }
            if (!isUsedAsIterable(node)) {
              String sourceTypeName = findSourceTypeName(node);
              if ("List".equals(sourceTypeName)) {
                addReplaceEdit(SourceRangeFactory.forEndLength(node, 0), ".toList()");
              }
              if ("Set".equals(sourceTypeName)) {
                addReplaceEdit(SourceRangeFactory.forEndLength(node, 0), ".toSet()");
              }
            }
          }
        }
        // Strings.join(strings, separator)  --->  strings.join(separator)
        if (node.getTarget() instanceof DartIdentifier
            && ((DartIdentifier) node.getTarget()).getName().equals("Strings")) {
          if (Elements.isIdentifierName(nameNode, "join")) {
            addReplaceEdit(SourceRangeFactory.forStartStart(node, args.get(0)), "");
            addReplaceEdit(SourceRangeFactory.forEndStart(args.get(0), args.get(1)), ".join(");
          }
          if (Elements.isIdentifierName(nameNode, "concatAll")) {
            addReplaceEdit(SourceRangeFactory.forStartStart(node, args.get(0)), "");
            addReplaceEdit(SourceRangeFactory.forEndLength(args.get(0), 0), ".join(");
          }
        }
        // uri.isAbsolute() becomes uri.isAbsolute, and uri.hasAuthority() becomes uri.hasAuthority
        convertMethodToGetter(change, node, METHOD_TO_GETTER_LIST);
        // element.on.click.add(listener)  --->  element.onClick.listen(listener)
        if (nameNode.getName().equals("add") && node.getRealTarget() instanceof DartPropertyAccess) {
          DartPropertyAccess elementOnEvent = (DartPropertyAccess) node.getRealTarget();
          if (elementOnEvent.getQualifier() instanceof DartPropertyAccess) {
            DartPropertyAccess elementOn = (DartPropertyAccess) elementOnEvent.getQualifier();
            DartNode element = elementOn.getQualifier();
            if (elementOn.getName().getName().equals("on")
                && element.getType() instanceof InterfaceType) {
              InterfaceType elementType = (InterfaceType) element.getType();
              if (isSubType(elementType, "Element", "dart://html/dartium/html_dartium.dart")) {
                String eventName = elementOnEvent.getName().getName();
                String onEventName = "on" + StringUtils.capitalize(eventName) + ".listen";
                addReplaceEdit(
                    SourceRangeFactory.forStartEnd(elementOn.getName(), nameNode),
                    onEventName);
              }
            }
          }
        }
        // done
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitNewExpression(DartNewExpression node) {
        ConstructorElement element = node.getElement();
        DartNode constructor = node.getConstructor();
        List<DartExpression> args = node.getArguments();
        // new X(y[, ...])
        if (args.size() >= 1 && element != null) {
          InterfaceType createdType = element.getConstructorType().getType();
          // Timer
          if (isSubType(createdType, "Timer", "dart://async/async.dart")
              && isArgInterfaceType(args, 0, "int")) {
            super.visitNewExpression(node);
            // new Timer(0, (){})  --->  Timer.run((){})
            if (args.get(0) instanceof DartIntegerLiteral) {
              DartIntegerLiteral msLiteral = (DartIntegerLiteral) args.get(0);
              if (msLiteral.getValue().equals(BigInteger.ZERO)) {
                addReplaceEdit(SourceRangeFactory.forStartStart(node, constructor), "");
                addReplaceEdit(SourceRangeFactory.forEndStart(constructor, args.get(1)), ".run(");
                return null;
              }
            }
            // new Timer(ms, (){})  --->  new Timer(const Duration(milliseconds: ms), (){})
            addReplaceEdit(
                SourceRangeFactory.forStartLength(args.get(0), 0),
                "const Duration(milliseconds: ");
            addReplaceEdit(SourceRangeFactory.forEndLength(args.get(0), 0), ")");
            return null;
          }
          // new Future.delayed(ms[, computation()])
          if (isSubType(createdType, "Future", "dart://async/async.dart")
              && constructor instanceof DartPropertyAccess) {
            DartPropertyAccess prop = (DartPropertyAccess) constructor;
            if (prop.getName().toString().equals("delayed") && isArgInterfaceType(args, 0, "int")) {
              super.visitNewExpression(node);
              addReplaceEdit(
                  SourceRangeFactory.forStartLength(args.get(0), 0),
                  "const Duration(milliseconds: ");
              addReplaceEdit(SourceRangeFactory.forEndLength(args.get(0), 0), ")");
              return null;
            }
          }
        }
        // named constructors rename
        if (constructor instanceof DartPropertyAccess) {
          DartPropertyAccess prop = (DartPropertyAccess) constructor;
          String typeName = prop.getQualifier().toSource();
          String accessor = prop.getName().toSource();
          // new DateTime.fromString("...")  --->  DateTime.parse("...")
          if ((typeName.equals("Date") || typeName.equals("DateTime"))
              && accessor.equals("fromString") && args.size() == 1) {
            addReplaceEdit(SourceRangeFactory.forStartStart(node, args.get(0)), "DateTime.parse(");
            return null;
          }
          // new Uri.fromString("...")  --->  Uri.parse("...")
          if ((typeName.equals("Uri")) && accessor.equals("fromString") && args.size() == 1) {
            addReplaceEdit(SourceRangeFactory.forStartStart(node, args.get(0)), "Uri.parse(");
            return null;
          }
        }
        return super.visitNewExpression(node);
      }

      @Override
      public Void visitTypeNode(DartTypeNode node) {
        super.visitTypeNode(node);
        // was in "dart:core", now removed
        if (node.getIdentifier() instanceof DartIdentifier
            && ((DartIdentifier) node.getIdentifier()).getName().equals("IllegalJSRegExpException")) {
          addReplaceEdit(SourceRangeFactory.create(node), "FormatException");
          return null;
        }
        // handle as yet existing
        if (node.getIdentifier() instanceof DartIdentifier
            && ((DartIdentifier) node.getIdentifier()).getName().equals("Date")) {
          addReplaceEdit(SourceRangeFactory.create(node), "DateTime");
          return null;
        }
        return null;
      }

      @Override
      public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
        // Iterator  --->  HasNextIterator
        {
          DartIdentifier nameNode = node.getTarget();
          replaceIteratorType(node, nameNode);
        }
        return super.visitUnqualifiedInvocation(node);
      }

      private void convertIteratorImplementation(DartClass node, Type intType) {
        // may be already migrated
        for (DartNode member : node.getMembers()) {
          if (member instanceof DartMethodDefinition) {
            DartMethodDefinition method = (DartMethodDefinition) member;
            DartExpression nameNode = method.getName();
            if (nameNode.toString().equals("moveNext")) {
              return;
            }
          }
        }
        // prepare element type
        String elementTypeSource = null;
        {
          String typeArgs = getTypeArgsSource(intType);
          typeArgs = StringUtils.remove(typeArgs, node.getName().getName() + ".");
          if (typeArgs.startsWith("<")) {
            elementTypeSource = StringUtils.substringBetween(typeArgs, "<", ">");
          }
        }
        // rename "hasNext" and "next"
        for (DartNode member : node.getMembers()) {
          // unwrap "field", extract "accessor"
          if (member instanceof DartFieldDefinition) {
            DartFieldDefinition fieldDefinition = (DartFieldDefinition) member;
            List<DartField> fields = fieldDefinition.getFields();
            for (DartField field : fields) {
              if (field.getAccessor() != null) {
                member = field.getAccessor();
              }
            }
          }
          // update method names
          if (member instanceof DartMethodDefinition) {
            DartMethodDefinition method = (DartMethodDefinition) member;
            DartExpression nameNode = method.getName();
            // hasNext  ---> _hasNext
            if (nameNode.toString().equals("hasNext")) {
              addReplaceEdit(SourceRangeFactory.create(nameNode), "_hasNext");
            }
            // next  ---> _next
            if (nameNode.toString().equals("next")) {
              addReplaceEdit(SourceRangeFactory.create(nameNode), "_next");
            }
          }
        }
        // add "moveNext" and "current"
        if (elementTypeSource != null) {
          SourceRange r = SourceRangeFactory.forStartLength(node.getCloseBraceOffset(), 0);
          String eol = utils.getEndOfLine();
          addReplaceEdit(r, "  " + elementTypeSource + " _current;" + eol);
          addReplaceEdit(r, "  bool moveNext() {" + eol);
          addReplaceEdit(r, "    if (_hasNext) {" + eol);
          addReplaceEdit(r, "      _current = _next();" + eol);
          addReplaceEdit(r, "      return true;" + eol);
          addReplaceEdit(r, "    }" + eol);
          addReplaceEdit(r, "    _current = null;" + eol);
          addReplaceEdit(r, "    return false;" + eol);
          addReplaceEdit(r, "  }" + eol);
          addReplaceEdit(r, "  " + elementTypeSource + " current => _current;" + eol);
        }
      }

      private String findSourceTypeName(DartExpression expression) {
        if (expression instanceof DartMethodInvocation) {
          DartMethodInvocation invocation = (DartMethodInvocation) expression;
          DartExpression target = invocation.getTarget();
          if (target != null) {
            Type sourceType = target.getType();
            if (TypeKind.of(sourceType) == TypeKind.INTERFACE) {
              String sourceTypeName = sourceType.getElement().getName();
              if (sourceTypeName.equals("List") || sourceTypeName.equals("Set")) {
                return sourceTypeName;
              }
            }
            return findSourceTypeName(target);
          }
        }
        return null;
      }

      /**
       * Replaces <code>iterator()</code> invocation with <code>iterator</code> property access.
       */
      private void replaceIteratorType(DartInvocation node, DartIdentifier nameNode) {
        if (node.getArguments().isEmpty() && Elements.isIdentifierName(nameNode, "iterator")) {
          String typeArg = getTypeArgsSource(node.getType());
          addReplaceEdit(SourceRangeFactory.forStartLength(node, 0), "new HasNextIterator"
              + typeArg + "(");
          addReplaceEdit(SourceRangeFactory.forEndEnd(nameNode, node), ")");
          replaceParentVariableDeclarationStatementType(node, typeArg);
        }
      }

      /**
       * If parent of "node" is {@link DartVariableStatement} with explicit type, then replaces its
       * type from <code>Iterator</code> to <code>HasNextIterator</code>.
       */
      private void replaceParentVariableDeclarationStatementType(DartInvocation node, String typeArg) {
        if (node.getParent() instanceof DartVariable) {
          DartVariable var = (DartVariable) node.getParent();
          if (var.getValue() == node && var.getParent() instanceof DartVariableStatement) {
            DartVariableStatement varStatement = (DartVariableStatement) var.getParent();
            DartTypeNode typeNode = varStatement.getTypeNode();
            if (typeNode != null) {
              addReplaceEdit(SourceRangeFactory.create(typeNode), "HasNextIterator" + typeArg);
            }
          }
        }
      }
    });
  }
}
