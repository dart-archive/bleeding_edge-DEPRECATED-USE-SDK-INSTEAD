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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;

public class OpenTypeHierarchyUtil {

  /**
   * Converts the input to a possible input candidates
   */
  public static DartElement[] getCandidates(Object input) {
    if (!(input instanceof DartElement)) {
      return null;
    }
    try {
      DartElement elem = (DartElement) input;
      switch (elem.getElementType()) {
        case DartElement.METHOD:
        case DartElement.FIELD:
        case DartElement.TYPE:
//        case DartElement.PACKAGE_FRAGMENT_ROOT:
        case DartElement.DART_PROJECT:
          return new DartElement[] {elem};
//        case DartElement.PACKAGE_FRAGMENT:
//          if (((IPackageFragment) elem).containsJavaResources())
//            return new DartElement[]{elem};
//          break;
//        case DartElement.IMPORT_DECLARATION:
//          IImportDeclaration decl = (IImportDeclaration) elem;
//          if (decl.isOnDemand()) {
//            elem = DartModelUtil.findTypeContainer(elem.getJavaScriptProject(),
//                Signature.getQualifier(elem.getElementName()));
//          } else {
//            elem = elem.getJavaScriptProject().findType(elem.getElementName());
//          }
//          if (elem == null)
//            return null;
//          return new DartElement[]{elem};

//        case DartElement.CLASS_FILE:
//          return new DartElement[]{((IClassFile) input).getType()};
        case DartElement.COMPILATION_UNIT: {
          CompilationUnit cu = elem.getAncestor(CompilationUnit.class);
          if (cu != null) {
            Type[] types = cu.getTypes();
            if (types.length > 0) {
              return types;
            }
          }
          break;
        }
        default:
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

// TODO (pquitslund): port/implement TypeHierarchyViewPart
//
//  public static TypeHierarchyViewPart open(DartElement element,
//      IWorkbenchWindow window) {
//    DartElement[] candidates = getCandidates(element);
//    if (candidates != null) {
//      return open(candidates, window);
//    }
//    return null;
//  }
//
//  public static TypeHierarchyViewPart open(DartElement[] candidates,
//      IWorkbenchWindow window) {
//    Assert.isTrue(candidates != null && candidates.length != 0);
//
//    DartElement input = null;
//    if (candidates.length > 1) {
//      String title = DartUIMessages.OpenTypeHierarchyUtil_selectionDialog_title;
//      String message = DartUIMessages.OpenTypeHierarchyUtil_selectionDialog_message;
//      input = SelectionConverter.selectJavaElement(candidates,
//          window.getShell(), title, message);
//    } else {
//      input = candidates[0];
//    }
//    if (input == null)
//      return null;
//
//    return openInViewPart(window, input);
//  }
//
//  private static TypeHierarchyViewPart openInViewPart(IWorkbenchWindow window,
//      DartElement input) {
//    IWorkbenchPage page = window.getActivePage();
//    try {
//      TypeHierarchyViewPart result = (TypeHierarchyViewPart) page.findView(DartUI.ID_TYPE_HIERARCHY);
//      if (result != null) {
//        result.clearNeededRefresh(); // avoid refresh of old hierarchy on
//// 'becomes visible'
//      }
//      result = (TypeHierarchyViewPart) page.showView(DartUI.ID_TYPE_HIERARCHY);
//      result.setInputElement(input);
//      return result;
//    } catch (CoreException e) {
//      ExceptionHandler.handle(e, window.getShell(),
//          DartUIMessages.OpenTypeHierarchyUtil_error_open_view, e.getMessage());
//    }
//    return null;
//  }

  private OpenTypeHierarchyUtil() {
  }
}
