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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.CreateFileChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.MergeCompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.AddDependencyCorrectionProposal;
import com.google.dart.engine.services.correction.ChangeCorrectionProposal;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.CreateFileCorrectionProposal;
import com.google.dart.engine.services.correction.LinkedPositionProposal;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusContext;
import com.google.dart.engine.services.status.RefactoringStatusEntry;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext_OLD;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.correction.proposals.LinkedCorrectionProposal_OLD;
import com.google.dart.tools.ui.internal.text.correction.proposals.TrackedPositions;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Utilities to create LTK wrapper around Engine Services objects.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServiceUtils_OLD {
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
    // leaf CreateFileChange
    if (change instanceof CreateFileChange) {
      CreateFileChange fileChange = (CreateFileChange) change;
      return new com.google.dart.tools.ui.internal.text.correction.proposals.CreateFileChange(
          fileChange.getName(),
          fileChange.getFile(),
          fileChange.getContent());
    }
    // may be MergeCompositeChange
    if (change instanceof MergeCompositeChange) {
      MergeCompositeChange mergeChange = (MergeCompositeChange) change;
      return toLTK(mergeChange);
    }
    // should be CompositeChange
    CompositeChange compositeChange = (CompositeChange) change;
    return toLTK(compositeChange);
  }

  /**
   * @return the LTK change for the given Services {@link CompositeChange}.
   */
  public static org.eclipse.ltk.core.refactoring.CompositeChange toLTK(
      CompositeChange compositeChange) {
    org.eclipse.ltk.core.refactoring.CompositeChange ltkChange = new org.eclipse.ltk.core.refactoring.CompositeChange(
        compositeChange.getName());
    for (Change child : compositeChange.getChildren()) {
      ltkChange.add(toLTK(child));
    }
    return ltkChange;
  }

  /**
   * @return the Editor specific {@link Image} to given {@link CorrectionImage} identifier.
   */
  public static Image toLTK(final CorrectionImage imageId) {
    if (imageId != null) {
      return ExecutionUtils.runObjectUI(new RunnableObjectEx<Image>() {
        @Override
        public Image runObject() throws Exception {
          switch (imageId) {
            case IMG_CORRECTION_CHANGE:
              return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
            case IMG_CORRECTION_CLASS:
              return DartPluginImages.get(DartPluginImages.IMG_OBJS_CLASS);
          }
          return null;
        }
      });
    }
    return null;
  }

  /**
   * @return the LTK change for the given Services {@link CompositeChange}.
   */
  public static org.eclipse.ltk.core.refactoring.Change toLTK(MergeCompositeChange mergeChange) {
    String mergedName = mergeChange.getName();
    CompositeChange pChange = mergeChange.getPreviewChange();
    CompositeChange eChange = mergeChange.getExecuteChange();
    final org.eclipse.ltk.core.refactoring.CompositeChange previewChange = toLTK(pChange);
    final org.eclipse.ltk.core.refactoring.CompositeChange executeChange = toLTK(eChange);
    // May be no preview changes, for example because all these changes are applied to external
    // files (in the Pub cache) and we ignored them.
    if (previewChange.getChildren().length == 0) {
      return getRenamedChange(mergedName, executeChange);
    }
    // OK, create wrapper LTK CompositeChange
    return new org.eclipse.ltk.core.refactoring.CompositeChange(
        mergedName,
        new org.eclipse.ltk.core.refactoring.Change[] {previewChange, executeChange}) {
      @Override
      public org.eclipse.ltk.core.refactoring.Change perform(IProgressMonitor pm)
          throws CoreException {
        mergeTextChanges(executeChange, previewChange);
        return executeChange.perform(pm);
      }
    };
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
    if (context == null) {
      return null;
    }
    return new DartStatusContext_OLD(context.getContext(), context.getSource(), context.getRange());
  }

  /**
   * @return the LTK {@link TextFileChange} for the given services {@link SourceChange}.
   */
  public static TextFileChange toLTK(SourceChange change) {
    Source source = change.getSource();
    // prepare IFile
    IFile file = getFile(source);
    if (file == null) {
      return null;
    }
    // prepare CompilationUnitChange
    CompilationUnitChange ltkChange = new CompilationUnitChange(change.getName(), file);
    ltkChange.setEdit(new MultiTextEdit());
    Map<String, List<Edit>> editGroups = change.getEditGroups();
    for (Entry<String, List<Edit>> entry : editGroups.entrySet()) {
      List<Edit> edits = entry.getValue();
      // add edits
      TextEdit ltkEdits[] = toLTK(edits);
      try {
        for (TextEdit ltkEdit : ltkEdits) {
          ltkChange.addEdit(ltkEdit);
        }
      } catch (MalformedTreeException e) {
        throw new Error(source + " " + StringUtils.join(ltkEdits, " "), e);
      }
      // add group
      String groupName = entry.getKey();
      if (StringUtils.isNotEmpty(groupName)) {
        ltkChange.addTextEditGroup(new TextEditGroup(groupName, ltkEdits));
      }
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
   * @return the {@link IDartCompletionProposal} for the given {@link CreateFileCorrectionProposal}.
   */
  public static IDartCompletionProposal toUI(AddDependencyCorrectionProposal proposal) {
    return new com.google.dart.tools.ui.internal.text.correction.proposals.AddDependencyCorrectionProposal(
        proposal.getKind().getRelevance(),
        proposal.getName(),
        proposal.getFile(),
        proposal.getPackageName());
  }

  /**
   * @return the Eclipse {@link ICompletionProposal} for the given {@link CorrectionProposal}.
   */
  public static ICompletionProposal toUI(CorrectionProposal serviceProposal) {
    if (serviceProposal instanceof ChangeCorrectionProposal) {
      ChangeCorrectionProposal changeProposal = (ChangeCorrectionProposal) serviceProposal;
      org.eclipse.ltk.core.refactoring.Change ltkChange = toLTK(changeProposal.getChange());
      if (ltkChange == null) {
        return null;
      }
      return new com.google.dart.tools.ui.internal.text.correction.proposals.ChangeCorrectionProposal(
          changeProposal.getName(),
          ltkChange,
          changeProposal.getKind().getRelevance(),
          toLTK(changeProposal.getKind().getImage()));
    }
    if (serviceProposal instanceof CreateFileCorrectionProposal) {
      CreateFileCorrectionProposal fileProposal = (CreateFileCorrectionProposal) serviceProposal;
      return toUI(fileProposal);
    }
    if (serviceProposal instanceof AddDependencyCorrectionProposal) {
      AddDependencyCorrectionProposal proposal = (AddDependencyCorrectionProposal) serviceProposal;
      return toUI(proposal);
    }
    if (serviceProposal instanceof SourceCorrectionProposal) {
      SourceCorrectionProposal sourceProposal = (SourceCorrectionProposal) serviceProposal;
      return toUI(sourceProposal);
    }
    return null;
  }

  /**
   * @return the {@link IDartCompletionProposal} for the given {@link CreateFileCorrectionProposal}.
   */
  public static IDartCompletionProposal toUI(CreateFileCorrectionProposal fileProposal) {
    return new com.google.dart.tools.ui.internal.text.correction.proposals.CreateFileCorrectionProposal(
        fileProposal.getKind().getRelevance(),
        fileProposal.getName(),
        fileProposal.getFile(),
        fileProposal.getContent());
  }

  /**
   * @return the {@link LinkedCorrectionProposal_OLD} for the given {@link SourceCorrectionProposal}.
   */
  public static LinkedCorrectionProposal_OLD toUI(SourceCorrectionProposal sourceProposal) {
    // prepare TextChange
    SourceChange sourceChange = sourceProposal.getChange();
    TextChange textChange = ServiceUtils_OLD.toLTK(sourceChange);
    if (textChange == null) {
      return null;
    }
    // prepare UI proposal
    CorrectionKind kind = sourceProposal.getKind();
    Image image = ServiceUtils_OLD.toLTK(kind.getImage());
    LinkedCorrectionProposal_OLD uiProposal = new LinkedCorrectionProposal_OLD(
        sourceProposal.getName(),
        sourceChange.getSource(),
        textChange,
        kind.getRelevance(),
        image);
    // add linked positions
    for (Entry<String, List<SourceRange>> entry : sourceProposal.getLinkedPositions().entrySet()) {
      String group = entry.getKey();
      for (SourceRange position : entry.getValue()) {
        uiProposal.addLinkedPosition(TrackedPositions.forRange(position), false, group);
      }
    }
    // add proposals
    for (Entry<String, List<LinkedPositionProposal>> entry : sourceProposal.getLinkedPositionProposals().entrySet()) {
      String group = entry.getKey();
      for (LinkedPositionProposal proposal : entry.getValue()) {
        uiProposal.addLinkedPositionProposal(group, proposal.getText(), toLTK(proposal.getIcon()));
      }
    }
    // set end position
    {
      SourceRange endRange = sourceProposal.getEndRange();
      if (endRange != null) {
        uiProposal.setEndPosition(TrackedPositions.forRange(endRange));
      }
    }
    // done
    return uiProposal;
  }

  /**
   * @return the error {@link IStatus} for the given {@link Throwable}.
   */
  private static IStatus createRuntimeStatus(Throwable e) {
    return new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), e.getMessage(), e);
  }

  /**
   * @return the {@link IFile} of the given {@link Source}, may be {@code null} if external.
   */
  private static IFile getFile(Source source) {
    return (IFile) DartCore.getProjectManager().getResource(source);
  }

  private static org.eclipse.ltk.core.refactoring.CompositeChange getRenamedChange(String newName,
      final org.eclipse.ltk.core.refactoring.CompositeChange executeChange) {
    org.eclipse.ltk.core.refactoring.CompositeChange renamedExecuteChange;
    renamedExecuteChange = new org.eclipse.ltk.core.refactoring.CompositeChange(newName);
    renamedExecuteChange.merge(executeChange);
    return renamedExecuteChange;
  }

  /**
   * Merges {@link TextChange}s from "newCompositeChange" into "existingCompositeChange".
   */
  private static void mergeTextChanges(
      org.eclipse.ltk.core.refactoring.CompositeChange existingCompositeChange,
      org.eclipse.ltk.core.refactoring.CompositeChange newCompositeChange) {
    // [element -> Change map] in CompositeChange
    Map<Object, org.eclipse.ltk.core.refactoring.Change> elementChanges = Maps.newHashMap();
    for (org.eclipse.ltk.core.refactoring.Change change : existingCompositeChange.getChildren()) {
      Object modifiedElement = change.getModifiedElement();
      elementChanges.put(modifiedElement, change);
    }
    // merge new changes into CompositeChange
    for (org.eclipse.ltk.core.refactoring.Change newChange : newCompositeChange.getChildren()) {
      // ignore if disabled (in preview UI)
      if (!newChange.isEnabled()) {
        continue;
      }
      // prepare existing TextChange
      Object modifiedElement = newChange.getModifiedElement();
      org.eclipse.ltk.core.refactoring.Change existingChange = elementChanges.get(modifiedElement);
      // add TextEditChangeGroup from new TextChange
      if (existingChange instanceof TextChange && newChange instanceof TextChange) {
        TextChange existingTextChange = (TextChange) existingChange;
        TextEdit existingTextEdit = existingTextChange.getEdit();
        TextChange newTextChange = (TextChange) newChange;
        // remember TextEdit(s) disabled in preview UI
        Set<TextEdit> disabledTextEdits = Sets.newHashSet();
        for (TextEditChangeGroup group : newTextChange.getTextEditChangeGroups()) {
          if (!group.isEnabled()) {
            Collections.addAll(disabledTextEdits, group.getTextEdits());
          }
        }
        // merge not disabled TextEdit(s)
        TextEdit newTextEdit = newTextChange.getEdit();
        if (newTextEdit != null) {
          for (TextEdit childTextEdit : newTextEdit.getChildren()) {
            if (disabledTextEdits.contains(childTextEdit)) {
              continue;
            }
            childTextEdit.getParent().removeChild(childTextEdit);
            TextChangeCompatibility.insert(existingTextEdit, childTextEdit);
          }
        }
      } else {
        newCompositeChange.remove(newChange);
        existingCompositeChange.add(newChange);
      }
    }
  }

  private static TextEdit toLTK(Edit edit) {
    return new ReplaceEdit(edit.getOffset(), edit.getLength(), edit.getReplacement());
  }

  private static TextEdit[] toLTK(List<Edit> edits) {
    // NB(scheglov) It is a bad idea to ensure uniqueness of Edit(s) here.
    List<TextEdit> ltkEdits = Lists.newArrayList();
    for (Edit edit : edits) {
      TextEdit ltkEdit = toLTK(edit);
      ltkEdits.add(ltkEdit);
    }
    return ltkEdits.toArray(new TextEdit[ltkEdits.size()]);
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
