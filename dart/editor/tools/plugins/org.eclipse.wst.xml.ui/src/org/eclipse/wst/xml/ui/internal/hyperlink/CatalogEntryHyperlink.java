/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.hyperlink;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

class CatalogEntryHyperlink implements IHyperlink {

  private IRegion fHyperlinkRegion = null;
  private ICatalogEntry fEntry = null;

  /**
   * @param hyperlinkRegion
   * @param entry
   */
  CatalogEntryHyperlink(IRegion hyperlinkRegion, ICatalogEntry entry) {
    super();
    fHyperlinkRegion = hyperlinkRegion;
    fEntry = entry;
  }

  /**
   * @return
   */
  private IHyperlink getHyperlink() {
    if (fEntry.getURI().startsWith("file:")) { //$NON-NLS-1$
      return new ExternalFileHyperlink(fHyperlinkRegion, new File(fEntry.getURI().substring(5)));
    } else if (fEntry.getURI().startsWith("platform:/resource/")) { //$NON-NLS-1$
      IPath path = new Path(fEntry.getURI().substring(20));
      if (path.segmentCount() > 1)
        return new WorkspaceFileHyperlink(fHyperlinkRegion,
            ResourcesPlugin.getWorkspace().getRoot().getFile(path));
    } else {
      /*
       * the URL detector will already work on the literal text, so offer to open the contents in an
       * editor
       */
      try {
        if (fEntry.getURI().startsWith("jar:file:"))
          return new URLStorageHyperlink(fHyperlinkRegion, new URL(fEntry.getURI())) {
            public String getHyperlinkText() {
              final String editorLabel = getEditorLabel();
              if (editorLabel != null)
                return NLS.bind(XMLUIMessages.Open_With, fEntry.getKey(), editorLabel);
              return NLS.bind(XMLUIMessages.Open, fEntry.getKey());
            }
          };
      } catch (MalformedURLException e) {
        // not valid?
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
   */
  public IRegion getHyperlinkRegion() {
    return fHyperlinkRegion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
   */
  public String getHyperlinkText() {
    IHyperlink link = getHyperlink();
    if (link != null)
      return link.getHyperlinkText();
    return NLS.bind(XMLUIMessages.Open, fEntry.getKey());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
   */
  public String getTypeLabel() {
    IHyperlink link = getHyperlink();
    if (link != null)
      return link.getTypeLabel();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
   */
  public void open() {
    IHyperlink link = getHyperlink();
    if (link != null)
      link.open();
  }
}
