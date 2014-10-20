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

package com.google.dart.tools.ui.internal.filesview.nodes.old.sdk;

import com.google.dart.engine.sdk.SdkLibrary;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.old.IDartNode_OLD;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class used to represent the Dart SDK in the Files view.
 */
class NewDartSdkNode_OLD extends DartSdkNode_OLD {

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
    Platform.getAdapterManager().registerAdapters(
        new SdkDirectoryWorkbenchAdapter(),
        IDartNode_OLD.class);
  }

  private DartLibraryNode_OLD[] libraries;

  public NewDartSdkNode_OLD() {
    libraries = getLibraries();
  }

  @Override
  public IFileStore getFileStore() {
    if (DartSdkManager.getManager().hasSdk()) {
      File sdkLibDir = DartSdkManager.getManager().getSdk().getLibraryDirectory();
      return EFS.getLocalFileSystem().fromLocalFile(sdkLibDir);
    }
    return null;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.getImageDescriptor("icons/full/dart16/sdk.png"); //$NON-NLS-1$
  }

  @Override
  public String getLabel() {
    return "Dart SDK"; //$NON-NLS-1$
  }

  @Override
  public DartLibraryNode_OLD[] getLibraries() {
    if (libraries == null) {
      List<DartLibraryNode_OLD> libs = new ArrayList<DartLibraryNode_OLD>();

      File libFile;

      if (!DartSdkManager.getManager().hasSdk()) {
        libFile = null;
      } else {
        SdkLibrary[] systemLibraries = DartSdkManager.getManager().getSdk().getSdkLibraries();
        //TODO (pquitslund): fix how we're getting the SDK directory
        File sdkDirectory = DartSdkManager.getManager().getSdk().getDirectory();
        IFileSystem fileSystem = EFS.getLocalFileSystem();
        for (SdkLibrary systemLibrary : systemLibraries) {
          if (systemLibrary.isDocumented()) {
            libFile = new File(sdkDirectory, "lib"); //$NON-NLS-1$
            String pathToLib = systemLibrary.getPath();
            if (pathToLib.indexOf("/") != -1) { //$NON-NLS-1$
              libFile = new File(libFile, new Path(pathToLib).removeLastSegments(1).toOSString());
            }
            if (!systemLibrary.isShared()) {
              libs.add(new DartLibraryNode_OLD(
                  this,
                  fileSystem.fromLocalFile(libFile),
                  systemLibrary.getShortName(),
                  systemLibrary.getCategory().toLowerCase()));
            } else {
              libs.add(new DartLibraryNode_OLD(
                  this,
                  fileSystem.fromLocalFile(libFile),
                  systemLibrary.getShortName()));
            }

          }
        }

      }

      libraries = libs.toArray(new DartLibraryNode_OLD[libs.size()]);
    }
    return libraries;
  }

  @Override
  public IDartNode_OLD getParent() {
    return null;
  }

  @Override
  public String toString() {
    return getLabel();
  }
}
