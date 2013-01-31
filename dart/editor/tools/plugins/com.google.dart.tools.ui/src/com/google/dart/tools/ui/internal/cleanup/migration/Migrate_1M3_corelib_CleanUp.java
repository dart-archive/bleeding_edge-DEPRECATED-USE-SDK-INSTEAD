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

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartIdentifier;
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
import com.google.dart.compiler.resolver.ClassNodeElement;
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

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_FOR_IN_STATEMENT_ITERABLE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_METHOD_INVOCATION_TARGET;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_VARIABLE_VALUE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;
import static com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp.isSubType;

import java.text.MessageFormat;
import java.util.List;

/**
 * In 1.0 M3 many change to the <code>Iterator</code>, <code>Iterable</code> and <code>Future</code>
 * will be done.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M3_corelib_CleanUp extends AbstractMigrateCleanUp {

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
      return "mappedBy".equals(name) || "map".equals(name) || "where".equals(name)
          || "filter".equals(name) || "contains".equals(name) || "forEach".equals(name)
          || "reduce".equals(name) || "every".equals(name) || "some".equals(name);
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
        if (Elements.isIdentifierName(nameNode, "iterator")) {
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
              // map()  --->  mappedBy()
              if (isMapMethod && isSet) {
                String src = MessageFormat.format(
                    "Iterable mappedBy(f({0} element)) => new MappedIterable<{0}, dynamic>(this, f);",
                    typeVarName);
                addReplaceEdit(SourceRangeFactory.create(node), src);
              }
              if (isMapMethod && isList) {
                String src = MessageFormat.format(
                    "List mappedBy(f({0} element)) => new MappedList<{0}, dynamic>(this, f);",
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
        // Iterator  --->  HasNextIterator
        replaceIteratorType(node, nameNode);
        // filter -> where      map -> mappedBy
        {
          boolean wasFilter = Elements.isIdentifierName(nameNode, "filter");
          boolean wasMap = Elements.isIdentifierName(nameNode, "map");
          if (wasFilter || wasMap) {
            if (wasFilter) {
              addReplaceEdit(SourceRangeFactory.create(nameNode), "where");
            }
            if (wasMap) {
              addReplaceEdit(SourceRangeFactory.create(nameNode), "mappedBy");
            }
            if (!isUsedAsIterable(node)) {
              String sourceTypeName = findSourceTypeName(node);
              if (sourceTypeName.equals("List")) {
                addReplaceEdit(SourceRangeFactory.forEndLength(node, 0), ".toList()");
              }
              if (sourceTypeName.equals("Set")) {
                addReplaceEdit(SourceRangeFactory.forEndLength(node, 0), ".toSet()");
              }
            }
          }
        }
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitNewExpression(DartNewExpression node) {
        ConstructorElement element = node.getElement();
        List<DartExpression> args = node.getArguments();
        // new List(5)  --->  new List.fixedLength(5)
        if (element != null && element.getConstructorType().getName().equals("List")
            && StringUtils.isEmpty(element.getName()) && args.size() == 1) {
          DartExpression arg = args.get(0);
          if (arg != null) {
            Type argType = arg.getType();
            if (argType != null && argType.toString().equals("int")) {
              addReplaceEdit(
                  SourceRangeFactory.forEndStart(node.getConstructor(), arg),
                  ".fixedLength(");
            }
          }
        }
        DartNode constructor = node.getConstructor();
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
        Type type = node.getType();
        if (type != null && type.getElement() instanceof ClassNodeElement) {
          ClassNodeElement element = (ClassNodeElement) type.getElement();
          if (element != null && element.getName().equals("Date")
              && element.getLibrary().getName().equals("dart://core/core.dart")) {
            addReplaceEdit(SourceRangeFactory.create(node), "DateTime");
          }
        }
        return super.visitTypeNode(node);
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
