/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.debug.ui.internal.chromeapp.ChromeAppLaunchShortcut;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * A {@link PropertyTester} for checking whether the resource can be launched as a Chrome app.
 * Defines the property "canLaunchChromeApp".
 */
public class RunInChromeAppPropertyTester extends PropertyTester {

  public RunInChromeAppPropertyTester() {

  }

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if ("canLaunchChromeApp".equalsIgnoreCase(property)) {
      if (receiver instanceof IStructuredSelection) {
        Object o = ((IStructuredSelection) receiver).getFirstElement();

        if (o instanceof IFile) {
          IFile file = (IFile) o;

          if (ChromeAppLaunchShortcut.MANIFEST_FILE_NAME.equals(file.getName())) {
            return true;
          }
        }
      }
    }

    return false;
  }

}
