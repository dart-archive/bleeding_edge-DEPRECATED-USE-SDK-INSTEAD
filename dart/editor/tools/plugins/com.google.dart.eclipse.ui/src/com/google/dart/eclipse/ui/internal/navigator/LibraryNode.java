/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.eclipse.ui.internal.navigator;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.old.IDartNode_OLD;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a library node in the project explorer
 */
public class LibraryNode implements IDartNode_OLD {

  static class SdkDirectoryWorkbenchAdapter extends WorkbenchAdapter implements IAdapterFactory {
    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
      if (adapterType == IWorkbenchAdapter.class) {
        return this;
      } else {
        return null;
      }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getAdapterList() {
      return new Class[] {IWorkbenchAdapter.class};
    }

    @Override
    public ImageDescriptor getImageDescriptor(Object object) {
      return ((IDartNode_OLD) object).getImageDescriptor();
    }

    @Override
    public String getLabel(Object object) {
      return ((IDartNode_OLD) object).getLabel();
    }
  }

  static {
    Platform.getAdapterManager().registerAdapters(new SdkDirectoryWorkbenchAdapter(),
        IDartNode_OLD.class);
  }

  private Object parent;
  private IFileStore root;
  private String name;

  public LibraryNode(Object parent, IFileStore root, String name) {
    this.parent = parent;
    this.root = root;
    this.name = name;
  }

  public IFileStore[] getFiles() {
    try {
      List<IFileStore> members = filteredMembers(root);

      return members.toArray(new IFileStore[members.size()]);
    } catch (CoreException e) {
      return new IFileStore[0];
    }
  }

  @Override
  public IFileStore getFileStore() {

    return root;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.findImageDescriptor("icons/full/obj16/package_obj.gif");
  }

  @Override
  public String getLabel() {
    return name;
  }

  @Override
  public Object getParent() {
    return parent;
  }

  private List<IFileStore> filteredMembers(IFileStore file) throws CoreException {
    List<IFileStore> children = new ArrayList<IFileStore>();

    for (IFileStore child : file.childStores(EFS.NONE, new NullProgressMonitor())) {
      String name = child.getName();
      if (!(name.startsWith("."))) {
        children.add(child);
      }
    }

    return children;
  }

}
