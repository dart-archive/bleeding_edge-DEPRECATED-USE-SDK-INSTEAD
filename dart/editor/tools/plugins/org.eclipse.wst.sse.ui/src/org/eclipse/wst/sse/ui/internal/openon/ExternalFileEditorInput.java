/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.editors.text.ILocationProvider;

import java.io.File;

/**
 * EditorInput for external files. Copied from
 * org.eclipse.ui.internal.editors.text.JavaFileEditorInput
 * 
 * @deprecated Use org.eclipse.ui.ide.FileStoreEditorInput and
 *             EFS.getLocalFileSystem().fromLocalFile()
 */
public class ExternalFileEditorInput implements IEditorInput, ILocationProvider {

  private File fFile;

  public ExternalFileEditorInput(File file) {
    super();
    fFile = file;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (o == this)
      return true;

    if (o instanceof ExternalFileEditorInput) {
      ExternalFileEditorInput input = (ExternalFileEditorInput) o;
      return fFile.equals(input.fFile);
    }

    return false;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#exists()
   */
  public boolean exists() {
    return fFile.exists();
  }

  /*
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter) {
    if (ILocationProvider.class.equals(adapter))
      return this;
    return Platform.getAdapterManager().getAdapter(this, adapter);
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
   */
  public ImageDescriptor getImageDescriptor() {
    return null;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getName()
   */
  public String getName() {
    return fFile.getName();
  }

  /*
   * @see org.eclipse.ui.editors.text.ILocationProvider#getPath(java.lang.Object)
   */
  public IPath getPath(Object element) {
    if (element instanceof ExternalFileEditorInput) {
      ExternalFileEditorInput input = (ExternalFileEditorInput) element;
      return new Path(input.fFile.getAbsolutePath());
    }
    return null;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getPersistable()
   */
  public IPersistableElement getPersistable() {
    return null;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getToolTipText()
   */
  public String getToolTipText() {
    return fFile.getAbsolutePath();
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return fFile.hashCode();
  }
}
