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

package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.status.RefactoringStatusEntry;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import java.util.List;

/**
 * Utilities to create LTK wrapper around Engine Services objects.
 */
public class ServiceUtils {
  /**
   * @return the {@link CoreException} wrapper around given {@link Throwable}.
   */
  public static CoreException createCoreException(Throwable e) {
    IStatus status = createRuntimeStatus(e);
    return new CoreException(status);
  }

  /**
   * @return the LTK change for the given Services {@link Change}.
   */
  public static org.eclipse.ltk.core.refactoring.Change toLTK(Change change) {
    // leaf SourceChange
    if (change instanceof SourceChange) {
      SourceChange sourceChange = (SourceChange) change;
      return toLTK(sourceChange);
    }
    // should be CompositeChange
    CompositeChange compositeChange = (CompositeChange) change;
    org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = new org.eclipse.ltk.core.refactoring.CompositeChange(
        compositeChange.getName());
    for (Change child : compositeChange.getChildren()) {
      ltkChange.add(toLTK(child));
    }
    // TODO(scheglov) edit groups
    return ltkChange;
  }

  /**
   * @return the Editor specific {@link Image} to given {@link CorrectionImage} identifier.
   */
  public static Image toLTK(CorrectionImage imageId) {
    switch (imageId) {
      case IMG_CORRECTION_CHANGE:
        return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
    }
    return null;
  }

  /**
   * @return the LTK status for the given Services {@link RefactoringStatus}.
   */
  public static org.eclipse.ltk.core.refactoring.RefactoringStatus toLTK(RefactoringStatus status) {
    org.eclipse.ltk.core.refactoring.RefactoringStatus result = new org.eclipse.ltk.core.refactoring.RefactoringStatus();
    for (RefactoringStatusEntry entry : status.getEntries()) {
      result.addEntry(
          toLTK(entry.getSeverity()),
          entry.getMessage(),
          toLTK(entry.getContext()),
          null,
          org.eclipse.ltk.core.refactoring.RefactoringStatusEntry.NO_CODE);
    }
    return result;
  }

  /**
   * @return the Dart status context for the given Services {@link RefactoringStatusContext}.
   */
  public static org.eclipse.ltk.core.refactoring.RefactoringStatusContext toLTK(
      RefactoringStatusContext context) {
    // TODO(scheglov) not implemented yet
    return null;
  }

  /**
   * @return the LTK {@link TextFileChange} for the given services {@link SourceChange}.
   */
  public static TextFileChange toLTK(SourceChange change) {
    Source source = change.getSource();
    IFile file = (IFile) DartCore.getProjectManager().getResource(source);
    TextFileChange ltkChange = new TextFileChange(source.getShortName(), file);
    ltkChange.setEdit(new MultiTextEdit());
    List<Edit> edits = change.getEdits();
    for (Edit edit : edits) {
      ltkChange.addEdit(new ReplaceEdit(edit.offset, edit.length, edit.replacement));
    }
    return ltkChange;
  }

  /**
   * @return the error status for given {@link Throwable}.
   */
  public static org.eclipse.ltk.core.refactoring.RefactoringStatus toLTK(Throwable e) {
    IStatus status = createRuntimeStatus(e);
    return org.eclipse.ltk.core.refactoring.RefactoringStatus.create(status);
  }

  /**
   * @return the error {@link IStatus} for the given {@link Throwable}.
   */
  private static IStatus createRuntimeStatus(Throwable e) {
    return new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), e.getMessage(), e);
  }

  /**
   * @return the LTK status severity for the given Service {@link RefactoringStatusSeverity}.
   */
  private static int toLTK(RefactoringStatusSeverity severity) {
    switch (severity) {
      case OK:
        return org.eclipse.ltk.core.refactoring.RefactoringStatus.OK;
      case INFO:
        return org.eclipse.ltk.core.refactoring.RefactoringStatus.INFO;
      case WARNING:
        return org.eclipse.ltk.core.refactoring.RefactoringStatus.WARNING;
      case ERROR:
        return org.eclipse.ltk.core.refactoring.RefactoringStatus.ERROR;
      case FATAL:
        return org.eclipse.ltk.core.refactoring.RefactoringStatus.FATAL;
      default:
        throw new IllegalArgumentException("severity: " + severity);
    }
  }
}
