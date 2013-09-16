/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.ui.IActionBars;
import org.eclipse.wst.xml.ui.internal.actions.ActionContributorXML;

/**
 * SourcePageActionContributor This class is for multi page editor's source page contributor. Use
 * XMLEditorActionContributor for single page editor.
 */
public class SourcePageActionContributor extends ActionContributorXML {

  private IActionBars fBars;

  /**
   * This method calls:
   * <ul>
   * <li><code>contributeToMenu</code> with <code>bars</code>' menu manager</li>
   * <li><code>contributeToToolBar</code> with <code>bars</code>' tool bar manager</li>
   * <li><code>contributeToStatusLine</code> with <code>bars</code>' status line manager</li>
   * </ul>
   * The given action bars are also remembered and made accessible via <code>getActionBars</code>.
   * 
   * @param bars the action bars
   */
  public void init(IActionBars bars) {
    fBars = bars;
    contributeToMenu(bars.getMenuManager());
    contributeToToolBar(bars.getToolBarManager());
    contributeToStatusLine(bars.getStatusLineManager());
  }

  /**
   * Returns this contributor's action bars.
   * 
   * @return the action bars
   */
  public IActionBars getActionBars() {
    return fBars;
  }
}
