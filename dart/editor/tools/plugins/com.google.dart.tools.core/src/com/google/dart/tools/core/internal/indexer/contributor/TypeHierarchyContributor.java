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
import com.google.dart.compiler.common.Symbol;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.model.Type;

/**
 * Instances of the class <code>TypeHierarchyContributor</code> implement a contributor that adds a
 * reference every time it finds a type that is a subtype of another type.
 */
public class TypeHierarchyContributor extends AbstractDartContributor {
  @Override
  public Void visitClass(DartClass node) {
    Symbol binding = node.getSymbol();
    if (binding instanceof ClassElement) {
      ClassElement typeSymbol = (ClassElement) binding;
      InterfaceType superclass = typeSymbol.getSupertype();
      if (superclass != null) {
        processSupertype(node, superclass);
      }
      for (InterfaceType type : typeSymbol.getInterfaces()) {
        processSupertype(node, type);
      }
    }
    return super.visitClass(node);
  }

  private void processSupertype(DartClass node, InterfaceType binding) {
    Type type = getDartElement(binding);
    if (type != null) {
      recordRelationship(node, new TypeLocation(type, getSourceRange(type, binding)));
    }
  }
}
