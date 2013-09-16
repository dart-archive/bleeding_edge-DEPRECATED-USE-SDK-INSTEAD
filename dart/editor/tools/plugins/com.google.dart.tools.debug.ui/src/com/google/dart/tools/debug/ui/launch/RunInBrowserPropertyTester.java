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

import com.google.dart.engine.source.SourceKind;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.html.DartHtmlScriptHelper;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.io.IOException;
import java.util.List;

/**
 * A {@link PropertyTester} for checking whether the resource can be launched in a non Dartium
 * browser. It is used to contribute the Run as JavaScript and Run Polymer App as JavaScript context
 * menus in the Files view. Defines the property "canLaunchBrowser" and "canDeployPolymer".
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
          IFile file = (IFile) o;
          if (DartCore.isHtmlLikeFileName(((IFile) o).getName()) && !usesBootJs(file)) {
            return true;
          }

          ProjectManager manager = DartCore.getProjectManager();
          if (manager.getSourceKind(file) == SourceKind.LIBRARY
              && manager.isClientLibrary(manager.getSource(file))) {
            return true;
          }
        }
      }
    }

    if ("canDeployPolymer".equalsIgnoreCase(property)) {
      if (receiver instanceof IStructuredSelection) {
        Object o = ((IStructuredSelection) receiver).getFirstElement();
        if (o instanceof IFile) {
          IFile file = (IFile) o;
          if (DartCore.isHtmlLikeFileName(((IFile) o).getName())) {
            if (usesBootJs(file)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private boolean usesBootJs(IFile file) {
    try {
      String contents = FileUtilities.getContents(file.getLocation().toFile(), "UTF-8");
      if (contents != null) {
        List<String> list = DartHtmlScriptHelper.getNonDartScripts(contents);
        if (!list.isEmpty()) {
          for (String string : list) {
            if (string.contains("polymer/boot.js")) {
              return true;
            }
          }
        }
      }
    } catch (IOException e) {
      DartDebugUIPlugin.logError(e);
    }
    return false;
  }

}
