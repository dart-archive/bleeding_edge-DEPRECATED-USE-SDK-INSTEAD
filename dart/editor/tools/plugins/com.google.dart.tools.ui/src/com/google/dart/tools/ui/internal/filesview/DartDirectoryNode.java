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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A directory in the SDK (lib or pkg).
 */
public class DartDirectoryNode implements IDartNode {

  protected static DartDirectoryNode createLibNode(DartSdkNode sdk) {
    return new DartDirectoryNode(sdk, true);
  }

  protected static DartDirectoryNode createPkgNode(DartSdkNode sdk) {
    return new DartDirectoryNode(sdk, false);
  }

  private DartSdkNode parent;
  private boolean isLibDir;
  private DartLibraryNode[] libraries;

  private DartDirectoryNode(DartSdkNode sdkNode, boolean isLibDir) {
    this.parent = sdkNode;
    this.isLibDir = isLibDir;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.getImageDescriptor("icons/full/dart16/fldr_obj_pkg.png");
  }

  @Override
  public String getLabel() {
    return isLibDir ? "lib" : "pkg";
  }

  public DartLibraryNode[] getLibraries() {
    if (libraries == null) {
      List<DartLibraryNode> libs = new ArrayList<DartLibraryNode>();

      File file;

      if (!DartSdkManager.getManager().hasSdk()) {
        file = null;
      } else if (isLibDir) {
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

      } else {
        file = DartSdkManager.getManager().getSdk().getPackageDirectory();

        if (file != null) {
          for (File child : file.listFiles()) {
            if (child.isDirectory()) {
              libs.add(new DartLibraryNode(
                  this,
                  EFS.getLocalFileSystem().fromLocalFile(child),
                  child.getName()));
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
    return parent;
  }

  @Override
  public String toString() {
    return getLabel();
  }
}
