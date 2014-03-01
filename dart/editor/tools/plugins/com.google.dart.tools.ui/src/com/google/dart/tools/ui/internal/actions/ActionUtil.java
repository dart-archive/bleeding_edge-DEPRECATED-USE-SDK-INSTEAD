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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
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
  private static final String STRING_SELECTION = "selection";
  private static final String STRING_SETTER = "setter";
  private static final String STRING_SPACE = " ";
  private static final String STRING_TYPE = "type";
  private static final String STRING_VARIABLE = "variable";
  private static final String STRING_FOR = "For";
  private static final String STRING_DO = "do:";
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

  public static String constructSelectionLabel(DartSelection selection) {
    StringBuffer text = new StringBuffer(STRING_FOR);
    text.append(STRING_SPACE);
    Element element = getActionElement(selection);
    if (element != null) {
      // prepare name
      String name = element.getDisplayName();
      if (element instanceof ConstructorElement) {
        ConstructorElement constructor = (ConstructorElement) element;
        String className = constructor.getEnclosingElement().getDisplayName();
        if (name.isEmpty()) {
          name = className + "()";
        } else {
          name = className + "." + element.getDisplayName();
        }
      }
      // show name or element kind
      if (name == null) {
        text.append(STRING_SELECTION);
      } else if (name.length() > MAX_NAME_LENGTH) {
        text.append(element.getKind().getDisplayName());
      } else {
        text.append('\"');
        text.append(name);
        text.append('\"');
      }
    } else {
      text.append(STRING_SELECTION);
    }
    text.append(STRING_SPACE);
    text.append(STRING_DO);
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

  /**
   * @return the {@link Element} to perform action on, may be {@code null}. In the most cases as
   *         simple as just {@link Element} of covered {@link AstNode}, but sometimes we want to be
   *         smarter.
   */
  public static Element getActionElement(DartSelection selection) {
    AssistContext context = selection.getContext();
    if (context == null) {
      return null;
    }
    return context.getCoveredElement();
  }

  /**
   * @return {@code true} if there are items in group of the given {@link IContributionManager}.
   */
  public static boolean hasItemsInGroup(IContributionManager manager, String groupId) {
    boolean groupFound = false;
    int numItems = 0;
    for (IContributionItem item : manager.getItems()) {
      if (item.isGroupMarker()) {
        if (item.getId().equals(groupId)) {
          groupFound = true;
        } else {
          groupFound = false;
        }
        continue;
      }
      if (groupFound) {
        numItems++;
      }
    }
    return numItems != 0;
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

  public static boolean isEditable(Shell shell, com.google.dart.engine.element.Element element) {
    Source source = element.getSource();
    if (source != null) {
      IResource resource = DartCore.getProjectManager().getResource(source);
      if (resource != null) {
        if (resource.isDerived()) {
          return false;
        }
        return !resource.getResourceAttributes().isReadOnly();
      }
    }
    return true;
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

  public static boolean isOnBuildPath(DartElement element) {

    return false;
  }

  public static boolean isOnBuildPath(Element element) {
    //TODO (pquitslund): when is an element *not* on the build path?
    return true;
  }

  public static boolean isOpenHierarchyAvailable(DartSelection selection) {
    if (selection == null) {
      return false;
    }
    // prepare AssistContext
    AssistContext context = selection.getContext();
    if (context == null) {
      return false;
    }
    // we need ClassElement 
    Element coveredElement = context.getCoveredElement();
    return coveredElement instanceof ClassElement;
  }

  public static boolean isProcessable(DartEditor editor) {
    if (editor == null) {
      return true;
    }
    Shell shell = editor.getSite().getShell();
    CompilationUnitElement input = editor.getInputElement();
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

  public static boolean isProcessable(Shell shell, Element element) {
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

  private ActionUtil() {
  }

}
