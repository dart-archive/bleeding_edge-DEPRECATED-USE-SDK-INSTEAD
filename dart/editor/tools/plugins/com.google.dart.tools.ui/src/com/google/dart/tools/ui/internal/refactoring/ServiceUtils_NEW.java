/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.server.generated.types.LinkedEditGroup;
import com.google.dart.server.generated.types.LinkedEditSuggestion;
import com.google.dart.server.generated.types.LinkedEditSuggestionKind;
import com.google.dart.server.generated.types.Location;
import com.google.dart.server.generated.types.Position;
import com.google.dart.server.generated.types.RefactoringProblem;
import com.google.dart.server.generated.types.RefactoringProblemSeverity;
import com.google.dart.server.generated.types.SourceChange;
import com.google.dart.server.generated.types.SourceEdit;
import com.google.dart.server.generated.types.SourceFileEdit;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.refactoring.base.DartStatusContext_NEW;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.correction.proposals.ChangeCorrectionProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.CreateFileChange;
import com.google.dart.tools.ui.internal.text.correction.proposals.LinkedCorrectionProposal_NEW;
import com.google.dart.tools.ui.internal.text.correction.proposals.TrackedPositions;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.util.List;

/**
 * Utilities to create LTK wrapper around Engine Services objects.
 * 
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ServiceUtils_NEW {
  /**
   * @return the {@link CoreException} wrapper around given {@link Throwable}.
   */
  public static CoreException createCoreException(Throwable e) {
    IStatus status = createRuntimeStatus(e);
    return new CoreException(status);
  }

  /**
   * Simplifies the given {@link Change}. If it is a {@link CompositeChange}, and it is zero
   * children - returns {@code null}; if it has one child - attempts to simplify it; otherwise
   * returns the current {@link Change}.
   */
  public static Change expandSingleChildCompositeChanges(Change change) {
    while (change instanceof CompositeChange) {
      CompositeChange compositeChange = (CompositeChange) change;
      Change[] children = compositeChange.getChildren();
      if (children.length == 0) {
        return null;
      } else if (children.length == 1) {
        change = children[0];
        compositeChange.remove(change);
      } else {
        break;
      }
    }
    return change;
  }

  /**
   * @return the LTK change for the given Services {@link CompositeChange}.
   */
  public static CompositeChange toLTK(SourceChange sourceChange) {
    if (sourceChange == null) {
      return null;
    }
    CompositeChange ltkChange = new CompositeChange(sourceChange.getMessage());
    for (SourceFileEdit fileEdit : sourceChange.getEdits()) {
      Change textChange = toLTK(fileEdit);
      ltkChange.add(textChange);
    }
    return ltkChange;
  }

  /**
   * @return the LTK {@link Change} for the given {@link SourceFileEdit}.
   */
  public static Change toLTK(SourceFileEdit change) {
    if (change.getFileStamp() == -1) {
      String filePath = change.getFile();
      File fileJava = new File(filePath);
      return new CreateFileChange(filePath, fileJava, change.getEdits().get(0).getReplacement());
    }
    // prepare IFile
    IFile file = getFile(change);
    if (file == null) {
      return null;
    }
    // prepare CompilationUnitChange
    String name = file.getName();
    CompilationUnitChange ltkChange = new CompilationUnitChange(name, file);
    ltkChange.setEdit(new MultiTextEdit());
    // add edits
    List<SourceEdit> sourceEdits = change.getEdits();
    TextEdit ltkEdits[] = toLTK(sourceEdits);
    try {
      for (TextEdit ltkEdit : ltkEdits) {
        ltkChange.addEdit(ltkEdit);
      }
    } catch (MalformedTreeException e) {
      throw new Error(name + " " + StringUtils.join(ltkEdits, " "), e);
    }
    // done
    return ltkChange;
  }

  /**
   * @return the error status for given {@link Throwable}.
   */
  public static RefactoringStatus toLTK(Throwable e) {
    IStatus status = createRuntimeStatus(e);
    return RefactoringStatus.create(status);
  }

  /**
   * @return the LTK status for the given {@link RefactoringProblem}s.
   */
  public static RefactoringStatus toRefactoringStatus(List<RefactoringProblem> problems) {
    RefactoringStatus result = new RefactoringStatus();
    for (RefactoringProblem problem : problems) {
      result.addEntry(
          toProblemSeverity(problem.getSeverity()),
          problem.getMessage(),
          toRefactoringContext(problem.getLocation()),
          null,
          RefactoringStatusEntry.NO_CODE);
    }
    return result;
  }

  /**
   * @return the {@link LinkedCorrectionProposal_NEW} for the given {@link SourceChange}.
   */
  public static ChangeCorrectionProposal toUI(SourceChange sourceChange) {
    List<SourceFileEdit> fileEdits = sourceChange.getEdits();
    if (fileEdits.size() != 1) {
      return null;
    }
    SourceFileEdit fileEdit = fileEdits.get(0);
    // prepare presentation
    Image image = DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE);
    int relevance = 50;
    // prepare TextChange
    TextChange textChange;
    Change change = toLTK(fileEdit);
    if (change instanceof TextChange) {
      textChange = (TextChange) change;
    } else if (change != null) {
      return new ChangeCorrectionProposal(sourceChange.getMessage(), change, relevance, image);
    } else {
      return null;
    }
    // prepare UI proposal
    // TODO(scheglov) expose "image" and "relevance" through the server API
    LinkedCorrectionProposal_NEW uiProposal = new LinkedCorrectionProposal_NEW(
        sourceChange.getMessage(),
        fileEdit.getFile(),
        textChange,
        relevance,
        image);
    // add linked positions
    List<LinkedEditGroup> linkedGroups = sourceChange.getLinkedEditGroups();
    for (int i = 0; i < linkedGroups.size(); i++) {
      String groupId = "group_" + i;
      LinkedEditGroup linkedGroup = linkedGroups.get(i);
      int length = linkedGroup.getLength();
      // add positions
      for (Position position : linkedGroup.getPositions()) {
        int offset = position.getOffset();
        uiProposal.addLinkedPosition(
            TrackedPositions.forStartLength(offset, length),
            false,
            groupId);
      }
      // add suggestions
      for (LinkedEditSuggestion suggestion : linkedGroup.getSuggestions()) {
        Image icon = getLinkedEditSuggestionIcon(suggestion);
        uiProposal.addLinkedPositionProposal(groupId, suggestion.getValue(), icon);
      }
    }
    // set end position
    {
      Position selection = sourceChange.getSelection();
      if (selection != null) {
        uiProposal.setEndPosition(TrackedPositions.forStartLength(selection.getOffset(), 0));
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
   * @return the {@link IFile} of the given {@link SourceFileEdit}, may be {@code null} if external.
   */
  private static IFile getFile(SourceFileEdit change) {
    String filePath = change.getFile();
    File fileJava = new File(filePath);
    return ResourceUtil.getFile(fileJava);
  }

  private static Image getLinkedEditSuggestionIcon(LinkedEditSuggestion suggestion) {
    String kind = suggestion.getKind();
    ImageDescriptor imageDescriptor = null;
    if (LinkedEditSuggestionKind.METHOD.equals(kind)) {
      imageDescriptor = DartPluginImages.DESC_DART_METHOD_PUBLIC;
    }
    if (LinkedEditSuggestionKind.PARAMETER.equals(kind)) {
      imageDescriptor = DartPluginImages.DESC_DART_LOCAL_VARIABLE;
    }
    if (LinkedEditSuggestionKind.TYPE.equals(kind)) {
      imageDescriptor = DartPluginImages.DESC_DART_CLASS_PUBLIC;
    }
    if (LinkedEditSuggestionKind.VARIABLE.equals(kind)) {
      imageDescriptor = DartPluginImages.DESC_DART_LOCAL_VARIABLE;
    }
    if (imageDescriptor == null) {
      return null;
    }
    return DartToolsPlugin.getImageDescriptorRegistry().get(imageDescriptor);
  }

  private static TextEdit[] toLTK(List<SourceEdit> edits) {
    List<TextEdit> ltkEdits = Lists.newArrayList();
    for (SourceEdit edit : edits) {
      TextEdit ltkEdit = toLTK(edit);
      ltkEdits.add(ltkEdit);
    }
    return ltkEdits.toArray(new TextEdit[ltkEdits.size()]);
  }

  private static TextEdit toLTK(SourceEdit edit) {
    return new ReplaceEdit(edit.getOffset(), edit.getLength(), edit.getReplacement());
  }

  private static int toProblemSeverity(String severity) {
    if (RefactoringProblemSeverity.FATAL.equals(severity)) {
      return RefactoringStatus.FATAL;
    }
    if (RefactoringProblemSeverity.ERROR.equals(severity)) {
      return RefactoringStatus.ERROR;
    }
    if (RefactoringProblemSeverity.WARNING.equals(severity)) {
      return RefactoringStatus.WARNING;
    }
    return RefactoringStatus.OK;
  }

  /**
   * @return the Dart status context for the given {@link Location}.
   */
  private static RefactoringStatusContext toRefactoringContext(Location location) {
    if (location == null) {
      return null;
    }
    return new DartStatusContext_NEW(location);
  }
}
