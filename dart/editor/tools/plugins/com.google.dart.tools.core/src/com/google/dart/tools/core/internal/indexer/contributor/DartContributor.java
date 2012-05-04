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

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.exceptions.IndexRequestFailedUnchecked;
import com.google.dart.indexer.index.configuration.Contributor;
import com.google.dart.indexer.index.updating.LayerUpdater;
import com.google.dart.indexer.locations.Location;
import com.google.dart.tools.core.internal.indexer.location.FieldLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionLocation;
import com.google.dart.tools.core.internal.indexer.location.FunctionTypeAliasLocation;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.internal.indexer.location.TypeLocation;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

public class DartContributor extends ASTVisitor<Void> implements Contributor {
  /**
   * The compilation unit that is being visited.
   */
  private CompilationUnit compilationUnit;

  /**
   * The updater used to contribute relationships to the layer with which this contributor is
   * associated.
   */
  private LayerUpdater layerUpdater;

  /**
   * Initialize a newly created contributor.
   */
  public DartContributor() {
  }

  /**
   * Initialize this contributor to visit the nodes in the given compilation unit, using the given
   * layer updater to update the layer being contributed to.
   * 
   * @param compilationUnit the compilation unit that is about to be visited
   * @param layerUpdater the updater used to contribute relationships to the appropriate layer
   */
  public void initialize(CompilationUnit compilationUnit, LayerUpdater layerUpdater) {
    this.compilationUnit = compilationUnit;
    this.layerUpdater = layerUpdater;
  }

  /**
   * Return the compilation unit currently being visited, or <code>null</code> if we are not
   * currently visiting a compilation unit.
   * 
   * @return the compilation unit currently being visited
   */
  protected CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * Return the Dart model element corresponding to the given resolved field.
   * 
   * @param fieldBinding the resolved field used to locate the model element
   * @return the Dart model element corresponding to the resolved field
   */
  protected CompilationUnitElement getDartElement(FieldElement fieldBinding) {
    DartLibrary library = compilationUnit.getAncestor(DartLibrary.class);
    return BindingUtils.getDartElement(library, fieldBinding);
  }

  /**
   * Return the Dart model element corresponding to the given resolved type.
   * 
   * @param typeBinding the resolved type used to locate the model element
   * @return the Dart model element corresponding to the resolved type
   */
  protected CompilationUnitElement getDartElement(InterfaceType typeBinding) {
    DartLibrary library = compilationUnit.getAncestor(DartLibrary.class);
    return BindingUtils.getDartElement(library, typeBinding);
  }

  /**
   * Return the Dart model element corresponding to the given resolved method.
   * 
   * @param methodBinding the resolved method used to locate the model element
   * @return the Dart model element corresponding to the resolved method
   */
  protected DartFunction getDartElement(MethodElement methodBinding) {
    DartLibrary library = compilationUnit.getAncestor(DartLibrary.class);
    return BindingUtils.getDartElement(library, methodBinding);
  }

  /**
   * Return the Dart model element corresponding to the given resolved local variable.
   * 
   * @param variableBinding the resolved local variable used to locate the model element
   * @return the Dart model element corresponding to the resolved field
   */
  protected DartVariableDeclaration getDartElement(VariableElement variableBinding) {
    DartLibrary library = compilationUnit.getAncestor(DartLibrary.class);
    return BindingUtils.getDartElement(library, variableBinding);
  }

  protected SourceRange getSourceRange(SourceReference sourceReference, Element element) {
    SourceRange range = null;
    if (sourceReference != null) {
      try {
        range = sourceReference.getNameRange();
      } catch (DartModelException exception) {
      }
    }
    if (range == null && element != null) {
      range = new SourceRangeImpl(element);
    }
    if (range == null && sourceReference != null) {
      try {
        range = sourceReference.getSourceRange();
      } catch (DartModelException exception) {
        // Fall through to create an empty range
      }
    }
    if (range == null) {
      range = new SourceRangeImpl(0, 0);
    }
    return range;
  }

