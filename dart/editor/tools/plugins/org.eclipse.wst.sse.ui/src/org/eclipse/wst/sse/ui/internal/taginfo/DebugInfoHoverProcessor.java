/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.taginfo;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;

/**
 * Provides debug hover help
 * 
 * @see org.eclipse.jface.text.ITextHover
 */
public class DebugInfoHoverProcessor extends AbstractHoverProcessor {
  public static final String TRACEFILTER = "debuginfohover"; //$NON-NLS-1$
  protected IPreferenceStore fPreferenceStore = null;

  public DebugInfoHoverProcessor() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer,
   * org.eclipse.jface.text.IRegion)
   */
  public String getHoverInfo(ITextViewer viewer, IRegion hoverRegion) {
    String displayText = null;
    if ((hoverRegion == null) || (viewer == null) || (viewer.getDocument() == null)) {
      displayText = null;
    } else {
      int offset = hoverRegion.getOffset();

      ITypedRegion region;
      try {
        region = viewer.getDocument().getPartition(offset);
        if (region != null) {
          displayText = region.getType();
        } else {
          displayText = "Null Region was returned?!"; //$NON-NLS-1$
        }
      } catch (BadLocationException e) {
        displayText = "BadLocationException Occurred!?"; //$NON-NLS-1$
      }

    }
    return displayText;
  }

  /**
   * Returns the region to hover the text over based on the offset.
   * 
   * @param textViewer
   * @param offset
   * @return IRegion region to hover over if offset is not over invalid whitespace. otherwise,
   *         returns <code>null</code>
   * @see org.eclipse.jface.text.ITextHover#getHoverRegion(ITextViewer, int)
   */
  public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
    ITypedRegion region = null;
    if ((textViewer == null) || (textViewer.getDocument() == null)) {
      region = null;
    } else {

      try {
        region = textViewer.getDocument().getPartition(offset);
      } catch (BadLocationException e) {
        region = null;
      }
    }
    return region;
  }
}
