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
package com.google.dart.tools.ui;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.OpenableElement;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * TODO(brianwilkerson): This is a temporary interface, used to resolve compilation errors.
 */
public class StubUtility {
  public static ImportRewrite createImportRewrite(CompilationUnit fCompilationUnit, boolean b)
      throws CoreException {
    return null;
  }

  public static ImportRewrite createImportRewrite(DartUnit cu, boolean b) throws CoreException {
    return null;
  }

  public static String getLineDelimiterPreference(IProject project) {
    IScopeContext[] scopeContext;
    if (project != null) {
      // project preference
      scopeContext = new IScopeContext[] {new ProjectScope(project)};
      String lineDelimiter = Platform.getPreferencesService().getString(
          Platform.PI_RUNTIME,
          Platform.PREF_LINE_SEPARATOR,
          null,
          scopeContext);
      if (lineDelimiter != null) {
        return lineDelimiter;
      }
    }
    // workspace preference
    scopeContext = new IScopeContext[] {InstanceScope.INSTANCE};
    String platformDefault = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    return Platform.getPreferencesService().getString(
        Platform.PI_RUNTIME,
        Platform.PREF_LINE_SEPARATOR,
        platformDefault,
        scopeContext);
  }

  /**
   * Examines a string and returns the first line delimiter found.
   * 
   * @param elem the element
   * @return the line delimiter used for the element
   */
  public static String getLineDelimiterUsed(DartElement elem) {
    OpenableElement openable = elem.getOpenable();
    if (openable instanceof CompilationUnit) {
      try {
        return openable.findRecommendedLineSeparator();
      } catch (DartModelException exception) {
        // Use project setting
      }
    }
    DartProject project = elem.getDartProject();
    return getProjectLineDelimiter(project.exists() ? project : null);
  }

  public static String getMethodBodyContent(
      boolean isConstructor,
      DartProject dartProject,
      String declaringTypeName,
      String methodName,
      String bodyStatement,
      String lineDelimiter) {
    return null;
  }

  public static String getMethodComment(
      CompilationUnit cu,
      String declaringTypeName,
      String methodName,
      String[] paramNames,
      String[] excTypeSig,
      String retTypeSig,
      boolean b,
      String lineDelimiter) {
    return null;
  }

  private static String getProjectLineDelimiter(DartProject javaProject) {
    IProject project = null;
    if (javaProject != null) {
      project = javaProject.getProject();
    }

    String lineDelimiter = getLineDelimiterPreference(project);
    if (lineDelimiter != null) {
      return lineDelimiter;
    }

    return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

}
