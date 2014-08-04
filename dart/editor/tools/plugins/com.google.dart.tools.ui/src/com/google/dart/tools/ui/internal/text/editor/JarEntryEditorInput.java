/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * An EditorInput for a JarEntryFile.
 */
public class JarEntryEditorInput implements IStorageEditorInput {

  private final IStorage fJarEntryFile;

  public JarEntryEditorInput(IStorage jarEntryFile) {
    fJarEntryFile = jarEntryFile;
  }

  /*
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JarEntryEditorInput)) {
      return false;
    }
    JarEntryEditorInput other = (JarEntryEditorInput) obj;
    return fJarEntryFile.equals(other.fJarEntryFile);
  }

  /*
   * @see IEditorInput#exists()
   */
  @Override
  public boolean exists() {
    // JAR entries can't be deleted
    return true;
  }

  /*
   * @see IAdaptable#getAdapter(Class)
   */
  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    return null;
  }

  /*
   * @see IEditorInput#getContentType()
   */
  public String getContentType() {
    return fJarEntryFile.getFullPath().getFileExtension();
  }

  /*
   * @see IEditorInput#getImageDescriptor()
   */
  @Override
  public ImageDescriptor getImageDescriptor() {
    IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
    return registry.getImageDescriptor(getContentType());
  }

  /*
   * @see IEditorInput#getName()
   */
  @Override
  public String getName() {
    return fJarEntryFile.getName();
  }

  /*
   * @see IEditorInput#getPersistable()
   */
  @Override
  public IPersistableElement getPersistable() {
    return null;
  }

  /*
   * see IStorageEditorInput#getStorage()
   */
  @Override
  public IStorage getStorage() {
    return fJarEntryFile;
  }

  /*
   * @see IEditorInput#getToolTipText()
   */
  @Override
  public String getToolTipText() {
    IPath fullPath = fJarEntryFile.getFullPath();
    if (fullPath == null) {
      return null;
    }
    return fullPath.toString();
  }
}
