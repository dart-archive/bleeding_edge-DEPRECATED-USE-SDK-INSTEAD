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

import com.google.dart.compiler.SystemLibrary;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class used to represent the Dart SDK in the Files view.
 */
class DartSdkNode implements IDartNode {

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
      return ((IDartNode) object).getImageDescriptor();
    }

    @Override
    public String getLabel(Object object) {
      return ((IDartNode) object).getLabel();
    }
  }

  static {
    Platform.getAdapterManager().registerAdapters(
        new SdkDirectoryWorkbenchAdapter(),
        IDartNode.class);
  }

  private DartLibraryNode[] libraries;

  public DartSdkNode() {
    libraries = getLibraries();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.getImageDescriptor("icons/full/dart16/sdk.png");
  }

  @Override
  public String getLabel() {
    return "Dart SDK";
  }

  public DartLibraryNode[] getLibraries() {
    if (libraries == null) {
      List<DartLibraryNode> libs = new ArrayList<DartLibraryNode>();

      File file;

      if (!DartSdkManager.getManager().hasSdk()) {
        file = null;
      } else {
        Collection<SystemLibrary> systemLibraries = PackageLibraryManagerProvider.getAnyLibraryManager().getSystemLibraries();
        for (SystemLibrary systemLibrary : systemLibraries) {
          if (systemLibrary.isDocumented()) {
            file = systemLibrary.getLibraryDir();
            String pathToLib = systemLibrary.getPathToLib();
            if (pathToLib.indexOf("/") != -1) {
              file = new File(file, new Path(pathToLib).removeLastSegments(1).toOSString());
            }
            if (!systemLibrary.isShared()) {
              libs.add(new DartLibraryNode(
                  this,
                  EFS.getLocalFileSystem().fromLocalFile(file),
                  systemLibrary.getShortName(),
                  systemLibrary.getCategory().toLowerCase()));
            } else {
              libs.add(new DartLibraryNode(
                  this,
                  EFS.getLocalFileSystem().fromLocalFile(file),
                  systemLibrary.getShortName()));
            }

          }
        }

      }

      libraries = libs.toArray(new DartLibraryNode[libs.size()]);
    }
    return libraries;
  }

  @Override
  public IDartNode getParent() {
    return null;
  }

  @Override
  public String toString() {
    return getLabel();
  }

}
