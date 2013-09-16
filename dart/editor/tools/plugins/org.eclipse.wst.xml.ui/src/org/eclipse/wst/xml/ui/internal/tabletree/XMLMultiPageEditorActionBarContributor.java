/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.wst.sse.ui.internal.ISourceViewerActionBarContributor;

public class XMLMultiPageEditorActionBarContributor extends SourceEditorActionBarContributor {

  private boolean needsMultiInit = true;

  public XMLMultiPageEditorActionBarContributor() {
    super();
  }

  protected void initDesignViewerActionBarContributor(IActionBars actionBars) {
    super.initDesignViewerActionBarContributor(actionBars);

    if (designViewerActionBarContributor != null) {
      if (designViewerActionBarContributor instanceof IDesignViewerActionBarContributor) {
        ((IDesignViewerActionBarContributor) designViewerActionBarContributor).initViewerSpecificContributions(actionBars);
      }
    }
  }

  protected void activateDesignPage(IEditorPart activeEditor) {
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

    if ((designViewerActionBarContributor != null)
        && (designViewerActionBarContributor instanceof IDesignViewerActionBarContributor)) {
      designViewerActionBarContributor.setActiveEditor(multiPageEditor);
      ((IDesignViewerActionBarContributor) designViewerActionBarContributor).setViewerSpecificContributionsEnabled(true);
    }
  }

  protected void activateSourcePage(IEditorPart activeEditor) {
    if ((designViewerActionBarContributor != null)
        && (designViewerActionBarContributor instanceof IDesignViewerActionBarContributor)) {
      designViewerActionBarContributor.setActiveEditor(multiPageEditor);
      ((IDesignViewerActionBarContributor) designViewerActionBarContributor).setViewerSpecificContributionsEnabled(false);
    }

    if ((sourceViewerActionContributor != null)
        && (sourceViewerActionContributor instanceof ISourceViewerActionBarContributor)) {
      sourceViewerActionContributor.setActiveEditor(activeEditor);
      ((ISourceViewerActionBarContributor) sourceViewerActionContributor).setViewerSpecificContributionsEnabled(true);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.EditorActionBarContributor#init(org.eclipse.ui.IActionBars)
   */
  public void init(IActionBars actionBars) {
    super.init(actionBars);
    needsMultiInit = true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
   */
  public void setActiveEditor(IEditorPart targetEditor) {
    if (needsMultiInit) {
      designViewerActionBarContributor = new XMLTableTreeActionBarContributor();
      initDesignViewerActionBarContributor(getActionBars());
      needsMultiInit = false;
    }
    super.setActiveEditor(targetEditor);
  }

}
