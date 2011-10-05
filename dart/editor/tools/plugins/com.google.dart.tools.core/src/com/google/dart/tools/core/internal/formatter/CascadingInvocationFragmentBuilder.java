/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;

import java.util.ArrayList;

class CascadingInvocationFragmentBuilder extends DartNodeTraverser<DartNode> {

  ArrayList<DartInvocation> fragmentsList;

  CascadingInvocationFragmentBuilder() {
    fragmentsList = new ArrayList<DartInvocation>();
  }

  public DartInvocation[] fragments() {
    DartInvocation[] fragments = new DartInvocation[fragmentsList.size()];
    fragmentsList.toArray(fragments);
    return fragments;
  }

  public int size() {
    return fragmentsList.size();
  }

  @Override
  public DartNode visitFunctionObjectInvocation(DartFunctionObjectInvocation invocation) {
    if (!(invocation.getTarget().getParent() instanceof DartParenthesizedExpression)) {
      if (invocation.getTarget() instanceof DartInvocation) {
        fragmentsList.add(0, invocation);
        invocation.getTarget().accept(this);
        return null;
      }
      fragmentsList.add(0, invocation);
      fragmentsList.add(1, invocation);
    } else {
      fragmentsList.add(0, invocation);
      fragmentsList.add(1, invocation);
    }
    return null;
  }

  @Override
  public DartNode visitMethodInvocation(DartMethodInvocation invocation) {
    if (!(invocation.getTarget().getParent() instanceof DartParenthesizedExpression)) {
      if (invocation.getTarget() instanceof DartInvocation) {
        fragmentsList.add(0, invocation);
        invocation.getTarget().accept(this);
        return null;
      }
      fragmentsList.add(0, invocation);
      fragmentsList.add(1, invocation);
    } else {
      fragmentsList.add(0, invocation);
      fragmentsList.add(1, invocation);
    }
    return null;
  }

  @Override
  public DartNode visitUnqualifiedInvocation(DartUnqualifiedInvocation invocation) {
    fragmentsList.add(0, invocation);
    fragmentsList.add(1, invocation);
    return null;
  }
}
