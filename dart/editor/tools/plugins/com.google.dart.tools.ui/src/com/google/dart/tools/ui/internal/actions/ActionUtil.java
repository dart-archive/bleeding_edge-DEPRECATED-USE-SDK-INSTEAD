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
package com.google.dart.tools.ui.internal.actions;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.ExternalDartProject;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartElementSelection;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

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

public class ActionUtil {

  private static final String STRING_ALIAS = "alias"; // TODO(messick): Externalize strings.
  private static final String STRING_CLASS = "class";
  private static final String STRING_FIELD = "field";
  private static final String STRING_FUNCTION = "function";
  private static final String STRING_GETTER = "getter";
  private static final String STRING_IMPORT = "import";
  private static final String STRING_METHOD = "method";
  private static final String STRING_OF = " of ";
  private static final String STRING_SELECTION = "selection";
  private static final String STRING_SETTER = "setter";
  private static final String STRING_SPACE = " ";
  private static final String STRING_TYPE = "type";
  private static final String STRING_VARIABLE = "variable";
  private static final int MAX_NAME_LENGTH = 30; // prevent menus from filling the screen

  public static boolean areProcessable(Shell shell, DartElement[] elements) {
    for (int i = 0; i < elements.length; i++) {
      if (!isOnBuildPath(elements[i])) {
        MessageDialog.openInformation(
            shell,
            ActionMessages.ActionUtil_notOnBuildPath_title,
            Messages.format(
                ActionMessages.ActionUtil_notOnBuildPath_resource_message,
                new Object[] {elements[i].getPath()}));
        return false;
      }
    }
    return true;
  }

  public static String constructMenuText(String template, boolean isAdjectivePhrase,
      DartTextSelection selection) {
    StringBuffer text = new StringBuffer(template);
    String sep = isAdjectivePhrase ? STRING_OF : STRING_SPACE;
    try {
      DartElement[] elements = selection.resolveElementAtOffset();
      if (elements.length == 1) {
        String name = elements[0].getElementName();
        text.append(sep);
        if (name == null) {
          text.append(STRING_SELECTION);
        } else if (name.length() > MAX_NAME_LENGTH) {
          text.append(findGenericName(elements[0]));
        } else {
          text.append('\"');
          text.append(name);
          text.append('\"');
        }
      } else {
        DartNode node = getResolvedNodeFromSelection(selection);
        String src;
        if ((node instanceof com.google.dart.compiler.ast.DartIdentifier)
            && ((src = node.toSource()) != null)) {
          // TODO(pquitslund): Searches that begin when this branch is taken always fail.
          text.append(sep);
          text.append('\"');
          text.append(src);
          text.append('\"');
        } else {
          text.append(sep);
          text.append(STRING_SELECTION);
        }
      }
    } catch (DartModelException ex) {
      // should not happen
      text.append(sep);
      text.append(STRING_SELECTION);
    }
    return text.toString();
  }

