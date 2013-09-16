/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.swt.widgets.Shell;

/**
 * Escapes diff hover presentation text (converts < to &lt; > to &gt; etc...) so that html in the
 * diff file (displayed in hover) isn't presented as style (bold, italic, colors, etc...)
 * 
 * @deprecated
 */
public class StructuredLineChangeHover extends LineChangeHover {
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getHoverControlCreator()
   */
  public IInformationControlCreator getHoverControlCreator() {
    // use the default information control creator that just displays text
    // as text, not html content
    // because there is no special html that should be presented when just
    // showing diff
    // in the future, sourceviewer should be used instead of this plain
    // text control like java uses
    // SourceViewerInformationControl
    return new IInformationControlCreator() {
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent);
      }
    };
  }
}
