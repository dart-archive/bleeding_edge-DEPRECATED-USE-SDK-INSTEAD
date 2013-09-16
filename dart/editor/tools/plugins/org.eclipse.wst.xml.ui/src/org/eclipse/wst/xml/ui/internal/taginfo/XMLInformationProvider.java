/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.taginfo;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

/**
 * Provides context information for XML tags (Shows tooltip description)
 * 
 * @deprecated StructuredTextViewerConfiguration creates the appropriate information provider
 */
public class XMLInformationProvider implements IInformationProvider, IInformationProviderExtension {

  private ITextHover fTextHover = null;

  public XMLInformationProvider() {
    fTextHover = SSEUIPlugin.getDefault().getTextHoverManager().createBestMatchHover(
        new XMLTagInfoHoverProcessor());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.information.IInformationProvider#getInformation(org.eclipse.jface.text
   * .ITextViewer, org.eclipse.jface.text.IRegion)
   */
  public String getInformation(ITextViewer textViewer, IRegion subject) {
    return (String) getInformation2(textViewer, subject);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.information.IInformationProviderExtension#getInformation2(org.eclipse
   * .jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
   */
  public Object getInformation2(ITextViewer textViewer, IRegion subject) {
    return fTextHover.getHoverInfo(textViewer, subject);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.information.IInformationProvider#getSubject(org.eclipse.jface.text.
   * ITextViewer, int)
   */
  public IRegion getSubject(ITextViewer textViewer, int offset) {
    return fTextHover.getHoverRegion(textViewer, offset);
  }
}
