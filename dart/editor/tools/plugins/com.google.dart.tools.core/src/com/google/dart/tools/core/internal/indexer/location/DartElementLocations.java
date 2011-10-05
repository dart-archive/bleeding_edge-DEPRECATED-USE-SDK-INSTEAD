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
package com.google.dart.tools.core.internal.indexer.location;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;

public class DartElementLocations {
  public static DartElementLocation byDartElement(DartElement element) throws DartModelException {
    switch (element.getElementType()) {
      case DartElement.COMPILATION_UNIT:
        CompilationUnit unit = (CompilationUnit) element;
        return new CompilationUnitLocation(unit, unit.getSourceRange());
      case DartElement.FIELD:
        Field field = (Field) element;
        return new FieldLocation(field, field.getNameRange());
      case DartElement.FUNCTION:
        DartFunction function = (DartFunction) element;
        return new FunctionLocation(function, function.getNameRange());
      case DartElement.FUNCTION_TYPE_ALIAS:
        DartFunctionTypeAlias alias = (DartFunctionTypeAlias) element;
        return new FunctionTypeAliasLocation(alias, alias.getNameRange());
      case DartElement.METHOD:
        Method method = (Method) element;
        return new MethodLocation(method, method.getNameRange());
      case DartElement.TYPE:
        Type type = (Type) element;
        return new TypeLocation(type, type.getNameRange());
      default:
        return null;
    }
  }
}