  public static String findGenericName(DartElement element) {
    switch (element.getElementType()) {
      case DartElement.FIELD:
        return STRING_FIELD;
      case DartElement.FUNCTION:
        DartFunction function = (DartFunction) element;
        // TODO(scheglov): Investigate functions (e.g. window) that reply "false" to isGetter().
        if (function.isGetter()) {
          return STRING_GETTER;
        } else if (function.isSetter()) {
          return STRING_SETTER;
        }
        return STRING_FUNCTION;
      case DartElement.FUNCTION_TYPE_ALIAS:
        return STRING_ALIAS;
      case DartElement.IMPORT:
        return STRING_IMPORT;
      case DartElement.METHOD:
        Method method = (Method) element;
        if (method.isGetter()) {
          return STRING_GETTER;
        } else if (method.isSetter()) {
          return STRING_SETTER;
        }
        return STRING_METHOD;
      case DartElement.TYPE:
        Type type = (Type) element;
        try {
          if (type.isClass()) {
            return STRING_CLASS;
          }
        } catch (DartModelException ex) {
          // fall thru
        }
        return STRING_TYPE;
      case DartElement.TYPE_PARAMETER:
        return STRING_TYPE;
      case DartElement.VARIABLE:
        return STRING_VARIABLE;
    }
    return STRING_SELECTION;
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

        // see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#validateEditorInputState()
        final String warnKey = AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WARN_IF_INPUT_DERIVED;
        IPreferenceStore store = EditorsUI.getPreferenceStore();
        if (!store.getBoolean(warnKey)) {
          return true;
        }

        MessageDialogWithToggle toggleDialog = MessageDialogWithToggle.openYesNoQuestion(
            shell,
            ActionMessages.ActionUtil_warning_derived_title,
            Messages.format(
                ActionMessages.ActionUtil_warning_derived_message,
                resource.getFullPath().toString()),
            ActionMessages.ActionUtil_warning_derived_dontShowAgain,
            false,
            null,
            null);

        EditorsUI.getPreferenceStore().setValue(warnKey, !toggleDialog.getToggleState());

        return toggleDialog.getReturnCode() == IDialogConstants.YES_ID;
      }
    }
    return true;
  }

  public static boolean isFindDeclarationsAvailable(DartElementSelection selection) {
    DartElement[] selectedElements = selection.toArray();
    if (selectedElements.length > 0) {
      if (selectedElements[0] instanceof Method || selectedElements[0] instanceof Field) {
        return true;
      }
    }
    DartNode node = getResolvedNodeFromSelection(selection);
    if (node != null) {
      if (node instanceof DartIdentifier) {
        DartIdentifier id = (DartIdentifier) node;
        if (id.getParent() instanceof DartPropertyAccess) {
          return true;
        }
        if (id.getParent() instanceof DartMethodInvocation) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isFindUsesAvailable(DartElementSelection selection) {
    DartNode node = getResolvedNodeFromSelection(selection);
    if (node != null) {
      if (node instanceof DartIdentifier) {
        return true;
      }
    }
    return false;
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

  public static boolean isOpenDeclarationAvailable(DartElementSelection selection) {
    if (selection.toArray().length == 1) {
      com.google.dart.compiler.type.Type type;
      DartNode[] nodes = selection.resolveSelectedNodes();
      if (nodes != null && nodes.length > 0) {
        DartNode node = nodes[0];
        type = node.getType();
        if (type != null) {
          return true;
        }
      }
      DartNode node = selection.resolveCoveringNode();
      if (node != null) {
        type = node.getType();
        if (type != null) {
          return true;
        }
        if (node.getElement() != null && node.getElement().getType() != null) {
          if (node.getParent() != null) {
            DartNode parent = node.getParent().getParent();
            // No need to "Open" a declaration if that's what is selected.
            if (parent == null || parent instanceof DartUnit) {
              return false;
            }
            if (parent instanceof com.google.dart.compiler.ast.DartFieldDefinition) {
              return false;
            }
            if (parent instanceof com.google.dart.compiler.ast.DartMethodDefinition) {
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isOpenHierarchyAvailable(DartElementSelection selection) {
    return !selection.isEmpty() && selection.getFirstElement() instanceof Type;
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
      MessageDialog.openInformation(
          shell,
          ActionMessages.ActionUtil_notOnBuildPath_title,
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
    MessageDialog.openInformation(
        shell,
        ActionMessages.ActionUtil_notOnBuildPath_title,
        ActionMessages.ActionUtil_notOnBuildPath_message);
    return false;
  }

  public static boolean mustDisableDartModelAction(Shell shell, Object element) {
    IResource resource = ResourceUtil.getResource(element);
    if ((resource == null) || (!(resource instanceof IFolder)) || (!resource.isLinked())) {
      return false;
    }

    MessageDialog.openInformation(
        shell,
        ActionMessages.ActionUtil_not_possible,
        ActionMessages.ActionUtil_no_linked);
    return true;
  }

  private static DartNode getResolvedNodeFromSelection(DartTextSelection selection) {
    DartNode node = null;
    DartNode[] nodes = selection.resolveSelectedNodes();
    if (nodes != null && nodes.length > 0) {
      node = nodes[0];
    }
    if (node == null) {
      node = selection.resolveCoveringNode();
      if (node instanceof com.google.dart.compiler.ast.DartFunction) {
        if (node.getParent() instanceof DartMethodDefinition) {
          node = ((DartMethodDefinition) node.getParent()).getName();
        }
      }
    } else if (node instanceof com.google.dart.compiler.ast.DartField) {
      com.google.dart.compiler.ast.DartField field = (com.google.dart.compiler.ast.DartField) node;
      SourceInfo info = field.getSourceInfo();
      if (info.getOffset() == selection.getOffset() & info.getLength() == selection.getLength()) {
        return field.getName();
      }
    }
    return node;
  }

  private ActionUtil() {
  }

}
