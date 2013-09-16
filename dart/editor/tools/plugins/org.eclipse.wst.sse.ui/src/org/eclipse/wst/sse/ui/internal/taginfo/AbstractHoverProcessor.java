/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.taginfo;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.wst.sse.ui.internal.derived.HTMLTextPresenter;

/**
 * Abstract class for providing hover information for Source editor. Includes a hover control
 * creator which has the "Press F2 for focus" message built in.
 * 
 * @since WTP 1.5
 */
abstract public class AbstractHoverProcessor implements ITextHover, ITextHoverExtension {
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
   */
  public IInformationControlCreator getHoverControlCreator() {
    return new IInformationControlCreator() {
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true),
            EditorsUI.getTooltipAffordanceString());
      }
    };
  }
}
