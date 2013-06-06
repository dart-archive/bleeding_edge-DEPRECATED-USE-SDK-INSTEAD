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
package com.google.dart.tools.core.utilities.compiler;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The class <code>DartCompilerUtilities</code> defines utility methods for parsing, resolving, and
 * compiling Dart source, including compilation units, libraries, and applications.
 * 
 * @deprecated
 */
@Deprecated
public class DartCompilerUtilities {

  public static DartUnit parseSource(String elementName, String source, boolean b,
      ArrayList<DartCompilationError> newArrayList) {
    // TODO remove
    return null;
  }

  public static DartUnit parseSource(String fileName, String contents, Object object) {
    // TODO remove
    return null;
  }

  public static DartUnit parseUnit(CompilationUnit typeRoot) throws DartModelException {
    // TODO remove
    return null;
  }

  public static DartUnit parseUnit(CompilationUnit cu, Object object) throws DartModelException {
    // TODO remove
    return null;
  }

  public static DartUnit resolveUnit(CompilationUnit typeRoot) throws DartModelException {
    // TODO remove
    return null;
  }

  public static DartUnit resolveUnit(CompilationUnit je,
      Collection<DartCompilationError> parseErrors) throws DartModelException {
    // TODO remove
    return null;
  }
}
