/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.hyperlink;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.URLHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.core.internal.util.JarUtilities;
import org.eclipse.wst.xml.core.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

/**
 * Hyperlink for URLs (opens in read-only mode)
 */
class URLStorageHyperlink implements IHyperlink {
  // copies of this class exist in:
  // org.eclipse.wst.xml.ui.internal.hyperlink
  // org.eclipse.wst.html.ui.internal.hyperlink
  // org.eclipse.jst.jsp.ui.internal.hyperlink

  static class StorageEditorInput implements IStorageEditorInput {
    IStorage fStorage = null;

    StorageEditorInput(IStorage storage) {
      fStorage = storage;
    }

    public IStorage getStorage() throws CoreException {
      return fStorage;
    }

    public boolean exists() {
      return fStorage != null;
    }

    public boolean equals(Object obj) {
      if (obj instanceof StorageEditorInput) {
        return fStorage.equals(((StorageEditorInput) obj).fStorage);
      }
      return super.equals(obj);
    }

    public ImageDescriptor getImageDescriptor() {
      return null;
    }

    public String getName() {
      return fStorage.getName();
    }

    public IPersistableElement getPersistable() {
      return null;
    }

    public String getToolTipText() {
      return fStorage.getFullPath() != null ? fStorage.getFullPath().toString()
          : fStorage.getName();
    }

    public Object getAdapter(Class adapter) {
      return null;
    }
  }

  static class URLStorage implements IStorage {
    URL fURL = null;

    URLStorage(URL url) {
      fURL = url;
    }

    public boolean equals(Object obj) {
      if (obj instanceof URLStorage) {
        return fURL.equals(((URLStorage) obj).fURL);
      }
      return super.equals(obj);
    }

    public InputStream getContents() throws CoreException {
      InputStream stream = null;
      try {
        if (fURL.toString().startsWith("jar:file"))
          stream = JarUtilities.getInputStream(fURL);
        else
          stream = fURL.openStream();
      } catch (Exception e) {
        throw new CoreException(new Status(IStatus.ERROR,
            XMLUIPlugin.getDefault().getBundle().getSymbolicName(), IStatus.ERROR, fURL.toString(),
            e));
      }
      return stream;
    }

    public IPath getFullPath() {
      return new Path(fURL.toString());
    }

    public String getName() {
      return new Path(fURL.getFile()).lastSegment();
    }

    public boolean isReadOnly() {
      return true;
    }

    public Object getAdapter(Class adapter) {
      return null;
    }

  }

  private IRegion fRegion;
  private URL fURL;

  public URLStorageHyperlink(IRegion region, URL url) {
    fRegion = region;
    fURL = url;
  }

  public IRegion getHyperlinkRegion() {
    return fRegion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
   */
  public String getTypeLabel() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
   */
  public String getHyperlinkText() {
    String path = fURL.toString();
    if (path.length() > 60) {
      path = path.substring(0, 25) + "..." + path.substring(path.length() - 25, path.length());
    }
    final String editorLabel = getEditorLabel();
    if (editorLabel != null)
      return NLS.bind(XMLUIMessages.Open_With, path, editorLabel);
    return NLS.bind(XMLUIMessages.Open, path);
  }

  protected String getEditorLabel() {
    IEditorDescriptor descriptor = getEditorDescriptor();
    return descriptor != null ? descriptor.getLabel() : null;
  }

  private IEditorDescriptor getEditorDescriptor() {
    final URLStorage storage = new URLStorage(fURL);
    final String path = fURL.getPath();
    String name = null;
    if (path != null)
      name = new Path(fURL.getPath()).lastSegment();

    IContentType contentType = null;
    try {
      InputStream is = null;
      try {
        is = storage.getContents();
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

    return PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(name, contentType);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
   */
  public void open() {
    if (fURL != null) {
      IEditorInput input = new StorageEditorInput(new URLStorage(fURL));
      try {
        final IEditorDescriptor descriptor = getEditorDescriptor();
        if (descriptor != null) {
          IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
          IDE.openEditor(page, input, descriptor.getId(), true);
        }
      } catch (PartInitException e) {
        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
        new URLHyperlink(fRegion, fURL.toString()).open();
      }
    }
  }
}
