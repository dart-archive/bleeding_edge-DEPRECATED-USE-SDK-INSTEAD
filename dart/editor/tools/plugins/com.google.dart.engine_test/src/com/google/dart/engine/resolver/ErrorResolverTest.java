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
package com.google.dart.engine.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;

import java.util.Map;

public class ErrorResolverTest extends ResolverTestCase {
  public void fail_labelInOuterScope() throws Exception {
    Source source = addSource("test.dart", createSource(//
        "class int {}",
        "",
        "class A {",
        "  void m(int i) {",
        "    l: while (i > 0) {",
        "      void f() {",
        "        break l;",
        "      };",
        "    }",
        "  }",
        "}"));
    Map<ASTNode, Element> resolvedElementMap = resolve(source);
    assertErrors(ResolverErrorCode.LABEL_IN_OUTER_SCOPE);
    verify(resolvedElementMap, source);
  }

  public void test_breakLabelOnSwitchMember() throws Exception {
    Source source = addSource("test.dart", createSource(//
        "class int {}",
        "",
        "class A {",
        "  void m(int i) {",
        "    switch (i) {",
        "      l: case 0:",
        "        break;",
        "      case 1:",
        "        break l;",
        "    }",
        "  }",
        "}"));
    Map<ASTNode, Element> resolvedElementMap = resolve(source);
    assertErrors(ResolverErrorCode.BREAK_LABEL_ON_SWITCH_MEMBER);
    verify(resolvedElementMap, source);
  }

  public void test_cannotBeResolved_static() throws Exception {
    Source source = addSource("test.dart", createSource(//
        "class A {}",
        "",
        "var a = A.B;"));
    resolve(source);
    assertErrors(ResolverErrorCode.CANNOT_BE_RESOLVED);
    // We cannot verify resolution with undefined members
  }

  public void test_continueLabelOnSwitch() throws Exception {
    Source source = addSource("/a.dart", createSource(//
        "class int {}",
        "",
        "class A {",
        "  void m(int i) {",
        "    l: switch (i) {",
        "      case 0:",
        "        continue l;",
        "    }",
        "  }",
        "}"));
    Map<ASTNode, Element> resolvedElementMap = resolve(source);
    assertErrors(ResolverErrorCode.CONTINUE_LABEL_ON_SWITCH);
    verify(resolvedElementMap, source);
  }

  public void test_duplicateMemberError() {
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "",
        "part 'a.dart';",
        "part 'b.dart';"));
    Source sourceA = addSource("/a.dart", createSource(//
        "part of lib;",
        "",
        "class A {}"));
    Source sourceB = addSource("/b.dart", createSource(//
        "part of lib;",
        "",
        "class A {}"));
    Map<ASTNode, Element> resolvedElementMap = resolve(librarySource, sourceA, sourceB);
    assertErrors(ResolverErrorCode.DUPLICATE_MEMBER_ERROR);
    verify(resolvedElementMap, librarySource, sourceA, sourceB);
  }

  public void test_undefinedLabel() throws Exception {
    Source source = addSource("test.dart", createSource(//
        "class int {}",
        "",
        "class A {",
        "  void m(int i) {",
        "    while (i > 0) {",
        "      break l;",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertErrors(ResolverErrorCode.UNDEFINED_LABEL);
    // We cannot verify resolution with undefined labels
  }
}
