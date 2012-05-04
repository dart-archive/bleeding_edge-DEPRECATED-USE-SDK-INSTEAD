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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.indexer.location.CompilationUnitLocation;
import com.google.dart.tools.core.internal.indexer.location.DartElementLocation;
import com.google.dart.tools.core.internal.indexer.location.FieldLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionLocation;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

import java.util.ArrayList;

/**
 * The abstract class <code>ScopedDartContributor</code> defines the behavior common to contributors
 * whose target is based on the enclosing lexical scope.
 */
public abstract class ScopedDartContributor extends DartContributor {
  /**
   * A stack whose top element (the element with the largest index) is the location of the
   * inner-most enclosing scope.
   */
  private ArrayList<DartElementLocation> locationStack = new ArrayList<DartElementLocation>();

  /**
   * Initialize a newly created contributor.
   */
  public ScopedDartContributor() {
    super();
  }

  /**
   * Enter the scope of the given class. If a subclass needs to process the class before entering
   * the scope it can do so by overriding this method like the following:
   * 
   * <pre>
   * public Void visitClass(DartClass node) {
   *   // Process the class. The current scope will be the enclosing scope.
   *   return super.visitClass(node);
   * }
   * </pre>
   * 
   * @param node the class being entered
   */
  @Override
  public Void visitClass(DartClass node) {
    Type type = BindingUtils.getDartElement(getCompilationUnit(), node);
    if (type == null) {
      pushTarget(null);
    } else {
      pushTarget(new TypeLocation(type, new SourceRangeImpl(node.getName())));
    }
    try {
      super.visitClass(node);
    } finally {
      popTarget();
    }
    return null;
  }

  /**
   * Enter the scope of the given field. If a subclass needs to process the field before entering
   * the scope it can do so by overriding this method like the following:
   * 
   * <pre>
   * public Void visitFieldDefinition(DartFieldDefinition node) {
   *   // Process the field. The current scope will be the enclosing scope.
   *   return super.visitFieldDefinition(node);
   * }
   * </pre>
   * 
   * @param node the field being entered
   */
  @Override
  public Void visitField(DartField node) {
    Field field = BindingUtils.getDartElement(getCompilationUnit(), node);
    if (field == null) {
      pushTarget(null);
    } else {
      pushTarget(new FieldLocation(field, new SourceRangeImpl(node.getName())));
    }
    try {
      super.visitField(node);
    } finally {
      popTarget();
    }
    return null;
  }

  /**
   * Enter the scope of the given function. If a subclass needs to process the function before
   * entering the scope it can do so by overriding this method like the following:
   * 
   * <pre>
   * public Void visitFunction(DartFunction node) {
   *   // Process the function. The current scope will be the enclosing scope.
   *   return super.visitFunction(node);
   * }
   * </pre>
   * 
   * @param node the function being entered
   */
  @Override
  public Void visitFunction(DartFunction node) {
    if (node.getParent() instanceof DartMethodDefinition) {
      super.visitFunction(node);
      return null;
    }
    com.google.dart.tools.core.model.DartFunction function = BindingUtils.getDartElement(
        getCompilationUnit(),
        node);
    if (function == null) {
      pushTarget(null);
    } else {
      pushTarget(new FunctionLocation(function, new SourceRangeImpl(node)));
    }
    try {
      super.visitFunction(node);
    } finally {
      popTarget();
    }
    return null;
  }

  /**
   * Enter the scope of the given method. If a subclass needs to process the method before entering
   * the scope it can do so by overriding this method like the following:
   * 
   * <pre>
   * public Void visitMethodDefinition(DartMethodDefinition node) {
   *   // Process the method. The current scope will be the enclosing scope.
   *   return super.visitMethodDefinition(node);
   * }
   * </pre>
   * 
   * @param node the method being entered
   */
  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    com.google.dart.tools.core.model.DartFunction function = BindingUtils.getDartElement(
        getCompilationUnit(),
        node);
    if (function instanceof Method) {
      pushTarget(new MethodLocation((Method) function, new SourceRangeImpl(node.getName())));
    } else if (function != null) {
      pushTarget(new FunctionLocation(function, new SourceRangeImpl(node.getName())));
    } else {
      pushTarget(null);
    }
    try {
      super.visitMethodDefinition(node);
    } finally {
      popTarget();
    }
    return null;
  }

  /**
   * Enter the scope of the given compilation unit.
   * 
   * @param node the compilation unit being entered
   */
  @Override
  public Void visitUnit(DartUnit node) {
    CompilationUnit unit = getCompilationUnit();
    if (unit == null) {
      pushTarget(null);
    } else {
      pushTarget(new CompilationUnitLocation(unit, new SourceRangeImpl(node)));
    }
    try {
      super.visitUnit(node);
    } finally {
      popTarget();
    }
    return null;
  }

  /**
   * Return the location of the inner-most enclosing scope.
   * 
   * @return the location of the inner-most enclosing scope
   */
  protected DartElementLocation peekTarget() {
    for (int i = locationStack.size() - 1; i >= 0; i--) {
      DartElementLocation location = locationStack.get(i);
      if (location != null) {
        return location;
      }
    }
    return null;
  }

  /**
   * Return the location of the inner-most enclosing scope, but substitute the given source range
   * for the default source range.
   * 
   * @param sourceRange the source range to use for the returned location
   * @return the location of the inner-most enclosing scope
   */
  protected DartElementLocation peekTarget(SourceRange sourceRange) {
    for (int i = locationStack.size() - 1; i >= 0; i--) {
      DartElementLocation location = locationStack.get(i);
      if (location != null) {
        return location;
//        return DartElementLocations.byDartElement(location.getDartElement(), sourceRange);
      }
    }
    return null;
  }

  /**
   * Exit the current scope.
   */
  private void popTarget() {
    locationStack.remove(locationStack.size() - 1);
  }

  /**
   * Enter a new scope represented by the given location.
   * 
   * @param source the location of the scope being entered
   */
  private void pushTarget(DartElementLocation source) {
    locationStack.add(source);
  }
}
