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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartX;

import org.eclipse.core.resources.IStorage;

/**
 * Utility methods for the Dart Model.
 */
public final class DartModelUtil {

  /**
   * Concatenates two names. Uses a dot for separation. Both strings can be empty or
   * <code>null</code>.
   */
  public static String concatenateName(String name1, String name2) {
    StringBuffer buf = new StringBuffer();
    if (name1 != null && name1.length() > 0) {
      buf.append(name1);
    }
    if (name2 != null && name2.length() > 0) {
      if (buf.length() > 0) {
        buf.append('.');
      }
      buf.append(name2);
    }
    return buf.toString();
  }

  /**
   * Returns the qualified type name of the given type using '.' as separators. This is a replace
   * for Type.getTypeQualifiedName() which uses '$' as separators. As '$' is also a valid character
   * in an id this is ambiguous. JavaScriptCore PR: 1GCFUNT
   */
  @SuppressWarnings("deprecation")
  public static String getTypeQualifiedName(Type type) {
    return type.getTypeQualifiedName('.');
  }

  public static boolean isImplicitImport(String qualifier, CompilationUnit cu) {
    if ("java.lang".equals(qualifier)) { //$NON-NLS-1$
      return true;
    }
    String packageName = cu.getParent().getElementName();
    if (qualifier.equals(packageName)) {
      return true;
    }
    String typeName = DartCore.removeDartLikeExtension(cu.getElementName());
    String mainTypeName = DartModelUtil.concatenateName(packageName, typeName);
    return qualifier.equals(mainTypeName);
  }

  public static boolean isOpenableStorage(Object storage) {
//    if (storage instanceof IJarEntryResource) {
//      return ((IJarEntryResource) storage).isFile();
//    } else {
    return storage instanceof IStorage;
//    }
  }

  /**
   * Returns true if a cu is a primary cu (original or shared working copy)
   */
  public static boolean isPrimary(CompilationUnit cu) {
    return cu.getPrimary() == cu;
  }

  /**
   * Force a reconcile of a compilation unit.
   * 
   * @param unit
   */
  public static void reconcile(CompilationUnit unit) throws DartModelException {
    DartX.todo();
//    ((CompilationUnitImpl) unit).reconcile(false, null);
//    unit.reconcile(CompilationUnit.NO_AST, false /*
//                                                  * don't force problem
//                                                  * detection
//                                                  */,
//        null /* use primary owner */, null /* no progress monitor */);
  }
}
