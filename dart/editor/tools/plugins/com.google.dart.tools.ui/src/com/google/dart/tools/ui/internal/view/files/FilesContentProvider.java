/*
 * Copyright 2012 Google Inc.
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
package com.google.dart.tools.ui.internal.view.files;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Files view content provider.
 */
public class FilesContentProvider implements ITreeContentProvider {

  private FilenameFilter filenameFilter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name != null && name.length() > 0 && name.charAt(0) != '.';
    }
  };

  private static final File[] NO_CHILDREN = new File[0];

  private Viewer viewer;

  public FilesContentProvider() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object element) {
    try {
      if (element instanceof File) {
        return getFileChildren((File) element);
      } else if (element instanceof TopLevelDirectoriesWrapper) {
        return ((TopLevelDirectoriesWrapper) element).getChildren();
      }
    } catch (Exception e) {
      //fall through
    }
    return NO_CHILDREN;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof File) {
      File file = (File) element;
      return file.getParentFile();
    } else {
      return null;
    }
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = viewer;
  }

  private Object[] getFileChildren(File file) throws CoreException {
    if (file.isDirectory()) {
      return file.listFiles(filenameFilter);
    } else {
      return NO_CHILDREN;
    }
  }
}
