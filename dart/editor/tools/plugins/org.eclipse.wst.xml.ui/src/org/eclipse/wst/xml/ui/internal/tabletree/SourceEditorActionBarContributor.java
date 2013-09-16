/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.internal.ExtendedEditorActionBuilder;
import org.eclipse.wst.sse.ui.internal.IExtendedContributor;
import org.eclipse.wst.sse.ui.internal.ISourceViewerActionBarContributor;

public class SourceEditorActionBarContributor extends MultiPageEditorActionBarContributor implements
    IExtendedContributor {

  protected IEditorActionBarContributor designViewerActionBarContributor = null;
  protected IEditorActionBarContributor sourceViewerActionContributor = null;
  protected MultiPageEditorPart multiPageEditor = null;

  // EditorExtension
  private static final String EDITOR_ID = "org.eclipse.wst.xml.ui.XMLMultiPageEditorPart"; //$NON-NLS-1$
  private IExtendedContributor extendedContributor;

  public SourceEditorActionBarContributor() {
    super();

    sourceViewerActionContributor = new SourcePageActionContributor();

    // Read action extensions.
    ExtendedEditorActionBuilder builder = new ExtendedEditorActionBuilder();
    extendedContributor = builder.readActionExtensions(EDITOR_ID);
  }

  public void init(IActionBars actionBars) {
    super.init(actionBars);

    if (actionBars != null) {
      initDesignViewerActionBarContributor(actionBars);
      initSourceViewerActionContributor(actionBars);
    }
  }

  protected void initDesignViewerActionBarContributor(IActionBars actionBars) {
    if (designViewerActionBarContributor != null) {
      designViewerActionBarContributor.init(actionBars, getPage());
    }
  }

  protected void initSourceViewerActionContributor(IActionBars actionBars) {
    if (sourceViewerActionContributor != null) {
      sourceViewerActionContributor.init(actionBars, getPage());
    }
  }

  public void dispose() {
    super.dispose();

    if (designViewerActionBarContributor != null) {
      designViewerActionBarContributor.dispose();
    }

    if (sourceViewerActionContributor != null) {
      sourceViewerActionContributor.dispose();
    }

    if (extendedContributor != null) {
      extendedContributor.dispose();
    }

    multiPageEditor = null;
  }

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(IMenuManager)
   */
  public final void contributeToMenu(IMenuManager menu) {
    super.contributeToMenu(menu);

    //addToMenu(menu);

    if (extendedContributor != null) {
      extendedContributor.contributeToMenu(menu);
    }
  }

  protected void addToMenu(IMenuManager menu) {
  }

  /**
   * @see IExtendedContributor#contributeToPopupMenu(IMenuManager)
   */
  public final void contributeToPopupMenu(IMenuManager menu) {

    addToPopupMenu(menu);

    if (extendedContributor != null) {
      extendedContributor.contributeToPopupMenu(menu);
    }
  }

  protected void addToPopupMenu(IMenuManager menu) {
  }

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToToolBar(IToolBarManager)
   */
  public final void contributeToToolBar(IToolBarManager toolBarManager) {
    super.contributeToToolBar(toolBarManager);

    addToToolBar(toolBarManager);

    if (extendedContributor != null) {
      extendedContributor.contributeToToolBar(toolBarManager);
    }
  }

  protected void addToToolBar(IToolBarManager toolBarManager) {
  }

  /**
   * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToStatusLine(IStatusLineManager)
   */
  public final void contributeToStatusLine(IStatusLineManager manager) {
    super.contributeToStatusLine(manager);

    addToStatusLine(manager);

    if (extendedContributor != null) {
      extendedContributor.contributeToStatusLine(manager);
    }
  }

  protected void addToStatusLine(IStatusLineManager manager) {
  }

  /**
   * @see IExtendedContributor#updateToolbarActions()
   */
  public void updateToolbarActions() {
    if (extendedContributor != null) {
      extendedContributor.updateToolbarActions();
    }
  }

  public void setActiveEditor(IEditorPart targetEditor) {
    // save multiPageEditor before calling
    // super.setActiveEditor(targetEditor)
    // super.setActiveEditor will call setActivePage(IEditorPart
    // activeEditor)
    // multiPageEditor is needed in setActivePage(IEditorPart
    // activeEditor)
    if (targetEditor instanceof MultiPageEditorPart) {
      multiPageEditor = (MultiPageEditorPart) targetEditor;
    }

    super.setActiveEditor(targetEditor);

    updateToolbarActions();

    if (extendedContributor != null) {
      extendedContributor.setActiveEditor(targetEditor);
    }
  }

  public void setActivePage(IEditorPart activeEditor) {
    // This contributor is designed for StructuredTextMultiPageEditorPart.
    // To safe-guard this from problems caused by unexpected usage by
    // other editors, the following
    // check is added.
    if (multiPageEditor != null) {
      if ((activeEditor != null) && (activeEditor instanceof ITextEditor)) {
        activateSourcePage(activeEditor);
      } else {
        activateDesignPage(activeEditor);
      }
    }

    updateToolbarActions();

    IActionBars actionBars = getActionBars();
    if (actionBars != null) {
      // update menu bar and tool bar
      actionBars.updateActionBars();
    }
  }

  protected void activateDesignPage(IEditorPart activeEditor) {
    if ((designViewerActionBarContributor != null)
        && (designViewerActionBarContributor instanceof IDesignViewerActionBarContributor)) {
      designViewerActionBarContributor.setActiveEditor(multiPageEditor);
    }

    if ((sourceViewerActionContributor != null)
        && (sourceViewerActionContributor instanceof ISourceViewerActionBarContributor)) {
      // if design page is not really an IEditorPart, activeEditor ==
      // null, so pass in multiPageEditor instead (d282414)
      if (activeEditor == null) {
        sourceViewerActionContributor.setActiveEditor(multiPageEditor);
      } else {
        sourceViewerActionContributor.setActiveEditor(activeEditor);
      }
      ((ISourceViewerActionBarContributor) sourceViewerActionContributor).setViewerSpecificContributionsEnabled(false);
    }
  }

  protected void activateSourcePage(IEditorPart activeEditor) {
    if ((designViewerActionBarContributor != null)
        && (designViewerActionBarContributor instanceof IDesignViewerActionBarContributor)) {
      designViewerActionBarContributor.setActiveEditor(multiPageEditor);
    }

    if ((sourceViewerActionContributor != null)
        && (sourceViewerActionContributor instanceof ISourceViewerActionBarContributor)) {
      sourceViewerActionContributor.setActiveEditor(activeEditor);
      ((ISourceViewerActionBarContributor) sourceViewerActionContributor).setViewerSpecificContributionsEnabled(true);
    }
  }

}
