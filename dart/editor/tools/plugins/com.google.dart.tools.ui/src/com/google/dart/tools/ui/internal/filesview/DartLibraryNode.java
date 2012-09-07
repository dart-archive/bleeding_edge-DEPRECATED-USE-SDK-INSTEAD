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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of a sdk library (e.g. dart:core, dart:io, package:logging/logging.dart, ...).
 */
class DartLibraryNode implements IDartNode {
  private DartDirectoryNode parent;
  private IFileStore root;
  private String name;

  public DartLibraryNode(DartDirectoryNode parent, IFileStore root, String name) {
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
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.findImageDescriptor("icons/full/obj16/package_obj.gif");
  }

  @Override
  public String getLabel() {
    if (isPkgNode()) {
      return "package:" + root.getName() + "/" + root.getName() + ".dart";
    } else {
      return "dart:" + name;
    }
  }

  @Override
  public IDartNode getParent() {
    return parent;
  }

  @Override
  public String toString() {
    return getLabel();
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

  private boolean isPkgNode() {
    return root.getParent().getName().equals("pkg");
  }

}
