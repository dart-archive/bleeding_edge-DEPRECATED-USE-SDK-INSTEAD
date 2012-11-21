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

public class SimpleResolverTest extends ResolverTestCase {
  public void test_class_extends_implements() throws Exception {
    Source source = addSource("test.dart", createSource(//
        "class A extends B implements C {}",
        "class B {}",
        "class C {}"));
    Map<ASTNode, Element> resolvedElementMap = resolve(source);
    assertNoErrors();
    verify(resolvedElementMap, source);
  }

  public void test_empty() throws Exception {
    Source source = addSource("test.dart", "");
    Map<ASTNode, Element> resolvedElementMap = resolve(source);
    assertNoErrors();
    verify(resolvedElementMap, source);
  }
}
