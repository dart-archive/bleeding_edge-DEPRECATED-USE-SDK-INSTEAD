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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code CompilationUnitElementImpl} implement a
 * {@link CompilationUnitElement}.
 * 
 * @coverage dart.engine.element
 */
public class CompilationUnitElementImpl extends ElementImpl implements CompilationUnitElement {
  /**
   * An array containing all of the top-level accessors (getters and setters) contained in this
   * compilation unit.
   */
  private PropertyAccessorElement[] accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the top-level functions contained in this compilation unit.
   */
  private FunctionElement[] functions = FunctionElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the variables contained in this compilation unit.
   */
  private TopLevelVariableElement[] variables = TopLevelVariableElementImpl.EMPTY_ARRAY;

  /**
   * The source that corresponds to this compilation unit.
   */
  private Source source;

  /**
   * An array containing all of the function type aliases contained in this compilation unit.
   */
  private FunctionTypeAliasElement[] typeAliases = FunctionTypeAliasElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the types contained in this compilation unit.
   */
  private ClassElement[] types = ClassElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of compilation unit elements.
   */
  public static final CompilationUnitElement[] EMPTY_ARRAY = new CompilationUnitElement[0];

  /**
   * Initialize a newly created compilation unit element to have the given name.
   * 
   * @param name the name of this element
   */
  public CompilationUnitElementImpl(String name) {
    // Compilation units do not contain their own declaration.
    super(name, -1);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitCompilationUnitElement(this);
  }

  @Override
  public boolean equals(Object object) {
    return object != null && getClass() == object.getClass()
        && source.equals(((CompilationUnitElementImpl) object).getSource());
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    return accessors;
  }

  @Override
  public ElementImpl getChild(String identifier) {
    //
    // The casts in this method are safe because the set methods would have thrown a CCE if any of
    // the elements in the arrays were not of the expected types.
    //
    for (PropertyAccessorElement accessor : accessors) {
      if (((PropertyAccessorElementImpl) accessor).getIdentifier().equals(identifier)) {
        return (PropertyAccessorElementImpl) accessor;
      }
    }
    for (VariableElement variable : variables) {
      if (((VariableElementImpl) variable).getIdentifier().equals(identifier)) {
        return (VariableElementImpl) variable;
      }
    }
    for (ExecutableElement function : functions) {
      if (((ExecutableElementImpl) function).getIdentifier().equals(identifier)) {
        return (ExecutableElementImpl) function;
      }
    }
    for (FunctionTypeAliasElement typeAlias : typeAliases) {
      if (((FunctionTypeAliasElementImpl) typeAlias).getIdentifier().equals(identifier)) {
        return (FunctionTypeAliasElementImpl) typeAlias;
      }
    }
    for (ClassElement type : types) {
      if (((ClassElementImpl) type).getIdentifier().equals(identifier)) {
        return (ClassElementImpl) type;
      }
    }
    return null;
  }

  @Override
  public LibraryElement getEnclosingElement() {
    return (LibraryElement) super.getEnclosingElement();
  }

  @Override
  public FunctionElement[] getFunctions() {
    return functions;
  }

  @Override
  public FunctionTypeAliasElement[] getFunctionTypeAliases() {
    return typeAliases;
  }

  @Override
  public String getIdentifier() {
    return getSource().getEncoding();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.COMPILATION_UNIT;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public TopLevelVariableElement[] getTopLevelVariables() {
    return variables;
  }

  @Override
  public ClassElement getType(String className) {
    for (ClassElement type : types) {
      if (type.getName().equals(className)) {
        return type;
      }
    }
    return null;
  }

  @Override
  public ClassElement[] getTypes() {
    return types;
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

  /**
   * Set the top-level accessors (getters and setters) contained in this compilation unit to the
   * given accessors.
   * 
   * @param the top-level accessors (getters and setters) contained in this compilation unit
   */
  public void setAccessors(PropertyAccessorElement[] accessors) {
    for (PropertyAccessorElement accessor : accessors) {
      ((PropertyAccessorElementImpl) accessor).setEnclosingElement(this);
    }
    this.accessors = accessors;
  }

  /**
   * Set the top-level functions contained in this compilation unit to the given functions.
   * 
   * @param functions the top-level functions contained in this compilation unit
   */
  public void setFunctions(FunctionElement[] functions) {
    for (FunctionElement function : functions) {
      ((FunctionElementImpl) function).setEnclosingElement(this);
    }
    this.functions = functions;
  }

  /**
   * Set the source that corresponds to this compilation unit to the given source.
   * 
   * @param source the source that corresponds to this compilation unit
   */
  public void setSource(Source source) {
    this.source = source;
  }

  /**
   * Set the top-level variables contained in this compilation unit to the given variables.
   * 
   * @param variables the top-level variables contained in this compilation unit
   */
  public void setTopLevelVariables(TopLevelVariableElement[] variables) {
    for (TopLevelVariableElement field : variables) {
      ((TopLevelVariableElementImpl) field).setEnclosingElement(this);
    }
    this.variables = variables;
  }

  /**
   * Set the function type aliases contained in this compilation unit to the given type aliases.
   * 
   * @param typeAliases the function type aliases contained in this compilation unit
   */
  public void setTypeAliases(FunctionTypeAliasElement[] typeAliases) {
    for (FunctionTypeAliasElement typeAlias : typeAliases) {
      ((FunctionTypeAliasElementImpl) typeAlias).setEnclosingElement(this);
    }
    this.typeAliases = typeAliases;
  }

  /**
   * Set the types contained in this compilation unit to the given types.
   * 
   * @param types types contained in this compilation unit
   */
  public void setTypes(ClassElement[] types) {
    for (ClassElement type : types) {
      ((ClassElementImpl) type).setEnclosingElement(this);
    }
    this.types = types;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(accessors, visitor);
    safelyVisitChildren(functions, visitor);
    safelyVisitChildren(typeAliases, visitor);
    safelyVisitChildren(types, visitor);
    safelyVisitChildren(variables, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    if (source == null) {
      builder.append("{compilation unit}");
    } else {
      builder.append(source.getFullName());
    }
  }
}
