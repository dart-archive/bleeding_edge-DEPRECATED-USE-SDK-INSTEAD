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

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import java.util.ArrayList;
import java.util.Collection;

public class Implementors {
  private static IImplementorFinder[] IMPLEMENTOR_FINDERS = new IImplementorFinder[] {new DartImplementorFinder()};
  private static Implementors SINGLETON;

  /**
   * Returns the shared instance.
   */
  public static Implementors getInstance() {
    if (SINGLETON == null) {
      SINGLETON = new Implementors();
    }
    return SINGLETON;
  }

  private Implementors() {
  }

  /**
   * Searches for implementors of the specified Dart elements. Currently, only Method instances are
   * searched for. Also, only the first element of the elements parameter is taken into
   * consideration.
   * 
   * @param elements
   * @return An array of found implementing Dart elements (currently only Method instances)
   */
  public DartElement[] searchForImplementors(DartElement[] elements,
      IProgressMonitor progressMonitor) {
    if ((elements != null) && (elements.length > 0)) {
      DartElement element = elements[0];

      try {
        if (element instanceof TypeMember) {
          TypeMember member = (TypeMember) element;
          Type type = member.getDeclaringType();

          if (type.isInterface()) {
            Type[] implementingTypes = findImplementingTypes(type, progressMonitor);

            if (member.getElementType() == DartElement.METHOD) {
              return findMethods((Method) member, implementingTypes, progressMonitor);
            } else {
              return implementingTypes;
            }
          }
        }
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      }
    }

    return null;
  }

  /**
   * Searches for interfaces which are implemented by the declaring classes of the specified Dart
   * elements. Currently, only Method instances are searched for. Also, only the first element of
   * the elements parameter is taken into consideration.
   * 
   * @param elements
   * @return An array of found interfaces implemented by the declaring classes of the specified Dart
   *         elements (currently only Method instances)
   */
  public DartElement[] searchForInterfaces(DartElement[] elements, IProgressMonitor progressMonitor) {
    if ((elements != null) && (elements.length > 0)) {
      DartElement element = elements[0];

      if (element instanceof TypeMember) {
        TypeMember member = (TypeMember) element;
        Type type = member.getDeclaringType();

        Type[] implementingTypes = findInterfaces(type, progressMonitor);

        if (!progressMonitor.isCanceled()) {
          if (member.getElementType() == DartElement.METHOD) {
            return findMethods((Method) member, implementingTypes, progressMonitor);
          } else {
            return implementingTypes;
          }
        }
      }
    }

    return null;
  }

  private Type[] findImplementingTypes(Type type, IProgressMonitor progressMonitor) {
    Collection<Type> implementingTypes = new ArrayList<Type>();

    IImplementorFinder[] finders = getImplementorFinders();

    for (int i = 0; (i < finders.length) && !progressMonitor.isCanceled(); i++) {
      Collection<Type> types = finders[i].findImplementingTypes(type, new SubProgressMonitor(
          progressMonitor,
          10,
          SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

      if (types != null) {
        implementingTypes.addAll(types);
      }
    }

    return implementingTypes.toArray(new Type[implementingTypes.size()]);
  }

  private Type[] findInterfaces(Type type, IProgressMonitor progressMonitor) {
    Collection<Type> interfaces = new ArrayList<Type>();

    IImplementorFinder[] finders = getImplementorFinders();

    for (int i = 0; (i < finders.length) && !progressMonitor.isCanceled(); i++) {
      Collection<Type> types = finders[i].findInterfaces(type, new SubProgressMonitor(
          progressMonitor,
          10,
          SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

      if (types != null) {
        interfaces.addAll(types);
      }
    }

    return interfaces.toArray(new Type[interfaces.size()]);
  }

  /**
   * Finds Method instances on the specified Type instances with identical signatures as the
   * specified Method parameter.
   * 
   * @param method The method to find "equals" of.
   * @param types The types in which the search is performed.
   * @return An array of methods which match the method parameter.
   */
  private DartElement[] findMethods(Method method, Type[] types, IProgressMonitor progressMonitor) {
    Collection<Method> foundMethods = new ArrayList<Method>();

    SubProgressMonitor subProgressMonitor = new SubProgressMonitor(
        progressMonitor,
        10,
        SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
    subProgressMonitor.beginTask("", types.length); //$NON-NLS-1$

    try {
      for (int i = 0; i < types.length; i++) {
        Type type = types[i];
        Method[] methods = type.findMethods(method);

        if (methods != null) {
          for (int j = 0; j < methods.length; j++) {
            foundMethods.add(methods[j]);
          }
        }

        subProgressMonitor.worked(1);
      }
    } finally {
      subProgressMonitor.done();
    }

    return foundMethods.toArray(new DartElement[foundMethods.size()]);
  }

  private IImplementorFinder[] getImplementorFinders() {
    return IMPLEMENTOR_FINDERS;
  }
}
