/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.ExternalDartProject;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/*
 * http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
 */
public class ActionUtil {

  public static boolean areProcessable(Shell shell, DartElement[] elements) {
    for (int i = 0; i < elements.length; i++) {
      if (!isOnBuildPath(elements[i])) {
        MessageDialog.openInformation(shell, ActionMessages.ActionUtil_notOnBuildPath_title,
            Messages.format(ActionMessages.ActionUtil_notOnBuildPath_resource_message,
                new Object[] {elements[i].getPath()}));
        return false;
      }
    }
    return true;
  }

  public static boolean isEditable(DartEditor editor) {
    if (!isProcessable(editor)) {
      return false;
    }

    return editor.validateEditorInputState();
  }

  /**
   * Check whether <code>editor</code> and <code>element</code> are processable and editable. If the
   * editor edits the element, the validation is only performed once. If necessary, ask the user
   * whether the file(s) should be edited.
   * 
   * @param editor an editor, or <code>null</code> iff the action was not executed from an editor
   * @param shell a shell to serve as parent for a dialog
   * @param element the element to check, cannot be <code>null</code>
   * @return <code>true</code> if the element can be edited, <code>false</code> otherwise
   */
  public static boolean isEditable(DartEditor editor, Shell shell, DartElement element) {
    if (editor != null) {
      DartElement input = SelectionConverter.getInput(editor);
      if (input != null && input.equals(element.getAncestor(CompilationUnit.class))) {
        return isEditable(editor);
      } else {
        return isEditable(editor) && isEditable(shell, element);
      }
    }
    return isEditable(shell, element);
  }

  public static boolean isEditable(Shell shell, DartElement element) {
    if (!isProcessable(shell, element)) {
      return false;
    }

    DartElement cu = element.getAncestor(CompilationUnit.class);
    if (cu != null) {
      IResource resource = cu.getResource();
      if (resource != null && resource.isDerived()) {

        // see
// org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#validateEditorInputState()
        final String warnKey = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WARN_IF_INPUT_DERIVED;
        IPreferenceStore store = EditorsUI.getPreferenceStore();
        if (!store.getBoolean(warnKey)) {
          return true;
        }

        MessageDialogWithToggle toggleDialog = MessageDialogWithToggle.openYesNoQuestion(shell,
            ActionMessages.ActionUtil_warning_derived_title, Messages.format(
                ActionMessages.ActionUtil_warning_derived_message,
                resource.getFullPath().toString()),
            ActionMessages.ActionUtil_warning_derived_dontShowAgain, false, null, null);

        EditorsUI.getPreferenceStore().setValue(warnKey, !toggleDialog.getToggleState());

        return toggleDialog.getReturnCode() == IDialogConstants.YES_ID;
      }
    }
    return true;
  }

  public static boolean isOnBuildPath(DartElement element) {
    // fix for bug http://dev.eclipse.org/bugs/show_bug.cgi?id=20051
    if (element.getElementType() == DartElement.DART_PROJECT) {
      return true;
    }
    DartProject project = element.getDartProject();
    if (project instanceof ExternalDartProject) {
      return true;
    }
    try {
      // if (!project.isOnClasspath(element))
      // return false;
      IProject resourceProject = project.getProject();
      if (resourceProject == null) {
        return false;
      }
      IProjectNature nature = resourceProject.getNature(DartCore.DART_PROJECT_NATURE);
      // We have a Dart project
      if (nature != null) {
        return true;
      }
    } catch (CoreException e) {
    }
    return false;
  }

  public static boolean isProcessable(DartEditor editor) {
    if (editor == null) {
      return true;
    }
    Shell shell = editor.getSite().getShell();
    DartElement input = SelectionConverter.getInput(editor);
    // if a Java editor doesn't have an input of type Java element
    // then it is for sure not on the build path
    if (input == null) {
      MessageDialog.openInformation(shell, ActionMessages.ActionUtil_notOnBuildPath_title,
          ActionMessages.ActionUtil_notOnBuildPath_message);
      return false;
    }
    return isProcessable(shell, input);
  }

  public static boolean isProcessable(Shell shell, DartElement element) {
    if (element == null) {
      return true;
    }
    if (isOnBuildPath(element)) {
      return true;
    }
    MessageDialog.openInformation(shell, ActionMessages.ActionUtil_notOnBuildPath_title,
        ActionMessages.ActionUtil_notOnBuildPath_message);
    return false;
  }

  public static boolean mustDisableDartModelAction(Shell shell, Object element) {
    IResource resource = ResourceUtil.getResource(element);
    if ((resource == null) || (!(resource instanceof IFolder)) || (!resource.isLinked())) {
      return false;
    }

    MessageDialog.openInformation(shell, ActionMessages.ActionUtil_not_possible,
        ActionMessages.ActionUtil_no_linked);
    return true;
  }

  private ActionUtil() {
  }

}
