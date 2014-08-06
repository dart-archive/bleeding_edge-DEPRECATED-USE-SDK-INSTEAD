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
import com.google.dart.tools.mock.ui.ContextSensitiveImportRewriteContext;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.List;

/**
 * Add imports to a library or application.
 */
public class ImportRewrite {
  public static final String[] NO_STRINGS = new String[0];
  private CompilationUnit compUnit;
  private boolean restoreExistingImports;

  List<String> imports = new ArrayList<String>();
  private List<String> addedImports;

  private List<String> removedImports;
  @SuppressWarnings("unused")
  private String[] createdImports;
//  private String[] importOrder;
  private boolean useContextToFilterImplicitImports;

  private Object filterImplicitImports;

  public ImportRewrite(CompilationUnit compUnit, boolean restoreExistingImports) {
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
    return !this.restoreExistingImports
        || (this.addedImports != null && !this.addedImports.isEmpty())
        || (this.removedImports != null && !this.removedImports.isEmpty());
  }

  /**
   * Converts all modifications recorded by this rewriter into an object representing the
   * corresponding text edits to the source code of the rewrite's compilation unit. The compilation
   * unit itself is not modified.
   */
  public TextEdit rewriteImports(IProgressMonitor monitor) {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    try {
      monitor.beginTask(DartTextMessages.ImportRewrite_processDescription, 2);
      if (!hasRecordedChanges()) {
        this.createdImports = NO_STRINGS;
        return new MultiTextEdit();
      }

      ImportRewriteAnalyzer computer = new ImportRewriteAnalyzer(
          this.compUnit,
          this.restoreExistingImports,
          this.useContextToFilterImplicitImports);
      computer.setFilterImplicitImports(this.filterImplicitImports);

      if (this.addedImports != null) {
        for (int i = 0; i < this.addedImports.size(); i++) {
          String curr = this.addedImports.get(i);
          computer.addImport(curr.substring(1));
        }
      }

      if (this.removedImports != null) {
        for (int i = 0; i < this.removedImports.size(); i++) {
          String curr = this.removedImports.get(i);
          computer.removeImport(curr.substring(1));
        }
      }

      TextEdit result = computer.getResultingEdits(new SubProgressMonitor(monitor, 1));
      this.createdImports = computer.getCreatedImports();

      return result;
    } finally {
      monitor.done();
    }
  }
}
