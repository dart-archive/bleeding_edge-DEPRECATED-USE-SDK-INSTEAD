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
package com.google.dart.tools.core.internal.refactoring.util;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.resources.IResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Methods copied from the JDT's
 * <code>org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils</code> for the
 * Cut/Copy/Paste/Delete actions.
 */
public class RefactoringUtils {

  /**
   * Returns the passed {@link DartElement} as a {@link CompilationUnit}, or returns the
   * {@link CompilationUnit} ancestor of the passed element.
   * <p>
   * If this element is not a {@link CompilationUnit}, and does not have a {@link CompilationUnit}
   * ancestor, then <code>null</code> is returned.
   */
  public static CompilationUnit getCompilationUnit(DartElement dartElement) {
    if (dartElement instanceof CompilationUnit) {
      return (CompilationUnit) dartElement;
    }
    return dartElement.getAncestor(CompilationUnit.class);
  }

  /**
   * Returns a subset of the passed {@link DartElement}[] that contains the elements which are a
   * subset of the type specified by the passed <code>Class<E></code>.
   */
  public static <E extends DartElement> List<E> getElementsOfType(DartElement[] dartElements,
      Class<E> classType) {
    List<E> result = new ArrayList<E>(dartElements.length);
    for (int i = 0; i < dartElements.length; i++) {
      if (classType.isAssignableFrom(dartElements[i].getClass())) {
        result.add((E) dartElements[i]);
      }
    }
    return result;
  }

  /**
   * Returns the resource associated with the passed {@link DartElement}.
   */
  public static IResource getResource(DartElement element) {
    if (element instanceof CompilationUnit) {
      return ((CompilationUnit) element).getPrimary().getResource();
    } else {
      return element.getResource();
    }
  }

  public static IResource[] getResources(DartElement[] elements) {
    IResource[] result = new IResource[elements.length];
    for (int i = 0; i < elements.length; i++) {
      result[i] = getResource(elements[i]);
    }
    return result;
  }

  public static IResource[] getResources(List<?> elements) {
    List<IResource> resources = new ArrayList<IResource>(elements.size());
    for (Iterator<?> iter = elements.iterator(); iter.hasNext();) {
      Object element = iter.next();
      if (element instanceof IResource) {
        resources.add((IResource) element);
      }
    }
    return resources.toArray(new IResource[resources.size()]);
  }

  public static DartElement[] getDartElements(List<?> elements) {
    List<DartElement> resources = new ArrayList<DartElement>(elements.size());
    for (Iterator<?> iter = elements.iterator(); iter.hasNext();) {
      Object element = iter.next();
      if (element instanceof DartElement) {
        resources.add((DartElement) element);
      }
    }
    return resources.toArray(new DartElement[resources.size()]);
  }

  /**
   * List<DartElement> dartElements return CompilationUnit -> List<DartElement>
   */
  public static Map<CompilationUnit, List<DartElement>> groupByCompilationUnit(
      List<DartElement> dartElements) {
    Map<CompilationUnit, List<DartElement>> result = new HashMap<CompilationUnit, List<DartElement>>();
    for (Iterator<DartElement> iter = dartElements.iterator(); iter.hasNext();) {
      DartElement element = iter.next();
      CompilationUnit cu = getCompilationUnit(element);
      if (cu != null) {
        if (!result.containsKey(cu)) {
          result.put(cu, new ArrayList<DartElement>(1));
        }
        result.get(cu).add(element);
      }
    }
    return result;
  }

  public static boolean hasAncestorOfType(DartElement element,
      Class<? extends DartElement> ancestorClass) {
    return element.getAncestor(ancestorClass) != null;
  }

  public static boolean isInsideCompilationUnit(DartElement element) {
    return !(element instanceof CompilationUnit)
        && hasAncestorOfType(element, CompilationUnit.class);
  }

}
