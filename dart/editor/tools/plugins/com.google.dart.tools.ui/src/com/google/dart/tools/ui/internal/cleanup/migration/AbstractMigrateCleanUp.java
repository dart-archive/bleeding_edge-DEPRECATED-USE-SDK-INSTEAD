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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.fix.CompilationUnitFix;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.ui.cleanup.CleanUpContext;
import com.google.dart.tools.ui.cleanup.CleanUpRequirements;
import com.google.dart.tools.ui.cleanup.ICleanUpFix;
import com.google.dart.tools.ui.internal.cleanup.AbstractCleanUp;
import com.google.dart.tools.ui.internal.util.DartModelUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * In specification 1.0 M1 getter should not have parameters.
 */
public abstract class AbstractMigrateCleanUp extends AbstractCleanUp {
  protected CompilationUnit unit;
  protected DartUnit unitNode;
  protected ExtractUtils utils;
  protected CompilationUnitChange change;
  private MultiTextEdit rootEdit;

  @Override
  public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
    try {
      initialize(context);
      if (change == null) {
        return null;
      }
      createFix();
      // ignore if empty
      if (!rootEdit.hasChildren()) {
        return null;
      }
      // OK, we have something to fix
      return new CompilationUnitFix(change);
    } finally {
      reset();
    }
  }

  @Override
  public CleanUpRequirements getRequirements() {
    return new CleanUpRequirements(true, false, false, null);
  }

  protected final void addReplaceEdit(SourceRange range, String text) {
    change.addEdit(new ReplaceEdit(range.getOffset(), range.getLength(), text));
  }

  /**
   * Adds {@link TextEdit} into {@link #change}.
   */
  protected abstract void createFix();

  /**
   * Initializes values of fields.
   */
  private void initialize(CleanUpContext context) throws DartModelException {
    reset();
    // store unit
    unit = context.getCompilationUnit();
    if (DartModelUtil.isExternal(unit)) {
      return;
    }
    // store AST
    unitNode = context.getAST();
    utils = new ExtractUtils(unit, unitNode);
    // create CompilationUnitChange
    change = new CompilationUnitChange(unit.getElementName(), unit);
    rootEdit = new MultiTextEdit();
    change.setEdit(rootEdit);
  }

  /**
   * Clears values of fields.
   */
  private void reset() {
    unit = null;
    unitNode = null;
    utils = null;
    change = null;
    rootEdit = null;
  }

}
