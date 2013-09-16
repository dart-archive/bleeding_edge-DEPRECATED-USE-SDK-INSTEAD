/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author nitin
 */
public class StructuredModelDocumentProvider implements IDocumentProvider,
    IDocumentProviderExtension, IDocumentProviderExtension4 {
  private static StructuredModelDocumentProvider _instance = null;

  /**
   * @return Returns the instance.
   */
  public static StructuredModelDocumentProvider getInstance() {
    return _instance;
  }

  {
    _instance = new StructuredModelDocumentProvider();
  }

  private StructuredModelDocumentProvider() {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#aboutToChange(java.lang.Object)
   */
  public void aboutToChange(Object element) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.IDocumentProvider#addElementStateListener(org.eclipse.ui.texteditor
   * .IElementStateListener)
   */
  public void addElementStateListener(IElementStateListener listener) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#canSaveDocument(java.lang.Object)
   */
  public boolean canSaveDocument(Object element) {
    return ((IStructuredModel) element).isDirty();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#changed(java.lang.Object)
   */
  public void changed(Object element) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#connect(java.lang.Object)
   */
  public void connect(Object element) throws CoreException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#disconnect(java.lang.Object)
   */
  public void disconnect(Object element) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#getAnnotationModel(java.lang.Object)
   */
  public IAnnotationModel getAnnotationModel(Object element) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#getDocument(java.lang.Object)
   */
  public IDocument getDocument(Object element) {
    return ((IStructuredModel) element).getStructuredDocument();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#getModificationStamp(java.lang.Object)
   */
  public long getModificationStamp(Object element) {
    return ((IStructuredModel) element).getSynchronizationStamp();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#getStatus(java.lang.Object)
   */
  public IStatus getStatus(Object element) {
    return Status.OK_STATUS;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#getSynchronizationStamp(java.lang.Object)
   */
  public long getSynchronizationStamp(Object element) {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#isDeleted(java.lang.Object)
   */
  public boolean isDeleted(Object element) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#isModifiable(java.lang.Object)
   */
  public boolean isModifiable(Object element) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#isReadOnly(java.lang.Object)
   */
  public boolean isReadOnly(Object element) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#isStateValidated(java.lang.Object)
   */
  public boolean isStateValidated(Object element) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#mustSaveDocument(java.lang.Object)
   */
  public boolean mustSaveDocument(Object element) {
    return ((IStructuredModel) element).isDirty();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.IDocumentProvider#removeElementStateListener(org.eclipse.ui.texteditor
   * .IElementStateListener)
   */
  public void removeElementStateListener(IElementStateListener listener) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProvider#resetDocument(java.lang.Object)
   */
  public void resetDocument(Object element) throws CoreException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.IDocumentProvider#saveDocument(org.eclipse.core.runtime.IProgressMonitor
   * , java.lang.Object, org.eclipse.jface.text.IDocument, boolean)
   */
  public void saveDocument(IProgressMonitor monitor, Object element, IDocument document,
      boolean overwrite) throws CoreException {
    try {
      ((IStructuredModel) element).save();
    } catch (UnsupportedEncodingException e) {
    } catch (IOException e) {
    } catch (CoreException e) {
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#setCanSaveDocument(java.lang.Object)
   */
  public void setCanSaveDocument(Object element) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#synchronize(java.lang.Object)
   */
  public void synchronize(Object element) throws CoreException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#updateStateCache(java.lang.Object)
   */
  public void updateStateCache(Object element) throws CoreException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IDocumentProviderExtension#validateState(java.lang.Object,
   * java.lang.Object)
   */
  public void validateState(Object element, Object computationContext) throws CoreException {
  }

  public IContentType getContentType(Object element) throws CoreException {
    return Platform.getContentTypeManager().getContentType(
        ((IStructuredModel) element).getContentTypeIdentifier());
  }
}
