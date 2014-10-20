/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.ui.internal.filesview.nodes.old;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Adapter for EFS filestores. (The Files View uses {@link IFileStore}s to represent referenced
 * libraries.
 */
public class FileStoreAdapterFactory implements IAdapterFactory {

  static class FileStoreWorkbenchAdapter implements IWorkbenchAdapter {
    private static final IResource[] NO_CHILDREN = new IResource[0];

    @Override
    public Object[] getChildren(Object object) {
      try {
        if (object instanceof IFileStore) {
          IFileStore fileStore = (IFileStore) object;
          return fileStore.childStores(EFS.NONE, null);
        }
      } catch (CoreException exception) {
        //fall through
      }
      return NO_CHILDREN;
    }

    @Override
    public ImageDescriptor getImageDescriptor(Object object) {
      if (object instanceof IFileStore) {
        IFileStore fileStore = (IFileStore) object;

        try {
          if (fileStore.fetchInfo().isDirectory()) {
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJ_FOLDER);
          }

          IEditorDescriptor descriptor = IDE.getEditorDescriptor(fileStore.getName());

          if (descriptor != null) {
            return descriptor.getImageDescriptor();
          } else {
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJ_FILE);
          }
        } catch (PartInitException e) {

        }
      }

      return null;
    }

    @Override
    public String getLabel(Object object) {
      if (object instanceof IFileStore) {
        IFileStore fileStore = (IFileStore) object;
        return fileStore.getName();
      } else {
        return null;
      }
    }

    @Override
    public Object getParent(Object object) {
      if (object instanceof IFileStore) {
        IFileStore fileStore = (IFileStore) object;
        return fileStore.getParent();
      } else {
        return null;
      }
    }
  }

  private static IWorkbenchAdapter workbenchAdapter = new FileStoreWorkbenchAdapter();

  public FileStoreAdapterFactory() {

  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (IWorkbenchAdapter.class == adapterType) {
      return workbenchAdapter;
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class[] getAdapterList() {
    return new Class[] {IWorkbenchAdapter.class};
  }

}
