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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * Viewer sorter which sorts the Dart elements like they appear in the source.
 */
public class SourcePositionComparator extends ViewerComparator {

  /*
   * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers .Viewer,
   * java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    if (!(e1 instanceof SourceReference)) {
      return 0;
    }
    if (!(e2 instanceof SourceReference)) {
      return 0;
    }

    DartElement parent1 = ((DartElement) e1).getParent();
    if (parent1 == null || !parent1.equals(((DartElement) e2).getParent())) {
      Type t1 = getOutermostDeclaringType(e1);
      if (t1 == null) {
        return 0;
      }

      Type t2 = getOutermostDeclaringType(e2);
      if (!t1.equals(t2)) {
        if (t2 == null) {
          return 0;
        }

        CompilationUnit cu1 = ((DartElement) e1).getAncestor(CompilationUnit.class);
        if (cu1 != null) {
          if (!cu1.equals(((DartElement) e2).getAncestor(CompilationUnit.class))) {
            return 0;
          }
        }
      }
    }

    try {
      SourceRange sr1 = ((SourceReference) e1).getSourceRange();
      SourceRange sr2 = ((SourceReference) e2).getSourceRange();
      if (sr1 == null || sr2 == null) {
        return 0;
      }

      return sr1.getOffset() - sr2.getOffset();

    } catch (DartModelException e) {
      return 0;
    }
  }

  private Type getOutermostDeclaringType(Object element) {
    if (!(element instanceof TypeMember)) {
      return null;
    }

    Type declaringType;
    if (element instanceof Type) {
      declaringType = (Type) element;
    } else {
      declaringType = ((TypeMember) element).getDeclaringType();
      if (declaringType == null) {
        return null;
      }
    }
    return declaringType;
  }
}
