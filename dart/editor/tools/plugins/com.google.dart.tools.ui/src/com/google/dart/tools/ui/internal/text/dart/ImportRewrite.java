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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.ContextSensitiveImportRewriteContext;
import com.google.dart.tools.ui.DartX;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Add imports to a library or application.
 */
public class ImportRewrite {

  private CompilationUnit compUnit;
  private boolean restoreExistingImports;
  List<String> imports = new ArrayList<String>();

  public ImportRewrite(CompilationUnit compUnit, boolean restoreExistingImports) {
    DartX.todo();
    this.compUnit = compUnit;
    this.restoreExistingImports = restoreExistingImports;
  }

  public String addImport(String importString) {
    imports.add(importString);
    return importString;
  }

  public String addImport(String importString, ContextSensitiveImportRewriteContext context) {
    // TODO(messick) proper implementation
    return addImport(importString);
  }

  public CompilationUnit getCompilationUnit() {
    return compUnit;
  }

  public boolean hasRecordedChanges() {
    // TODO(messick) proper implementation
    return false;
  }

  public TextEdit rewriteImports(IProgressMonitor monitor) {
    if (monitor != null) {
      if (restoreExistingImports) {
        monitor.worked(0);
      }
    }
    return new InsertEdit(0, "");
  }
}
