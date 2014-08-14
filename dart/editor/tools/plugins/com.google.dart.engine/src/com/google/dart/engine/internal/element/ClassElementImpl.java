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

import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.engine.utilities.translation.DartName;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Instances of the class {@code ClassElementImpl} implement a {@code ClassElement}.
 * 
 * @coverage dart.engine.element
 */
public class ClassElementImpl extends ElementImpl implements ClassElement {
  /**
   * An array containing all of the accessors (getters and setters) contained in this class.
   */
  private PropertyAccessorElement[] accessors = PropertyAccessorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the constructors contained in this class.
   */
  private ConstructorElement[] constructors = ConstructorElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the fields contained in this class.
   */
  private FieldElement[] fields = FieldElementImpl.EMPTY_ARRAY;

  /**
   * An array containing all of the mixins that are applied to the class being extended in order to
   * derive the superclass of this class.
   */
  private InterfaceType[] mixins = InterfaceType.EMPTY_ARRAY;

  /**
   * An array containing all of the interfaces that are implemented by this class.
   */
  private InterfaceType[] interfaces = InterfaceType.EMPTY_ARRAY;

  /**
   * An array containing all of the methods contained in this class.
   */
  private MethodElement[] methods = MethodElementImpl.EMPTY_ARRAY;

  /**
   * The superclass of the class, or {@code null} if the class does not have an explicit superclass.
   */
  private InterfaceType supertype;

  /**
   * An array containing all of the toolkit objects attached to this class.
   */
  private ToolkitObjectElement[] toolkitObjects = ToolkitObjectElement.EMPTY_ARRAY;

  /**
   * The type defined by the class.
   */
  private InterfaceType type;

  /**
   * An array containing all of the type parameters defined for this class.
   */
  private TypeParameterElement[] typeParameters = TypeParameterElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of class elements.
   */
  public static final ClassElement[] EMPTY_ARRAY = new ClassElement[0];

  /**
   * Initialize a newly created class element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public ClassElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created class element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public ClassElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitClassElement(this);
  }

  /**
   * Set the toolkit specific information objects attached to this class.
   * 
   * @param toolkitObjects the toolkit objects attached to this class
   */
  public void addToolkitObjects(ToolkitObjectElement toolkitObject) {
    ((ToolkitObjectElementImpl) toolkitObject).setEnclosingElement(this);
    toolkitObjects = ArrayUtils.add(toolkitObjects, toolkitObject);
  }

  @Override
  public PropertyAccessorElement[] getAccessors() {
    return accessors;
  }

  @Override
  public InterfaceType[] getAllSupertypes() {
    ArrayList<InterfaceType> list = new ArrayList<InterfaceType>();
    collectAllSupertypes(list);
    return list.toArray(new InterfaceType[list.size()]);
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
    for (ConstructorElement constructor : constructors) {
      if (((ConstructorElementImpl) constructor).getIdentifier().equals(identifier)) {
        return (ConstructorElementImpl) constructor;
      }
    }
    for (FieldElement field : fields) {
      if (((FieldElementImpl) field).getIdentifier().equals(identifier)) {
        return (FieldElementImpl) field;
      }
    }
    for (MethodElement method : methods) {
      if (((MethodElementImpl) method).getIdentifier().equals(identifier)) {
        return (MethodElementImpl) method;
      }
    }
    for (TypeParameterElement typeParameter : typeParameters) {
      if (((TypeParameterElementImpl) typeParameter).getIdentifier().equals(identifier)) {
        return (TypeParameterElementImpl) typeParameter;
      }
    }
    return null;
  }

  @Override
  public ConstructorElement[] getConstructors() {
    return constructors;
  }

  @Override
  public FieldElement getField(String name) {
    for (FieldElement fieldElement : fields) {
      if (name.equals(fieldElement.getName())) {
        return fieldElement;
      }
    }
    return null;
  }

  @Override
  public FieldElement[] getFields() {
    return fields;
  }

