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

package com.google.dart.tools.internal.corext.codemanipulation;

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_INVOCATION_ARGS;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_NAMED_EXPRESSION_EXPRESSION;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.parser.Token;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @coverage dart.editor.ui.corext
 */
public class StubUtility {
  private static final String[] KNOWN_METHOD_NAME_PREFIXES = {"get", "is", "to"};

  public static String[] getVariableNameSuggestions(Type expectedType,
      DartExpression assignedExpression, Set<String> excluded) {
    Set<String> res = Sets.newLinkedHashSet();
    // use expression
    if (assignedExpression != null) {
      String nameFromExpression = getBaseNameFromExpression(assignedExpression);
      if (nameFromExpression != null) {
        addAll(excluded, res, getVariableNameSuggestions(nameFromExpression));
      }

      String nameFromParent = getBaseNameFromLocationInParent(assignedExpression);
      if (nameFromParent != null) {
        addAll(excluded, res, getVariableNameSuggestions(nameFromParent));
      }
    }
    // use type
    if (expectedType != null && TypeKind.of(expectedType) != TypeKind.DYNAMIC) {
      String typeName = ExtractUtils.getTypeSource(expectedType);
      if ("int".equals(typeName)) {
        addSingleCharacterName(excluded, res, 'i');
      } else if ("double".equals(typeName)) {
        addSingleCharacterName(excluded, res, 'd');
      } else {
        typeName = StringUtils.substringBefore(typeName, "<");
        addAll(excluded, res, getVariableNameSuggestions(typeName));
      }
      res.remove(typeName);
    }
    // done
    return res.toArray(new String[res.size()]);
  }

  /**
   * Adds "toAdd" items which are not excluded.
   */
  private static void addAll(Set<String> excluded, Set<String> result, Collection<String> toAdd) {
    for (String item : toAdd) {
      // add name based on "item", but not "excluded"
      for (int suffix = 1;; suffix++) {
        // prepare name, just "item" or "item2", "item3", etc
        String name = item;
        if (suffix > 1) {
          name += suffix;
        }
        // add once found not excluded
        if (!excluded.contains(name)) {
          result.add(name);
          break;
        }
      }
    }
  }

  private static void addSingleCharacterName(Set<String> excluded, Set<String> result, char c) {
    while (c < 'z') {
      String name = String.valueOf(c);
      // may be done
      if (!excluded.contains(name)) {
        result.add(name);
        break;
      }
      // next character
      c = (char) (c + 1);
    }
  }

  private static String getBaseNameFromExpression(DartExpression expression) {
    String name = null;
    if (expression instanceof DartBinaryExpression) {
      DartBinaryExpression binaryExpression = (DartBinaryExpression) expression;
      if (binaryExpression.getOperator() == Token.AS) {
        expression = binaryExpression.getArg1();
      }
    }
    if (expression instanceof DartIdentifier) {
      DartIdentifier node = (DartIdentifier) expression;
      return node.getName();
    } else if (expression instanceof DartPropertyAccess) {
      DartPropertyAccess node = (DartPropertyAccess) expression;
      return node.getName().getName();
    } else if (expression instanceof DartMethodInvocation) {
      name = ((DartMethodInvocation) expression).getFunctionName().getName();
    } else if (expression instanceof DartUnqualifiedInvocation) {
      name = ((DartUnqualifiedInvocation) expression).getTarget().getName();
    }
    if (name != null) {
      for (int i = 0; i < KNOWN_METHOD_NAME_PREFIXES.length; i++) {
        String curr = KNOWN_METHOD_NAME_PREFIXES[i];
        if (name.startsWith(curr)) {
          if (name.equals(curr)) {
            return null; // don't suggest 'get' as variable name
          } else if (Character.isUpperCase(name.charAt(curr.length()))) {
            return name.substring(curr.length());
          }
        }
      }
    }
    return name;
  }

  private static String getBaseNameFromLocationInParent(DartExpression expression) {
    StructuralPropertyDescriptor location = getLocationInParent(expression);
    // value in named expression
    if (location == DART_NAMED_EXPRESSION_EXPRESSION) {
      return ((DartNamedExpression) expression.getParent()).getName().getName();
    }
    // positional argument
    if (location == DART_INVOCATION_ARGS) {
      if (expression.getInvocationParameterId() instanceof VariableElement) {
        VariableElement parameter = (VariableElement) expression.getInvocationParameterId();
        return parameter.getName();
      }
    }
    // unknown
    return null;
  }

  /**
   * @return all variants of names by removing leading words by one.
   */
  private static List<String> getVariableNameSuggestions(String name) {
    List<String> result = Lists.newArrayList();
    String[] parts = name.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    for (int i = 0; i < parts.length; i++) {
      String suggestion = parts[i].toLowerCase() + StringUtils.join(parts, "", i + 1, parts.length);
      result.add(suggestion);
    }
    return result;
  }

}
