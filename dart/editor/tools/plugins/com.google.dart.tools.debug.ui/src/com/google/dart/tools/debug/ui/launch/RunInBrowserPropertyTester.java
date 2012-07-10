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
package com.google.dart.tools.debug.ui.launch;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * A {@link PropertyTester} for checking whether the resource can be launched in the browser. It is
 * used to contribute the Run in Dartium and Run as JavaScript context menus in the Files view.
 * Defines the property "canLaunchBrowser"
 */
public class RunInBrowserPropertyTester extends PropertyTester {

  public RunInBrowserPropertyTester() {

  }

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

    if ("canLaunchBrowser".equalsIgnoreCase(property)) {
      if (receiver instanceof IStructuredSelection) {
        Object o = ((IStructuredSelection) receiver).getFirstElement();
        if (o instanceof IFile) {
          if (DartCore.isHTMLLikeFileName(((IFile) o).getName())) {
            DartElement element = DartCore.create((IFile) o);
            if (element != null) {
              return true;
            }
            return false;
          }

          DartElement element = DartCore.create((IFile) o);
          if (element instanceof CompilationUnitImpl
              && ((CompilationUnitImpl) element).definesLibrary()) {
            DartLibrary library = ((CompilationUnitImpl) element).getLibrary();
            if (library instanceof DartLibraryImpl) {
              DartLibraryImpl impl = (DartLibraryImpl) library;
              if (impl.isBrowserApplication()) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

}
