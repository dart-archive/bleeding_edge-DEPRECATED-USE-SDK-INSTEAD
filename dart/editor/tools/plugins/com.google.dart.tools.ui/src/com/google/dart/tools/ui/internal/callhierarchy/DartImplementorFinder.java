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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeHierarchy;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class DartImplementorFinder implements IImplementorFinder {

  @Override
  public Collection<Type> findImplementingTypes(Type type, IProgressMonitor progressMonitor) {
    TypeHierarchy typeHierarchy;

//    try {
//      typeHierarchy = type.newTypeHierarchy(progressMonitor);
//
//      Type[] implementingTypes = typeHierarchy.getAllClasses();
//      HashSet<Type> result = new HashSet<Type>(Arrays.asList(implementingTypes));
//
//      return result;
//    } catch (DartModelException e) {
//      DartToolsPlugin.log(e);
//    }

    return null;
  }

  @Override
  public Collection<Type> findInterfaces(Type type, IProgressMonitor progressMonitor) {
    TypeHierarchy typeHierarchy;

    try {
      typeHierarchy = type.newSupertypeHierarchy(progressMonitor);

      Type[] interfaces = typeHierarchy.getAllSuperInterfaces(type);
      HashSet<Type> result = new HashSet<Type>(Arrays.asList(interfaces));

      return result;
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }

    return null;
  }
}