  protected SourceRange getSourceRange(SourceReference element, InterfaceType type) {
    SourceRange range = null;
    if (element != null) {
      try {
        range = element.getNameRange();
      } catch (DartModelException exception) {
        // Fall through to try other ways of getting the information
      }
    }
    if (range == null && type != null) {
      ClassElement classElement = type.getElement();
      if (classElement != null) {
        range = new SourceRangeImpl(classElement);
      }
    }
    if (range == null && element != null) {
      try {
        range = element.getSourceRange();
      } catch (DartModelException exception) {
        // Fall through to create an empty range
      }
    }
    if (range == null) {
      range = new SourceRangeImpl(0, 0);
    }
    return range;
  }

  /**
   * Record the implied relationship between the source and target.
   * 
   * @param source the source of the relationship
   * @param target the location of the target of the relationship
   * @throws IndexRequestFailedUnchecked if the relationship could not be recorded
   */
  protected void recordRelationship(com.google.dart.compiler.ast.DartFunction source,
      Location target) {
    DartFunction function = BindingUtils.getDartElement(compilationUnit, source);
    if (function != null) {
      recordRelationship(
          new FunctionLocation(function, getSourceRange(function, (Element) null)),
          target);
    }
  }

  /**
   * Record the implied relationship between the source and target.
   * 
   * @param source the source of the relationship
   * @param target the location of the target of the relationship
   * @throws IndexRequestFailedUnchecked if the relationship could not be recorded
   */
  protected void recordRelationship(DartClass source, Location target) {
    Type type = BindingUtils.getDartElement(compilationUnit, source);
    if (type != null) {
      recordRelationship(new TypeLocation(type, getSourceRange(type, (Element) null)), target);
    }
  }

  /**
   * Record the implied relationship between the source and target.
   * 
   * @param source the source of the relationship
   * @param target the location of the target of the relationship
   * @throws IndexRequestFailedUnchecked if the relationship could not be recorded
   */
  protected void recordRelationship(DartField source, Location target) {
    Field field = BindingUtils.getDartElement(compilationUnit, source);
    if (field != null) {
      recordRelationship(new FieldLocation(field, getSourceRange(field, (Element) null)), target);
    }
  }

  /**
   * Record the implied relationship between the source and target.
   * 
   * @param source the source of the relationship
   * @param target the location of the target of the relationship
   * @throws IndexRequestFailedUnchecked if the relationship could not be recorded
   */
  protected void recordRelationship(DartFunctionTypeAlias source, Location target) {
    com.google.dart.tools.core.model.DartFunctionTypeAlias alias = BindingUtils.getDartElement(
        compilationUnit,
        source);
    if (alias != null) {
      recordRelationship(
          new FunctionTypeAliasLocation(alias, getSourceRange(alias, (Element) null)),
          target);
    }
  }

  /**
   * Record the implied relationship between the source and target.
   * 
   * @param source the source of the relationship
   * @param target the location of the target of the relationship
   * @throws IndexRequestFailedUnchecked if the relationship could not be recorded
   */
  protected void recordRelationship(DartMethodDefinition source, Location target) {
    com.google.dart.tools.core.model.DartFunction function = BindingUtils.getDartElement(
        getCompilationUnit(),
        source);
    if (function instanceof Method) {
      Location location = new MethodLocation((Method) function, getSourceRange(
          function,
          (Element) null));
      recordRelationship(location, target);
    } else if (function != null) {
      Location location = new FunctionLocation(function, getSourceRange(function, (Element) null));
      recordRelationship(location, target);
    }
  }

  /**
   * Record the implied relationship between the source and target.
   * 
   * @param source the location of the source of the relationship
   * @param target the location of the target of the relationship
   * @throws IndexRequestFailedUnchecked if the relationship could not be recorded
   */
  protected void recordRelationship(Location source, Location target) {
    try {
      layerUpdater.startLocation(source).hasReferenceTo(target);
    } catch (IndexRequestFailed exception) {
      throw new IndexRequestFailedUnchecked(exception);
    }
  }
}
