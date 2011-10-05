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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.tools.core.internal.indexer.location.SyntheticLocation;

/**
 * Instances of the class <code>ElementsByCategoryContributor</code> implement a contributor that
 * adds a reference from elements contained in a category to a synthetic location representing that
 * category.
 */
public class ElementsByCategoryContributor extends AbstractDartContributor {
  @Override
  public Void visitClass(DartClass node) {
    if (node.isInterface()) {
      recordRelationship(node, SyntheticLocation.ALL_INTERFACES);
    } else {
      recordRelationship(node, SyntheticLocation.ALL_CLASSES);
    }
    return super.visitClass(node);
  }

  @Override
  public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    recordRelationship(node, SyntheticLocation.ALL_FUNCTION_TYPE_ALIASES);
    return super.visitFunctionTypeAlias(node);
  }
}
