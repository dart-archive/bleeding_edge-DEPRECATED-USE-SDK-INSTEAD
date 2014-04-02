/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;

public class TypeProviderImplTest extends EngineTestCase {
  public void test_creation() {
    //
    // Create a mock library element with the types expected to be in dart:core. We cannot use
    // either ElementFactory or TestTypeProvider (which uses ElementFactory) because we side-effect
    // the elements in ways that would break other tests.
    //
    InterfaceType objectType = classElement("Object", null).getType();
    InterfaceType boolType = classElement("bool", objectType).getType();
    InterfaceType numType = classElement("num", objectType).getType();
    InterfaceType doubleType = classElement("double", numType).getType();
    InterfaceType functionType = classElement("Function", objectType).getType();
    InterfaceType intType = classElement("int", numType).getType();
    InterfaceType listType = classElement("List", objectType, "E").getType();
    InterfaceType mapType = classElement("Map", objectType, "K", "V").getType();
    InterfaceType stackTraceType = classElement("StackTrace", objectType).getType();
    InterfaceType stringType = classElement("String", objectType).getType();
    InterfaceType symbolType = classElement("Symbol", objectType).getType();
    InterfaceType typeType = classElement("Type", objectType).getType();
    CompilationUnitElementImpl unit = new CompilationUnitElementImpl("lib.dart");
    unit.setTypes(new ClassElement[] {
        boolType.getElement(), doubleType.getElement(), functionType.getElement(),
        intType.getElement(), listType.getElement(), mapType.getElement(), objectType.getElement(),
        stackTraceType.getElement(), stringType.getElement(), symbolType.getElement(),
        typeType.getElement(),});
    LibraryElementImpl library = new LibraryElementImpl(
        new AnalysisContextImpl(),
        libraryIdentifier("lib"));
    library.setDefiningCompilationUnit(unit);
    //
    // Create a type provider and ensure that it can return the expected types.
    //
    TypeProviderImpl provider = new TypeProviderImpl(library);
    assertSame(boolType, provider.getBoolType());
    assertNotNull(provider.getBottomType());
    assertSame(doubleType, provider.getDoubleType());
    assertNotNull(provider.getDynamicType());
    assertSame(functionType, provider.getFunctionType());
    assertSame(intType, provider.getIntType());
    assertSame(listType, provider.getListType());
    assertSame(mapType, provider.getMapType());
    assertSame(objectType, provider.getObjectType());
    assertSame(stackTraceType, provider.getStackTraceType());
    assertSame(stringType, provider.getStringType());
    assertSame(symbolType, provider.getSymbolType());
    assertSame(typeType, provider.getTypeType());
  }

  private ClassElement classElement(String typeName, InterfaceType superclassType,
      String... parameterNames) {
    ClassElementImpl element = new ClassElementImpl(identifier(typeName));
    element.setSupertype(superclassType);
    InterfaceTypeImpl type = new InterfaceTypeImpl(element);
    element.setType(type);

    int count = parameterNames.length;
    if (count > 0) {
      TypeParameterElementImpl[] typeParameters = new TypeParameterElementImpl[count];
      TypeParameterTypeImpl[] typeArguments = new TypeParameterTypeImpl[count];
      for (int i = 0; i < count; i++) {
        TypeParameterElementImpl typeParameter = new TypeParameterElementImpl(
            identifier(parameterNames[i]));
        typeParameters[i] = typeParameter;
        typeArguments[i] = new TypeParameterTypeImpl(typeParameter);
        typeParameter.setType(typeArguments[i]);
      }
      element.setTypeParameters(typeParameters);
      type.setTypeArguments(typeArguments);
    }

    return element;
  }
}