  @Override
  public PropertyAccessorElement getGetter(String getterName) {
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isGetter() && accessor.getName().equals(getterName)) {
        return accessor;
      }
    }
    return null;
  }

  @Override
  public InterfaceType[] getInterfaces() {
    return interfaces;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.CLASS;
  }

  @Override
  public MethodElement getMethod(String methodName) {
    for (MethodElement method : methods) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    return null;
  }

  @Override
  public MethodElement[] getMethods() {
    return methods;
  }

  @Override
  public InterfaceType[] getMixins() {
    return mixins;
  }

  @Override
  public ConstructorElement getNamedConstructor(String name) {
    for (ConstructorElement element : getConstructors()) {
      String elementName = element.getName();
      if (elementName != null && elementName.equals(name)) {
        return element;
      }
    }
    return null;
  }

  @Override
  public ClassDeclaration getNode() throws AnalysisException {
    return getNodeMatching(ClassDeclaration.class);
  }

  @Override
  public PropertyAccessorElement getSetter(String setterName) {
    // TODO (jwren) revisit- should we append '=' here or require clients to include it?
    // Do we need the check for isSetter below?
    if (!StringUtilities.endsWithChar(setterName, '=')) {
      setterName += '=';
    }
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isSetter() && accessor.getName().equals(setterName)) {
        return accessor;
      }
    }
    return null;
  }

  @Override
  public InterfaceType getSupertype() {
    return supertype;
  }

  @Override
  public ToolkitObjectElement[] getToolkitObjects() {
    return toolkitObjects;
  }

  @Override
  public InterfaceType getType() {
    return type;
  }

  @Override
  public TypeParameterElement[] getTypeParameters() {
    return typeParameters;
  }

  @Override
  public ConstructorElement getUnnamedConstructor() {
    for (ConstructorElement element : getConstructors()) {
      String name = element.getDisplayName();
      if (name == null || name.isEmpty()) {
        return element;
      }
    }
    return null;
  }

  @Override
  public boolean hasNonFinalField() {
    ArrayList<ClassElement> classesToVisit = new ArrayList<ClassElement>();
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    classesToVisit.add(this);
    while (!classesToVisit.isEmpty()) {
      ClassElement currentElement = classesToVisit.remove(0);
      if (visitedClasses.add(currentElement)) {
        // check fields
        for (FieldElement field : currentElement.getFields()) {
          if (!field.isFinal() && !field.isConst() && !field.isStatic() && !field.isSynthetic()) {
            return true;
          }
        }
        // check mixins
        for (InterfaceType mixinType : currentElement.getMixins()) {
          ClassElement mixinElement = mixinType.getElement();
          classesToVisit.add(mixinElement);
        }
        // check super
        InterfaceType supertype = currentElement.getSupertype();
        if (supertype != null) {
          ClassElement superElement = supertype.getElement();
          if (superElement != null) {
            classesToVisit.add(superElement);
          }
        }
      }
    }
    // not found
    return false;
  }

  @Override
  public boolean hasReferenceToSuper() {
    return hasModifier(Modifier.REFERENCES_SUPER);
  }

  @Override
  public boolean hasStaticMember() {
    for (MethodElement method : methods) {
      if (method.isStatic()) {
        return true;
      }
    }
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isStatic()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAbstract() {
    return hasModifier(Modifier.ABSTRACT);
  }

  @Override
  public boolean isEnum() {
    return hasModifier(Modifier.ENUM);
  }

  @Override
  public boolean isOrInheritsProxy() {
    return safeIsOrInheritsProxy(this, new HashSet<ClassElement>());
  }

  @Override
  public boolean isProxy() {
    for (ElementAnnotation annotation : getMetadata()) {
      if (annotation.isProxy()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isTypedef() {
    return hasModifier(Modifier.TYPEDEF);
  }

  @Override
  public boolean isValidMixin() {
    return hasModifier(Modifier.MIXIN);
  }

  @Override
  public MethodElement lookUpConcreteMethod(String methodName, LibraryElement library) {
    return internalLookUpConcreteMethod(methodName, library, true);
  }

  @Override
  public PropertyAccessorElement lookUpGetter(String getterName, LibraryElement library) {
    return internalLookUpGetter(getterName, library, true);
  }

  @Override
  public PropertyAccessorElement lookUpInheritedConcreteGetter(String getterName,
      LibraryElement library) {
    return internalLookUpConcreteGetter(getterName, library, false);
  }

  @Override
  public MethodElement lookUpInheritedConcreteMethod(String methodName, LibraryElement library) {
    return internalLookUpConcreteMethod(methodName, library, false);
  }

  @Override
  public PropertyAccessorElement lookUpInheritedConcreteSetter(String setterName,
      LibraryElement library) {
    return internalLookUpConcreteSetter(setterName, library, false);
  }

  @Override
  public MethodElement lookUpInheritedMethod(String methodName, LibraryElement library) {
    return internalLookUpMethod(methodName, library, false);
  }

  @Override
  public MethodElement lookUpMethod(String methodName, LibraryElement library) {
    return internalLookUpMethod(methodName, library, true);
  }

  @Override
  public PropertyAccessorElement lookUpSetter(String setterName, LibraryElement library) {
    return internalLookUpSetter(setterName, library, true);
  }

  /**
   * Set whether this class is abstract to correspond to the given value.
   * 
   * @param isAbstract {@code true} if the class is abstract
   */
  public void setAbstract(boolean isAbstract) {
    setModifier(Modifier.ABSTRACT, isAbstract);
  }

  /**
   * Set the accessors contained in this class to the given accessors.
   * 
   * @param accessors the accessors contained in this class
   */
  public void setAccessors(PropertyAccessorElement[] accessors) {
    for (PropertyAccessorElement accessor : accessors) {
      ((PropertyAccessorElementImpl) accessor).setEnclosingElement(this);
    }
    this.accessors = accessors;
  }

  /**
   * Set the constructors contained in this class to the given constructors.
   * 
   * @param constructors the constructors contained in this class
   */
  public void setConstructors(ConstructorElement[] constructors) {
    for (ConstructorElement constructor : constructors) {
      ((ConstructorElementImpl) constructor).setEnclosingElement(this);
    }
    this.constructors = constructors;
  }

  /**
   * Set whether this class is defined by an enum declaration to correspond to the given value.
   * 
   * @param isEnum {@code true} if the class is defined by an enum declaration
   */
  public void setEnum(boolean isEnum) {
    setModifier(Modifier.ENUM, isEnum);
  }

  /**
   * Set the fields contained in this class to the given fields.
   * 
   * @param fields the fields contained in this class
   */
  public void setFields(FieldElement[] fields) {
    for (FieldElement field : fields) {
      ((FieldElementImpl) field).setEnclosingElement(this);
    }
    this.fields = fields;
  }

  /**
   * Set whether this class references 'super' to the given value.
   * 
   * @param isReferencedSuper {@code true} references 'super'
   */
  public void setHasReferenceToSuper(boolean isReferencedSuper) {
    setModifier(Modifier.REFERENCES_SUPER, isReferencedSuper);
  }

  /**
   * Set the interfaces that are implemented by this class to the given types.
   * 
   * @param the interfaces that are implemented by this class
   */
  public void setInterfaces(InterfaceType[] interfaces) {
    this.interfaces = interfaces;
  }

  /**
   * Set the methods contained in this class to the given methods.
   * 
   * @param methods the methods contained in this class
   */
  public void setMethods(MethodElement[] methods) {
    for (MethodElement method : methods) {
      ((MethodElementImpl) method).setEnclosingElement(this);
    }
    this.methods = methods;
  }

  /**
   * Set the mixins that are applied to the class being extended in order to derive the superclass
   * of this class to the given types.
   * 
   * @param mixins the mixins that are applied to derive the superclass of this class
   */
  public void setMixins(InterfaceType[] mixins) {
    this.mixins = mixins;
  }

  /**
   * Set the superclass of the class to the given type.
   * 
   * @param supertype the superclass of the class
   */
  public void setSupertype(InterfaceType supertype) {
    this.supertype = supertype;
  }

  /**
   * Set the type defined by the class to the given type.
   * 
   * @param type the type defined by the class
   */
  public void setType(InterfaceType type) {
    this.type = type;
  }

  /**
   * Set whether this class is defined by a typedef construct to correspond to the given value.
   * 
   * @param isTypedef {@code true} if the class is defined by a typedef construct
   */
  public void setTypedef(boolean isTypedef) {
    setModifier(Modifier.TYPEDEF, isTypedef);
  }

  /**
   * Set the type parameters defined for this class to the given type parameters.
   * 
   * @param typeParameters the type parameters defined for this class
   */
  public void setTypeParameters(TypeParameterElement[] typeParameters) {
    for (TypeParameterElement typeParameter : typeParameters) {
      ((TypeParameterElementImpl) typeParameter).setEnclosingElement(this);
    }
    this.typeParameters = typeParameters;
  }

  /**
   * Set whether this class is a valid mixin to correspond to the given value.
   * 
   * @param isValidMixin {@code true} if this class can be used as a mixin
   */
  public void setValidMixin(boolean isValidMixin) {
    setModifier(Modifier.MIXIN, isValidMixin);
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(accessors, visitor);
    safelyVisitChildren(constructors, visitor);
    safelyVisitChildren(fields, visitor);
    safelyVisitChildren(methods, visitor);
    safelyVisitChildren(toolkitObjects, visitor);
    safelyVisitChildren(typeParameters, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    String name = getDisplayName();
    if (name == null) {
      builder.append("{unnamed class}");
    } else {
      builder.append(name);
    }
    int variableCount = typeParameters.length;
    if (variableCount > 0) {
      builder.append("<");
      for (int i = 0; i < variableCount; i++) {
        if (i > 0) {
          builder.append(", ");
        }
        ((TypeParameterElementImpl) typeParameters[i]).appendTo(builder);
      }
      builder.append(">");
    }
  }

  private void collectAllSupertypes(ArrayList<InterfaceType> supertypes) {
    ArrayList<InterfaceType> typesToVisit = new ArrayList<InterfaceType>();
    ArrayList<ClassElement> visitedClasses = new ArrayList<ClassElement>();
    typesToVisit.add(this.getType());
    while (!typesToVisit.isEmpty()) {
      InterfaceType currentType = typesToVisit.remove(0);
      ClassElement currentElement = currentType.getElement();
      if (!visitedClasses.contains(currentElement)) {
        visitedClasses.add(currentElement);
        if (currentType != this.getType()) {
          supertypes.add(currentType);
        }
        InterfaceType supertype = currentType.getSuperclass();
        if (supertype != null) {
          typesToVisit.add(supertype);
        }
        for (InterfaceType type : currentElement.getInterfaces()) {
          typesToVisit.add(type);
        }
        for (InterfaceType type : currentElement.getMixins()) {
          ClassElement element = type.getElement();
          if (!visitedClasses.contains(element)) {
            supertypes.add(type);
          }
        }
      }
    }
  }

  private PropertyAccessorElement internalLookUpConcreteGetter(String getterName,
      LibraryElement library, boolean includeThisClass) {
    PropertyAccessorElement getter = internalLookUpGetter(getterName, library, includeThisClass);
    while (getter != null && getter.isAbstract()) {
      Element definingClass = getter.getEnclosingElement();
      if (!(definingClass instanceof ClassElementImpl)) {
        return null;
      }
      getter = ((ClassElementImpl) definingClass).internalLookUpGetter(getterName, library, false);
    }
    return getter;
  }

  private MethodElement internalLookUpConcreteMethod(String methodName, LibraryElement library,
      boolean includeThisClass) {
    MethodElement method = internalLookUpMethod(methodName, library, includeThisClass);
    while (method != null && method.isAbstract()) {
      ClassElement definingClass = method.getEnclosingElement();
      if (definingClass == null) {
        return null;
      }
      method = definingClass.lookUpInheritedMethod(methodName, library);
    }
    return method;
  }

  private PropertyAccessorElement internalLookUpConcreteSetter(String setterName,
      LibraryElement library, boolean includeThisClass) {
    PropertyAccessorElement setter = internalLookUpSetter(setterName, library, includeThisClass);
    while (setter != null && setter.isAbstract()) {
      Element definingClass = setter.getEnclosingElement();
      if (!(definingClass instanceof ClassElementImpl)) {
        return null;
      }
      setter = ((ClassElementImpl) definingClass).internalLookUpSetter(setterName, library, false);
    }
    return setter;
  }

  private PropertyAccessorElement internalLookUpGetter(String getterName, LibraryElement library,
      boolean includeThisClass) {
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    ClassElement currentElement = this;
    if (includeThisClass) {
      PropertyAccessorElement element = currentElement.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    while (currentElement != null && visitedClasses.add(currentElement)) {
      for (InterfaceType mixin : currentElement.getMixins()) {
        ClassElement mixinElement = mixin.getElement();
        if (mixinElement != null) {
          PropertyAccessorElement element = mixinElement.getGetter(getterName);
          if (element != null && element.isAccessibleIn(library)) {
            return element;
          }
        }
      }
      InterfaceType supertype = currentElement.getSupertype();
      if (supertype == null) {
        return null;
      }
      currentElement = supertype.getElement();
      PropertyAccessorElement element = currentElement.getGetter(getterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    return null;
  }

  private MethodElement internalLookUpMethod(String methodName, LibraryElement library,
      boolean includeThisClass) {
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    ClassElement currentElement = this;
    if (includeThisClass) {
      MethodElement element = currentElement.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    while (currentElement != null && visitedClasses.add(currentElement)) {
      for (InterfaceType mixin : currentElement.getMixins()) {
        ClassElement mixinElement = mixin.getElement();
        if (mixinElement != null) {
          MethodElement element = mixinElement.getMethod(methodName);
          if (element != null && element.isAccessibleIn(library)) {
            return element;
          }
        }
      }
      InterfaceType supertype = currentElement.getSupertype();
      if (supertype == null) {
        return null;
      }
      currentElement = supertype.getElement();
      MethodElement element = currentElement.getMethod(methodName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    return null;
  }

  private PropertyAccessorElement internalLookUpSetter(String setterName, LibraryElement library,
      boolean includeThisClass) {
    HashSet<ClassElement> visitedClasses = new HashSet<ClassElement>();
    ClassElement currentElement = this;
    if (includeThisClass) {
      PropertyAccessorElement element = currentElement.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    while (currentElement != null && visitedClasses.add(currentElement)) {
      for (InterfaceType mixin : currentElement.getMixins()) {
        ClassElement mixinElement = mixin.getElement();
        if (mixinElement != null) {
          PropertyAccessorElement element = mixinElement.getSetter(setterName);
          if (element != null && element.isAccessibleIn(library)) {
            return element;
          }
        }
      }
      InterfaceType supertype = currentElement.getSupertype();
      if (supertype == null) {
        return null;
      }
      currentElement = supertype.getElement();
      PropertyAccessorElement element = currentElement.getSetter(setterName);
      if (element != null && element.isAccessibleIn(library)) {
        return element;
      }
    }
    return null;
  }

  private boolean safeIsOrInheritsProxy(ClassElement classElt,
      HashSet<ClassElement> visitedClassElts) {
    if (visitedClassElts.contains(classElt)) {
      return false;
    }
    visitedClassElts.add(classElt);
    if (classElt.isProxy()) {
      return true;
    } else if (classElt.getSupertype() != null
        && safeIsOrInheritsProxy(classElt.getSupertype().getElement(), visitedClassElts)) {
      return true;
    }
    InterfaceType[] supertypes = classElt.getInterfaces();
    for (int i = 0; i < supertypes.length; i++) {
      if (safeIsOrInheritsProxy(supertypes[i].getElement(), visitedClassElts)) {
        return true;
      }
    }
    supertypes = classElt.getMixins();
    for (int i = 0; i < supertypes.length; i++) {
      if (safeIsOrInheritsProxy(supertypes[i].getElement(), visitedClassElts)) {
        return true;
      }
    }
    return false;
  }

}
