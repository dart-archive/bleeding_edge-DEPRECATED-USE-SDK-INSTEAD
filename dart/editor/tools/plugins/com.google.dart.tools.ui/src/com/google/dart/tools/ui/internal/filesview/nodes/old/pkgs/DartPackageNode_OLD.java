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
package com.google.dart.tools.ui.internal.filesview.nodes.old.pkgs;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.old.IDartNode_OLD;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pub package in the pub cache and is shown in the Installed Packages
 */
public class DartPackageNode_OLD implements IDartNode_OLD {

  private InstalledPackagesNode_OLD parent;
  private String name;
  private String version;
  private String location;
  private IFileStore root;

  public DartPackageNode_OLD(InstalledPackagesNode_OLD parent, String name, String version, String location) {
    this.parent = parent;
    this.name = name;
    this.version = version;
    this.location = location;
  }

  public IFileStore[] getFiles() {
    try {
      if (root == null) {
        initRoot();
      }
      List<IFileStore> members = filteredMembers(root);

      return members.toArray(new IFileStore[members.size()]);
    } catch (CoreException e) {
      return new IFileStore[0];
    }
  }

  @Override
  public IFileStore getFileStore() {
    if (root == null) {
      initRoot();
    }
    return root;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    ImageDescriptor overlay = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/link_ovr.gif"); //$NON-NLS-1$
    ImageDescriptor baseImage = DartToolsPlugin.getImageDescriptor("icons/full/obj16/fldr_obj.gif"); //$NON-NLS-1$

    DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(
        baseImage.createImage(),
        overlay,
        IDecoration.BOTTOM_LEFT);
    return overlayIcon;
  }

  @Override
  public String getLabel() {
    return name;
  }

  @Override
  public Object getParent() {
    return parent;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return name;
  }

  private List<IFileStore> filteredMembers(IFileStore file) throws CoreException {
    List<IFileStore> children = new ArrayList<IFileStore>();

    for (IFileStore child : file.childStores(EFS.NONE, new NullProgressMonitor())) {
      String name = child.getName();
      if (!name.startsWith(".")) {
        children.add(child);
      }
    }
    return children;
  }

  private void initRoot() {
    IFileSystem fileSystem = EFS.getLocalFileSystem();
    File file = new File(location);
    root = fileSystem.fromLocalFile(file);
  }

}
