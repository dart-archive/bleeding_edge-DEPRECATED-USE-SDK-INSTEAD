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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.actions.SelectionConverter;
import com.google.dart.tools.ui.internal.callhierarchy.CallHierarchy;
import com.google.dart.tools.ui.internal.callhierarchy.CallLocation;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallHierarchyUI {
  private static final int DEFAULT_MAX_CALL_DEPTH = 10;
  private static final String PREF_MAX_CALL_DEPTH = "PREF_MAX_CALL_DEPTH"; //$NON-NLS-1$

  private static CallHierarchyUI SINGLETON;

  public static CallHierarchyUI getDefault() {
    if (SINGLETON == null) {
      SINGLETON = new CallHierarchyUI();
    }
    return SINGLETON;
  }

  public static IEditorPart isOpenInEditor(Object elem) {
    DartElement element = null;
    if (elem instanceof MethodWrapper) {
      element = ((MethodWrapper) elem).getMember();
    } else if (elem instanceof CallLocation) {
      element = ((CallLocation) elem).getCalledMember();
    }
    if (element != null) {
      return EditorUtility.isOpenInEditor(element);
    }
    return null;
  }

  public static void jumpToLocation(CallLocation callLocation) {
    try {
      IEditorPart methodEditor = DartUI.openInEditor(callLocation.getMember(), false, false);
      if (methodEditor instanceof ITextEditor) {
        ITextEditor editor = (ITextEditor) methodEditor;
        editor.selectAndReveal(
            callLocation.getStartPosition(),
            (callLocation.getEndPosition() - callLocation.getStartPosition()));
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    }
  }

  public static void jumpToMember(DartElement element) {
    if (element != null) {
      try {
        DartUI.openInEditor(element, true, true);
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      } catch (PartInitException e) {
        DartToolsPlugin.log(e);
      }
    }
  }

  /**
   * Opens the element in the editor or shows an error dialog if that fails.
   * 
   * @param element the element to open
   * @param shell parent shell for error dialog
   * @param activateOnOpen <code>true</code> if the editor should be activated
   * @return <code>true</code> iff no error occurred while trying to open the editor,
   *         <code>false</code> iff an error dialog was raised.
   */
  public static boolean openInEditor(Object element, Shell shell, boolean activateOnOpen) {
    CallLocation callLocation = CallHierarchy.getCallLocation(element);

    try {
      DartElement enclosingMember;
      int selectionStart;
      int selectionLength;

      if (callLocation != null) {
        enclosingMember = callLocation.getMember();
        selectionStart = callLocation.getStartPosition();
        selectionLength = callLocation.getEndPosition() - selectionStart;
      } else if (element instanceof MethodWrapper) {
        enclosingMember = ((MethodWrapper) element).getMember();
        SourceRange selectionRange = ((SourceReference) enclosingMember).getNameRange();
        if (selectionRange == null) {
          selectionRange = ((SourceReference) enclosingMember).getSourceRange();
        }
        if (selectionRange == null) {
          return true;
        }
        selectionStart = selectionRange.getOffset();
        selectionLength = selectionRange.getLength();
      } else {
        return true;
      }

      IEditorPart methodEditor = DartUI.openInEditor(enclosingMember, activateOnOpen, false);
      if (methodEditor instanceof ITextEditor) {
        ITextEditor editor = (ITextEditor) methodEditor;
        editor.selectAndReveal(selectionStart, selectionLength);
      }
      return true;
    } catch (DartModelException e) {
      DartToolsPlugin.log(new Status(
          IStatus.ERROR,
          DartToolsPlugin.getPluginId(),
          DartStatusConstants.INTERNAL_ERROR,
          CallHierarchyMessages.CallHierarchyUI_open_in_editor_error_message,
          e));

      ErrorDialog.openError(
          shell,
          CallHierarchyMessages.OpenLocationAction_error_title,
          CallHierarchyMessages.CallHierarchyUI_open_in_editor_error_message,
          e.getStatus());
      return false;
    } catch (PartInitException x) {
      String name;
      if (callLocation != null) {
        name = callLocation.getCalledMember().getElementName();
      } else if (element instanceof MethodWrapper) {
        name = ((MethodWrapper) element).getName();
      } else {
        name = ""; //$NON-NLS-1$
      }
      MessageDialog.openError(
          shell,
          CallHierarchyMessages.OpenLocationAction_error_title,
          Messages.format(
              CallHierarchyMessages.CallHierarchyUI_open_in_editor_error_messageArgs,
              new String[] {name, x.getMessage()}));
      return false;
    }
  }

  public static CallHierarchyViewPart openSelectionDialog(DartElement[] candidates,
      IWorkbenchWindow window) {
    Assert.isTrue(candidates != null);

    DartElement input = null;
    if (candidates.length > 1) {
      String title = CallHierarchyMessages.CallHierarchyUI_selectionDialog_title;
      String message = CallHierarchyMessages.CallHierarchyUI_selectionDialog_message;
      input = SelectionConverter.selectJavaElement(candidates, window.getShell(), title, message);
    } else if (candidates.length == 1) {
      input = candidates[0];
    }
    if (input == null) {
      return openView(new TypeMember[] {}, window);
    }

    return openView(new DartElement[] {input}, window);
  }

  public static CallHierarchyViewPart openView(DartElement[] input, IWorkbenchWindow window) {
    if (input.length == 0) {
      MessageDialog.openInformation(
          window.getShell(),
          CallHierarchyMessages.CallHierarchyUI_selectionDialog_title,
          CallHierarchyMessages.CallHierarchyUI_open_operation_unavialable);
      return null;
    }
    IWorkbenchPage page = window.getActivePage();
    try {
      CallHierarchyViewPart viewPart = getDefault().findLRUCallHierarchyViewPart(page); //find the first view which is not pinned
      String secondaryId = null;
      if (viewPart == null) {
        if (page.findViewReference(CallHierarchyViewPart.ID_CALL_HIERARCHY) != null) {
          secondaryId = String.valueOf(++getDefault().fViewCount);
        }
      } else {
        secondaryId = viewPart.getViewSite().getSecondaryId();
      }
      viewPart = (CallHierarchyViewPart) page.showView(
          CallHierarchyViewPart.ID_CALL_HIERARCHY,
          secondaryId,
          IWorkbenchPage.VIEW_ACTIVATE);
      viewPart.setInputElements(input);
      return viewPart;
    } catch (CoreException e) {
      ExceptionHandler.handle(
          e,
          window.getShell(),
          CallHierarchyMessages.CallHierarchyUI_error_open_view,
          e.getMessage());
    }
    return null;
  }

  /**
   * Converts an ISelection (containing MethodWrapper instances) to an ISelection with the
   * MethodWrapper's replaced by their corresponding TypeMembers. If the selection contains elements
   * which are not MethodWrapper instances or not already TypeMember instances they are discarded.
   * 
   * @param selection The selection to convert.
   * @return An ISelection containing TypeMember's in place of MethodWrapper instances.
   */
  static ISelection convertSelection(ISelection selection) {
    if (selection.isEmpty()) {
      return selection;
    }

    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      List<DartElement> elements = new ArrayList<DartElement>();
      for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
        Object element = iter.next();
        if (element instanceof MethodWrapper) {
          DartElement member = ((MethodWrapper) element).getMember();
          if (member != null) {
            elements.add(member);
          }
        } else if (element instanceof DartElement) {
          elements.add((DartElement) element);
        } else if (element instanceof CallLocation) {
          DartElement member = ((CallLocation) element).getMember();
          elements.add(member);
        }
      }
      return new StructuredSelection(elements);
    }
    return StructuredSelection.EMPTY;
  }

  private int fViewCount = 0;

  private final List<DartElement[]> fMethodHistory = new ArrayList<DartElement[]>();

  /**
   * List of the Call Hierarchy views in LRU order, where the most recently used view is at index 0.
   */
  private List<CallHierarchyViewPart> fLRUCallHierarchyViews = new ArrayList<CallHierarchyViewPart>();

  private CallHierarchyUI() {
    // Do nothing
  }

  /**
   * Returns the maximum tree level allowed
   */
  public int getMaxCallDepth() {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();
    int maxCallDepth = settings.getInt(PREF_MAX_CALL_DEPTH);
    if (maxCallDepth < 1 || maxCallDepth > 99) {
      maxCallDepth = DEFAULT_MAX_CALL_DEPTH;
    }
    return maxCallDepth;
  }

  public void setMaxCallDepth(int maxCallDepth) {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();
    settings.setValue(PREF_MAX_CALL_DEPTH, maxCallDepth);
  }

  /**
   * Adds the activated view part to the head of the list.
   * 
   * @param view the Call Hierarchy view part
   */
  void callHierarchyViewActivated(CallHierarchyViewPart view) {
    fLRUCallHierarchyViews.remove(view);
    fLRUCallHierarchyViews.add(0, view);
  }

  /**
   * Removes the closed view part from the list.
   * 
   * @param view the closed view part
   */
  void callHierarchyViewClosed(CallHierarchyViewPart view) {
    fLRUCallHierarchyViews.remove(view);
  }

  /**
   * Clears the history and updates all the open views.
   */
  void clearHistory() {
    for (Iterator<CallHierarchyViewPart> iter = fLRUCallHierarchyViews.iterator(); iter.hasNext();) {
      CallHierarchyViewPart part = iter.next();
      part.setHistoryEntries(new TypeMember[0][]);
      part.setInputElements(null);
    }
  }

  /**
   * Returns the method history.
   * 
   * @return the method history
   */
  List<DartElement[]> getMethodHistory() {
    return fMethodHistory;
  }

  /**
   * Finds the first Call Hierarchy view part instance that is not pinned.
   * 
   * @param page the active page
   * @return the Call Hierarchy view part to open or <code>null</code> if none found
   */
  private CallHierarchyViewPart findLRUCallHierarchyViewPart(IWorkbenchPage page) {
    boolean viewFoundInPage = false;
    for (Iterator<CallHierarchyViewPart> iter = fLRUCallHierarchyViews.iterator(); iter.hasNext();) {
      CallHierarchyViewPart view = iter.next();
      if (page.equals(view.getSite().getPage())) {
        if (!view.isPinned()) {
          return view;
        }
        viewFoundInPage = true;
      }
    }
    if (!viewFoundInPage) {
      // find unresolved views
      IViewReference[] viewReferences = page.getViewReferences();
      for (int i = 0; i < viewReferences.length; i++) {
        IViewReference curr = viewReferences[i];
        if (CallHierarchyViewPart.ID_CALL_HIERARCHY.equals(curr.getId())
            && page.equals(curr.getPage())) {
          CallHierarchyViewPart view = (CallHierarchyViewPart) curr.getView(true);
          if (view != null && !view.isPinned()) {
            return view;
          }
        }
      }
    }
    return null;
  }
}
