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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.FoldingMessages;
import com.google.dart.tools.ui.actions.IJavaEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.IFoldingCommandIds;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.TextOperationAction;

import java.util.ResourceBundle;

/**
 * Groups the JDT folding actions.
 */
public class FoldingActionGroup extends ActionGroup {
  /**
	 * 
	 */
  private class FoldingAction extends PreferenceAction {

    FoldingAction(ResourceBundle bundle, String prefix) {
      super(bundle, prefix, IAction.AS_PUSH_BUTTON);
    }

    @Override
    public void update() {
      setEnabled(FoldingActionGroup.this.isEnabled() && fViewer.isProjectionMode());
    }

  }

  private static abstract class PreferenceAction extends ResourceAction implements IUpdate {
    PreferenceAction(ResourceBundle bundle, String prefix, int style) {
      super(bundle, prefix, style);
    }
  }

  private ProjectionViewer fViewer;

  private final PreferenceAction fToggle;
  private final TextOperationAction fExpand;
  private final TextOperationAction fCollapse;
  private final TextOperationAction fExpandAll;
  private final IProjectionListener fProjectionListener;

  /* since 3.2 */
  private final PreferenceAction fRestoreDefaults;
  private final FoldingAction fCollapseMembers;
  private final FoldingAction fCollapseComments;
  private final TextOperationAction fCollapseAll;

