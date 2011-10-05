/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.core.internal.refactoring.util.RefactoringUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.ui.internal.util.Strings;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A tuple used to keep source of an element and its type.
 * <p>
 * This code was originally copied over from
 * <code>org.eclipse.jdt.internal.corext.refactoring.TypedSource</code>.
 * 
 * @see DartElement
 * @see SourceReference
 */
public class TypedSource {

  private static class SourceTuple {

    private CompilationUnit unit;

    private SourceTuple(CompilationUnit unit) {
      this.unit = unit;
    }
  }

  public static TypedSource create(String source, int type) {
    if (source == null || !canCreateForType(type)) {
      return null;
    }
    return new TypedSource(source, type);
  }

  public static Comparator<TypedSource> createTypeComparator() {
    return new Comparator<TypedSource>() {
      @Override
      public int compare(TypedSource arg0, TypedSource arg1) {
        return arg0.getType() - arg1.getType();
      }
    };
  }

  public static TypedSource[] createTypedSources(DartElement[] dartElements) throws CoreException {
    //Map<CompilationUnit, List<DartElement>>
    Map<CompilationUnit, List<DartElement>> grouped = RefactoringUtils.groupByCompilationUnit(Arrays.asList(dartElements));
    List<TypedSource> result = new ArrayList<TypedSource>(dartElements.length);
    for (Iterator<CompilationUnit> iter = grouped.keySet().iterator(); iter.hasNext();) {
      CompilationUnit cu = iter.next();
      for (Iterator<DartElement> iterator = grouped.get(cu).iterator(); iterator.hasNext();) {
        SourceTuple tuple = new SourceTuple(cu);
        TypedSource[] ts = createTypedSources(iterator.next(), tuple);
        if (ts != null) {
          result.addAll(Arrays.asList(ts));
        }
      }
    }
    return result.toArray(new TypedSource[result.size()]);
  }

  public static void sortByType(TypedSource[] typedSources) {
    Arrays.sort(typedSources, createTypeComparator());
  }

  private static boolean canCreateForType(int type) {
    return type == DartElement.FIELD || type == DartElement.TYPE || type == DartElement.METHOD
        || type == DartElement.METHOD || type == DartElement.FUNCTION_TYPE_ALIAS;
  }

  private static TypedSource[] createTypedSources(DartElement element, SourceTuple tuple)
      throws CoreException {
    if (!RefactoringUtils.isInsideCompilationUnit(element)) {
      return null;
    }
    // TODO future improvement, handle special cases here
//    if (element.getElementType() == IJavaElement.IMPORT_CONTAINER) {
//      return createTypedSourcesForImportContainer(tuple, (IImportContainer) element);
//    } else if (element.getElementType() == IJavaElement.FIELD) {
//      return new TypedSource[] {create(getFieldSource((IField) element, tuple),
//          element.getElementType())};
//    }
    return new TypedSource[] {create(getSourceOfDeclarationNode(element, tuple.unit),
        element.getElementType())};
  }

  private static String getSourceOfDeclarationNode(DartElement dartElement, CompilationUnit cu)
      throws DartModelException, CoreException {
    //Assert.isTrue(elem.getElementType() != IJavaElement.IMPORT_CONTAINER);
    if (dartElement instanceof SourceReference) {
      SourceReference reference = (SourceReference) dartElement;
      String source = reference.getSource();
      if (source != null) {
        return Strings.trimIndentation(source, cu.getDartProject(), false);
      }
    }
    return ""; //$NON-NLS-1$
  }

  private final String source;

  private final int type;

  private TypedSource(String source, int type) {
    Assert.isNotNull(source);
    Assert.isTrue(canCreateForType(type));
    this.source = source;
    this.type = type;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof TypedSource)) {
      return false;
    }

    TypedSource ts = (TypedSource) other;
    return ts.getSource().equals(getSource()) && ts.getType() == getType();
  }

  public String getSource() {
    return source;
  }

  public int getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return getSource().hashCode() ^ (97 * getType());
  }
}
