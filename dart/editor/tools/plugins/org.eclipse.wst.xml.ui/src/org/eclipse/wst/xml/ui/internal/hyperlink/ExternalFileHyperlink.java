/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.hyperlink;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * Hyperlink for external files.
 */
class ExternalFileHyperlink implements IHyperlink {
  // copies of this class exist in:
  // org.eclipse.wst.xml.ui.internal.hyperlink
  // org.eclipse.wst.html.ui.internal.hyperlink
  // org.eclipse.jst.jsp.ui.internal.hyperlink

  private IRegion fHyperlinkRegion;
  private File fHyperlinkFile;

  public ExternalFileHyperlink(IRegion region, File file) {
    fHyperlinkFile = file;
    fHyperlinkRegion = region;
  }

  public IRegion getHyperlinkRegion() {
    return fHyperlinkRegion;
  }

  public String getTypeLabel() {
    return null;
  }

  public String getHyperlinkText() {
    String path = fHyperlinkFile.getPath();
    if (path.length() > 60) {
      path = path.substring(0, 25) + "..." + path.substring(path.length() - 25, path.length());
    }
    final String editorLabel = getEditorLabel();
    if (editorLabel != null)
      return NLS.bind(XMLUIMessages.Open_With, path, editorLabel);
    return NLS.bind(XMLUIMessages.Open, path);
  }

  private String getEditorLabel() {
    final IFileStore store = EFS.getLocalFileSystem().getStore(fHyperlinkFile.toURI());
    final String name = store.fetchInfo().getName();

    if (name == null) {
      return null;
    }

    IContentType contentType = null;
    try {
      InputStream is = null;
      try {
        is = store.openInputStream(EFS.NONE, null);
        contentType = Platform.getContentTypeManager().findContentTypeFor(is, name);
      } finally {
        if (is != null) {
          is.close();
        }
      }
    } catch (CoreException ex) {
      // continue without content type
    } catch (IOException ex) {
      // continue without content type
    }

    final IEditorDescriptor descriptor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(
        name, contentType);
    return descriptor != null ? descriptor.getLabel() : null;
  }

  public void open() {
    if (fHyperlinkFile != null) {
      try {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IFileStore store = EFS.getLocalFileSystem().getStore(fHyperlinkFile.toURI());
        IDE.openEditorOnFileStore(page, store);
      } catch (PartInitException e) {
        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
      }
    }
  }
}