  /**
   * Creates a new projection action group for <code>editor</code>. If the supplied viewer is not an
   * instance of <code>ProjectionViewer</code>, the action group is disabled.
   * 
   * @param editor the text editor to operate on
   * @param viewer the viewer of the editor
   */
  public FoldingActionGroup(final ITextEditor editor, ITextViewer viewer) {
    if (!(viewer instanceof ProjectionViewer)) {
      fToggle = null;
      fExpand = null;
      fCollapse = null;
      fExpandAll = null;
      fCollapseAll = null;
      fRestoreDefaults = null;
      fCollapseMembers = null;
      fCollapseComments = null;
      fProjectionListener = null;
      return;
    }

    fViewer = (ProjectionViewer) viewer;

    fProjectionListener = new IProjectionListener() {

      @Override
      public void projectionDisabled() {
        update();
      }

      @Override
      public void projectionEnabled() {
        update();
      }
    };

    fViewer.addProjectionListener(fProjectionListener);

    fToggle = new PreferenceAction(FoldingMessages.getResourceBundle(),
        "Projection.Toggle.", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
      @Override
      public void run() {
        IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
        boolean current = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED);
        store.setValue(PreferenceConstants.EDITOR_FOLDING_ENABLED, !current);
      }

      @Override
      public void update() {
        ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);

        boolean isEnabled = (target != null && target.canDoOperation(ProjectionViewer.TOGGLE));
        setEnabled(isEnabled);
      }
    };
    fToggle.setChecked(true);
    fToggle.setActionDefinitionId(IFoldingCommandIds.FOLDING_TOGGLE);
    editor.setAction("FoldingToggle", fToggle); //$NON-NLS-1$

    fExpandAll = new TextOperationAction(FoldingMessages.getResourceBundle(),
        "Projection.ExpandAll.", editor, ProjectionViewer.EXPAND_ALL, true); //$NON-NLS-1$
    fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
    editor.setAction("FoldingExpandAll", fExpandAll); //$NON-NLS-1$

    fCollapseAll = new TextOperationAction(FoldingMessages.getResourceBundle(),
        "Projection.CollapseAll.", editor, ProjectionViewer.COLLAPSE_ALL, true); //$NON-NLS-1$
    fCollapseAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_COLLAPSE_ALL);
    editor.setAction("FoldingCollapseAll", fCollapseAll); //$NON-NLS-1$

    fExpand = new TextOperationAction(FoldingMessages.getResourceBundle(),
        "Projection.Expand.", editor, ProjectionViewer.EXPAND, true); //$NON-NLS-1$
    fExpand.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND);
    editor.setAction("FoldingExpand", fExpand); //$NON-NLS-1$

    fCollapse = new TextOperationAction(FoldingMessages.getResourceBundle(),
        "Projection.Collapse.", editor, ProjectionViewer.COLLAPSE, true); //$NON-NLS-1$
    fCollapse.setActionDefinitionId(IFoldingCommandIds.FOLDING_COLLAPSE);
    editor.setAction("FoldingCollapse", fCollapse); //$NON-NLS-1$

    fRestoreDefaults = new FoldingAction(FoldingMessages.getResourceBundle(), "Projection.Restore.") { //$NON-NLS-1$
      @Override
      public void run() {
        if (editor instanceof DartEditor) {
          DartEditor javaEditor = (DartEditor) editor;
          javaEditor.resetProjection();
        }
      }
    };
    fRestoreDefaults.setActionDefinitionId(IFoldingCommandIds.FOLDING_RESTORE);
    editor.setAction("FoldingRestore", fRestoreDefaults); //$NON-NLS-1$

    fCollapseMembers = new FoldingAction(FoldingMessages.getResourceBundle(),
        "Projection.CollapseMembers.") { //$NON-NLS-1$
      @Override
      public void run() {
        if (editor instanceof DartEditor) {
          DartEditor javaEditor = (DartEditor) editor;
          javaEditor.collapseMembers();
        }
      }
    };
    fCollapseMembers.setActionDefinitionId(IJavaEditorActionDefinitionIds.FOLDING_COLLAPSE_MEMBERS);
    editor.setAction("FoldingCollapseMembers", fCollapseMembers); //$NON-NLS-1$

    fCollapseComments = new FoldingAction(FoldingMessages.getResourceBundle(),
        "Projection.CollapseComments.") { //$NON-NLS-1$
      @Override
      public void run() {
        if (editor instanceof DartEditor) {
          DartEditor javaEditor = (DartEditor) editor;
          javaEditor.collapseComments();
        }
      }
    };
    fCollapseComments.setActionDefinitionId(IJavaEditorActionDefinitionIds.FOLDING_COLLAPSE_COMMENTS);
    editor.setAction("FoldingCollapseComments", fCollapseComments); //$NON-NLS-1$
  }

  /*
   * @see org.eclipse.ui.actions.ActionGroup#dispose()
   */
  @Override
  public void dispose() {
    if (isEnabled()) {
      fViewer.removeProjectionListener(fProjectionListener);
      fViewer = null;
    }
    super.dispose();
  }

  /**
   * Fills the menu with all folding actions.
   * 
   * @param manager the menu manager for the folding submenu
   */
  public void fillMenu(IMenuManager manager) {
    if (isEnabled()) {
      update();
      manager.add(fToggle);
      manager.add(fExpandAll);
      manager.add(fExpand);
      manager.add(fCollapse);
      manager.add(fCollapseAll);
      manager.add(fRestoreDefaults);
      manager.add(fCollapseMembers);
      manager.add(fCollapseComments);
    }
  }

  /*
   * @see org.eclipse.ui.actions.ActionGroup#updateActionBars()
   */
  @Override
  public void updateActionBars() {
    update();
  }

  /**
   * Returns <code>true</code> if the group is enabled.
   * 
   * <pre>
   * Invariant: isEnabled() <=> fViewer and all actions are != null.
   * </pre>
   * 
   * @return <code>true</code> if the group is enabled
   */
  protected boolean isEnabled() {
    return fViewer != null;
  }

  /**
   * Updates the actions.
   */
  protected void update() {
    if (isEnabled()) {
      fToggle.update();
      fToggle.setChecked(fViewer.isProjectionMode());
      fExpand.update();
      fExpandAll.update();
      fCollapse.update();
      fCollapseAll.update();
      fRestoreDefaults.update();
      fCollapseMembers.update();
      fCollapseComments.update();
    }
  }
}
