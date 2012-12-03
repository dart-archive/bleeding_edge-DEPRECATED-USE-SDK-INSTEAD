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
package com.google.dart.tools.ui.correction;

import com.google.common.collect.ImmutableSet;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.type.Type;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.internal.corext.codemanipulation.StubUtility;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.refactoring.AbstractDartTest;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Set;

/**
 * Test for {@link StubUtility}.
 */
public final class StubUtilityTest extends AbstractDartTest {
  private int nameSuggestOffset;
  private boolean nameSuggestUseParent;
  private boolean nameSuggestUseVariableType;
  private Set<String> nameSuggestExclude = ImmutableSet.of();

  public void test_getVariableNameSuggestions_expectedType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  TreeNode node = null;",
        "}");
    nameSuggestExclude = ImmutableSet.of("");
    nameSuggestOffset = findOffset("null");
    nameSuggestUseVariableType = true;
    assert_getVariableNameSuggestions("treeNode", "node");
  }

  public void test_getVariableNameSuggestions_expectedType_double() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  double res = 0;",
        "}");
    nameSuggestOffset = findOffset("0;");
    nameSuggestUseVariableType = true;
    // first choice for "double" is "d"
    {
      nameSuggestExclude = ImmutableSet.of("");
      assert_getVariableNameSuggestions("d");
    }
    // if "d" is used, try "e", "f", etc
    {
      nameSuggestExclude = ImmutableSet.of("d", "e");
      assert_getVariableNameSuggestions("f");
    }
  }

  public void test_getVariableNameSuggestions_expectedType_int() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class TreeNode {}",
        "main() {",
        "  int res = 0;",
        "}");
    nameSuggestOffset = findOffset("0;");
    nameSuggestUseVariableType = true;
    // first choice for "int" is "i"
    {
      nameSuggestExclude = ImmutableSet.of("");
      assert_getVariableNameSuggestions("i");
    }
    // if "i" is used, try "j", "k", etc
    {
      nameSuggestExclude = ImmutableSet.of("i", "j");
      assert_getVariableNameSuggestions("k");
    }
  }

  public void test_getVariableNameSuggestions_forText() throws Exception {
    {
      String[] suggestions = StubUtility.getVariableNameSuggestions(
          "Goodbye, cruel world!",
          nameSuggestExclude);
      assertThat(suggestions).isEqualTo(new String[] {"goodbyeCruelWorld", "cruelWorld", "world"});
    }
    {
      nameSuggestExclude = ImmutableSet.of("world");
      String[] suggestions = StubUtility.getVariableNameSuggestions(
          "Goodbye, cruel world!",
          nameSuggestExclude);
      assertThat(suggestions).isEqualTo(new String[] {"goodbyeCruelWorld", "cruelWorld", "world2"});
    }
  }

  public void test_getVariableNameSuggestions_invocationArgument_named() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo([a, b, c]) {}",
        "main() {",
        "  foo(111, c: 333, b: 222);",
        "}");
    nameSuggestExclude = ImmutableSet.of("");
    {
      nameSuggestOffset = findOffset("111");
      assert_getVariableNameSuggestions("a");
    }
    {
      nameSuggestOffset = findOffset("222");
      assert_getVariableNameSuggestions("b");
    }
    {
      nameSuggestOffset = findOffset("333");
      assert_getVariableNameSuggestions("c");
    }
  }

  public void test_getVariableNameSuggestions_invocationArgument_positional() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(a, b) {}",
        "main() {",
        "  foo(1, 2);",
        "}");
    nameSuggestExclude = ImmutableSet.of("");
    {
      nameSuggestOffset = findOffset("1,");
      assert_getVariableNameSuggestions("a");
    }
    {
      nameSuggestOffset = findOffset("2);");
      assert_getVariableNameSuggestions("b");
    }
  }

  public void test_getVariableNameSuggestions_Node_cast() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var sortedNodes;",
        "  var res = sortedNodes as String;",
        "}");
    nameSuggestOffset = findOffset("as String");
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_Node_methodInvocation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = doc.getSortedNodes();",
        "}");
    nameSuggestOffset = findOffset("getSortedNodes()");
    nameSuggestUseParent = true;
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions("sortedNodes", "nodes");
  }

  /**
   * "get" is valid, but not nice name.
   */
  public void test_getVariableNameSuggestions_Node_name_get() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = doc.get();",
        "}");
    nameSuggestOffset = findOffset("get()");
    nameSuggestUseParent = true;
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions();
  }

  public void test_getVariableNameSuggestions_Node_noPrefix() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = sortedNodes();",
        "}");
    nameSuggestOffset = findOffset("sortedNodes()");
    nameSuggestUseParent = true;
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_Node_propertyAccess() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = doc.sortedNodes;",
        "}");
    nameSuggestOffset = findOffset("sortedNodes");
    nameSuggestUseParent = true;
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_Node_simpleName() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var sortedNodes = null;",
        "  var res = sortedNodes;",
        "}");
    nameSuggestOffset = findOffset("sortedNodes;");
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_Node_unqualifiedInvocation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = getSortedNodes();",
        "}");
    nameSuggestOffset = findOffset("getSortedNodes()");
    nameSuggestUseParent = true;
    nameSuggestExclude = ImmutableSet.of("");
    assert_getVariableNameSuggestions("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_Node_withExclude() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var sortedTreeNodes = null;",
        "  var res = sortedTreeNodes;",
        "}");
    nameSuggestOffset = findOffset("sortedTreeNodes;");
    nameSuggestExclude = ImmutableSet.of("treeNodes");
    assert_getVariableNameSuggestions("sortedTreeNodes", "treeNodes2", "nodes");
  }

  public void test_getVariableNameSuggestions_String_multipleUpper() throws Exception {
    List<String> suggestions = getVariableNameSuggestions_String("sortedHTMLNodes");
    assertThat(suggestions).containsExactly("sortedHTMLNodes", "htmlNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_String_simpleCamel() throws Exception {
    List<String> suggestions = getVariableNameSuggestions_String("sortedNodes");
    assertThat(suggestions).containsExactly("sortedNodes", "nodes");
  }

  public void test_getVariableNameSuggestions_String_simpleName() throws Exception {
    List<String> suggestions = getVariableNameSuggestions_String("name");
    assertThat(suggestions).containsExactly("name");
  }

  private void assert_getVariableNameSuggestions(String... expected) throws Exception {
    String[] suggestions = getVariableNameSuggestions();
    assertThat(suggestions).isEqualTo(expected);
  }

  private String[] getVariableNameSuggestions() throws Exception {
    // prepare expression
    DartExpression expression;
    {
      ExtractUtils utils = new ExtractUtils(testUnit);
      DartNode coveringNode = NodeFinder.find(utils.getUnitNode(), nameSuggestOffset, 0).getCoveringNode();
      if (nameSuggestUseParent) {
        coveringNode = coveringNode.getParent();
      }
      expression = (DartExpression) coveringNode;
      assertNotNull(expression);
    }
    // prepare type
    Type expectedType = null;
    if (nameSuggestUseVariableType) {
      expectedType = ((DartVariable) expression.getParent()).getElement().getType();
    }
    // get suggestions
    return StubUtility.getVariableNameSuggestions(expectedType, expression, nameSuggestExclude);
  }

  /**
   * Calls {@link StubUtility#getVariableNameSuggestions(String)}.
   */
  @SuppressWarnings("unchecked")
  private List<String> getVariableNameSuggestions_String(String name) {
    return (List<String>) ReflectionUtils.invokeMethod(
        StubUtility.class,
        "getVariableNameSuggestions(java.lang.String)",
        name);
  }
}
