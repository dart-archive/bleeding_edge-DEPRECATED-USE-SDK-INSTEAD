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

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.common.Symbol;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.Type;

/**
 * Instances of the class <code>TypeReferencesContributor</code> implement a contributor that adds a
 * reference every time it finds a reference to a type.
 */
public class TypeReferencesContributor extends ScopedDartContributor {
  @Override
  public Void visitIdentifier(DartIdentifier node) {
    Symbol binding = node.getTargetSymbol();
    if (binding instanceof ClassElement) {
      process(node, (ClassElement) binding);
    }
    return super.visitIdentifier(node);
  }

  private void process(DartIdentifier node, ClassElement binding) {
    InterfaceType element = binding.getType();
    if (element != null) {
      processType(node, element);
    }
  }

  private void processType(DartIdentifier node, InterfaceType binding) {
    Type type = getDartElement(binding.asRawType());
    if (type != null) {
      recordRelationship(peekTarget(), new TypeLocation(type, new SourceRangeImpl(node)));
    }
  }
}
