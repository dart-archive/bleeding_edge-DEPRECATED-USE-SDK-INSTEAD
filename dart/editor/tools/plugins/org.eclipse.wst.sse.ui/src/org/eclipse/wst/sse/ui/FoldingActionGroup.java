/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

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
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;

import java.util.ResourceBundle;

class FoldingActionGroup extends ActionGroup {
  private static abstract class PreferenceAction extends ResourceAction implements IUpdate {
    PreferenceAction(ResourceBundle bundle, String prefix, int style) {
      super(bundle, prefix, style);
    }
  }

  private ProjectionViewer fViewer;

  private final PreferenceAction fToggle;
  private final TextOperationAction fExpand;
  private final TextOperationAction fExpandAll;
  private final IProjectionListener fProjectionListener;
  private final TextOperationAction fCollapse;
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
      fExpandAll = null;
      fCollapse = null;
      fCollapseAll = null;
      fProjectionListener = null;
      return;
    }

    fViewer = (ProjectionViewer) viewer;

    fProjectionListener = new IProjectionListener() {

      public void projectionEnabled() {
        update();
      }

      public void projectionDisabled() {
        update();
      }
    };

    fViewer.addProjectionListener(fProjectionListener);

    fToggle = new PreferenceAction(SSEUIMessages.getResourceBundle(),
        "Projection_Toggle_", IAction.AS_CHECK_BOX) { //$NON-NLS-1$
      public void run() {
        IPreferenceStore store = SSEUIPlugin.getDefault().getPreferenceStore();
        boolean current = store.getBoolean(AbstractStructuredFoldingStrategy.FOLDING_ENABLED);
        store.setValue(AbstractStructuredFoldingStrategy.FOLDING_ENABLED, !current);
      }

      public void update() {
        ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);

        boolean isEnabled = (target != null && target.canDoOperation(ProjectionViewer.TOGGLE));
        setEnabled(isEnabled);
      }
    };

    IPreferenceStore store = SSEUIPlugin.getDefault().getPreferenceStore();
    boolean checked = store.getBoolean(AbstractStructuredFoldingStrategy.FOLDING_ENABLED);
    fToggle.setChecked(checked);
    fToggle.setActionDefinitionId(IFoldingCommandIds.FOLDING_TOGGLE);
    editor.setAction("FoldingToggle", fToggle); //$NON-NLS-1$

    fExpand = new TextOperationAction(SSEUIMessages.getResourceBundle(),
        "Projection_Expand_", editor, ProjectionViewer.EXPAND, true); //$NON-NLS-1$
    fExpand.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND);
    editor.setAction("FoldingExpand", fExpand); //$NON-NLS-1$

    fExpandAll = new TextOperationAction(SSEUIMessages.getResourceBundle(),
        "Projection_ExpandAll_", editor, ProjectionViewer.EXPAND_ALL, true); //$NON-NLS-1$
    fExpandAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_EXPAND_ALL);
    editor.setAction("FoldingExpandAll", fExpandAll); //$NON-NLS-1$

    fCollapse = new TextOperationAction(SSEUIMessages.getResourceBundle(),
        "Projection_Collapse_", editor, ProjectionViewer.COLLAPSE, true); //$NON-NLS-1$
    fCollapse.setActionDefinitionId(IFoldingCommandIds.FOLDING_COLLAPSE);
    editor.setAction("FoldingCollapse", fCollapse); //$NON-NLS-1$

    fCollapseAll = new TextOperationAction(SSEUIMessages.getResourceBundle(),
        "Projection_CollapseAll_", editor, ProjectionViewer.COLLAPSE_ALL, true); //$NON-NLS-1$
    fCollapseAll.setActionDefinitionId(IFoldingCommandIds.FOLDING_COLLAPSE_ALL);
    editor.setAction("FoldingCollapseAll", fCollapseAll); //$NON-NLS-1$
  }

  /**
   * Returns <code>true</code> if the group is enabled.
   * 
   * <pre>
	 *        Invariant: isEnabled() &lt;=&gt; fViewer and all actions are != null.
	 * </pre>
   * 
   * @return <code>true</code> if the group is enabled
   */
  protected boolean isEnabled() {
    return fViewer != null;
  }

  /*
   * @see org.eclipse.ui.actions.ActionGroup#dispose()
   */
  public void dispose() {
    if (isEnabled()) {
      fViewer.removeProjectionListener(fProjectionListener);
      fViewer = null;
    }
    super.dispose();
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
    }
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
      manager.add(fExpand);
      manager.add(fExpandAll);
      manager.add(fCollapse);
      manager.add(fCollapseAll);
    }
  }

  /*
   * @see org.eclipse.ui.actions.ActionGroup#updateActionBars()
   */
  public void updateActionBars() {
    update();
  }
}
