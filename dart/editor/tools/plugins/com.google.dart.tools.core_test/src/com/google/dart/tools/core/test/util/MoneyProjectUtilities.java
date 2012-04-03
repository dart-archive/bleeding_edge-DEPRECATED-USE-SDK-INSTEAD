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
package com.google.dart.tools.core.test.util;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;

/**
 * Utility methods for loading and accessing the "Money" dart project
 */
public class MoneyProjectUtilities {

  public static CompilationUnit getMoneyCompilationUnit(String unitName) throws Exception {
    DartLibrary library = getMoneyLibrary();
    if (library == null) {
      return null;
    }
    CompilationUnit[] units = library.getCompilationUnits();
    for (CompilationUnit unit : units) {
      if (unit.getElementName().equals(unitName)) {
        return unit;
      }
    }
    return null;
  }

  public static DartLibrary getMoneyLibrary() throws Exception {
    DartProject project = getMoneyProject();
    if (project == null) {
      return null;
    }
    for (DartLibrary library : project.getDartLibraries()) {
      return library;
    }
    return null;
  }

  public static DartProject getMoneyProject() throws Exception {
    return TestUtilities.loadPluginRelativeProject("Money");
  }

}
