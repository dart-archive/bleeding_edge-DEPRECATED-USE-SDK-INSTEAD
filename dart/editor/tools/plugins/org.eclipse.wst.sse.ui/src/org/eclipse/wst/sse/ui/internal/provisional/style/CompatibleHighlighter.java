/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.style;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=224209 Created to provide compatibility with
 * refreshDisplay() and getTextViewer() methods on the superclass that might be called by
 * LineStyleProviders.
 */
public final class CompatibleHighlighter extends Highlighter {

  private ITextViewer textViewer;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#addProvider(java.lang.String,
   * org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider)
   */
  public void addProvider(String partitionType, LineStyleProvider provider) {
    super.addProvider(partitionType, provider);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#getTextViewer()
   */
  public ITextViewer getTextViewer() {
    return super.getTextViewer();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#install(org.eclipse.jface.text
   * .ITextViewer)
   */
  public void install(ITextViewer newTextViewer) {
    textViewer = newTextViewer;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#lineGetStyle(int, int)
   */
  public StyleRange[] lineGetStyle(int eventLineOffset, int eventLineLength) {
    return new StyleRange[0];
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#lineGetStyle(org.eclipse.swt.
   * custom.LineStyleEvent)
   */
  public void lineGetStyle(LineStyleEvent event) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#refreshDisplay()
   */
  public void refreshDisplay() {
    if (textViewer != null)
      textViewer.invalidateTextPresentation();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#refreshDisplay(int, int)
   */
  public void refreshDisplay(int start, int length) {
    refreshDisplay();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#removeProvider(java.lang.String)
   */
  public void removeProvider(String partitionType) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#setDocument(org.eclipse.wst.sse
   * .core.internal.provisional.text.IStructuredDocument)
   */
  public void setDocument(IStructuredDocument structuredDocument) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#setDocumentPartitioning(java.
   * lang.String)
   */
  public void setDocumentPartitioning(String partitioning) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.provisional.style.Highlighter#uninstall()
   */
  public void uninstall() {
    super.uninstall();
    textViewer = null;
  }
}
