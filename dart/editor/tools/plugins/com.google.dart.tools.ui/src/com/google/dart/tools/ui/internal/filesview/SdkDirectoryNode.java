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

package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class used to represent the SDK directory.
 */
class SdkDirectoryNode {

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
      return DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_container.png");
    }

    @Override
    public String getLabel(Object object) {
      return ((SdkDirectoryNode) object).toString();
    }
  }

  public static final SdkDirectoryNode INSTANCE = new SdkDirectoryNode();

  static {
    Platform.getAdapterManager().registerAdapters(
        new SdkDirectoryWorkbenchAdapter(),
        SdkDirectoryNode.class);
  }

  private SdkLibraryNode[] libraries;

  public SdkLibraryNode[] getLibraries() {
    if (libraries == null) {
      List<SdkLibraryNode> libs = new ArrayList<SdkLibraryNode>();

      File file = PackageLibraryManagerProvider.getPackageLibraryManager().getSdkLibPath();

      for (File child : file.listFiles()) {
        if (child.isDirectory()) {
          // Skip the config directory - it is not a Dart library.
          // TODO(devoncarew): will config be going away?
          if (child.getName().equals("config")) {
            continue;
          }

          // Skip the _internal directory (and any other similar private libraries).
          if (child.getName().startsWith("_")) {
            continue;
          }

          libs.add(new SdkLibraryNode(EFS.getLocalFileSystem().fromLocalFile(child)));
        }
      }

      libraries = libs.toArray(new SdkLibraryNode[libs.size()]);
    }

    return libraries;
  }

  @Override
  public String toString() {
    return "SDK Libraries";
  }

}
