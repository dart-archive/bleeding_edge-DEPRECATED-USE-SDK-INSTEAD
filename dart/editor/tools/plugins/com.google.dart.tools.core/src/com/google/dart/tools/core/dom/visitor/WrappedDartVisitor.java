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
package com.google.dart.tools.core.dom.visitor;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPlainVisitor;

/**
 * The interface <code>WrappedDartVisitor</code> defines the behavior of a visitor that implements
 * {@link #preVisit(DartNode)} and {@link #postVisit(DartNode)} behavior.
 */
public interface WrappedDartVisitor<R> extends DartPlainVisitor<R> {
  /**
   * The given node has just been visited.
   * 
   * @param node the node that was just visited
   */
  public void postVisit(DartNode node);

  /**
   * The given node is about to be visited.
   * 
   * @param node the node that is about to be visited
   */
  public void preVisit(DartNode node);
}
