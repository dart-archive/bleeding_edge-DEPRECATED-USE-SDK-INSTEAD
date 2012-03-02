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

package com.google.dart.tools.ui.internal.view.files;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.io.File;

/**
 * Adapter for java io file.
 */
public class FileAdapterFactory implements IAdapterFactory {

  static class FileWorkbenchAdapter implements IWorkbenchAdapter {
    private static final IResource[] NO_CHILDREN = new IResource[0];
    private static final DartElementImageProvider dartElementImageProvider = new DartElementImageProvider();

    @Override
    public Object[] getChildren(Object object) {
      if (object instanceof File) {
        File file = (File) object;
        if (file.isDirectory()) {
          return file.listFiles();
        }

      }
      return NO_CHILDREN;
    }

    @Override
    public ImageDescriptor getImageDescriptor(Object object) {
      if (object instanceof File) {
        File file = (File) object;

        try {
          if (file.isDirectory()) {
            return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJ_FOLDER);
          }

          IFile resource = ResourceUtil.getFile(file);
          if (resource != null) {
            return dartElementImageProvider.getBaseImageDescriptor(DartCore.create(resource),
                DartElementImageProvider.OVERLAY_ICONS);
          }

          IEditorDescriptor descriptor = IDE.getEditorDescriptor(file.getName());

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
      if (object instanceof File) {
        return ((File) object).getName();
      } else {
        return null;
      }
    }

    @Override
    public Object getParent(Object object) {
      if (object instanceof File) {
        return ((File) object).getParentFile();
      } else {
        return null;
      }
    }
  }

  private static IWorkbenchAdapter workbenchAdapter = new FileWorkbenchAdapter();

  public FileAdapterFactory() {

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
