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
package com.google.dart.tools.ui.internal.filesview.nodes.server.pkgs;

import com.google.common.base.Objects;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.server.IDartNode_NEW;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;

/**
 * A {@link IDartNode_NEW} representing a pub package in the pub cache.
 */
public class DartPackageNode_NEW implements IDartNode_NEW {
  private static final ImageDescriptor LINK_OVERLAY = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/link_ovr.gif");
  private static final ImageDescriptor FOLDER_BASE = DartToolsPlugin.getImageDescriptor("icons/full/obj16/fldr_obj.gif");
  private static final ImageDescriptor ICON = new DecorationOverlayIcon(
      FOLDER_BASE.createImage(),
      LINK_OVERLAY,
      IDecoration.BOTTOM_LEFT);

  private final InstalledPackagesNode_NEW parent;
  private final String name;
  private final String version;
  private final IProject project;

  public DartPackageNode_NEW(InstalledPackagesNode_NEW parent, String name, String version,
      IProject project) {
    this.parent = parent;
    this.name = name;
    this.version = version;
    this.project = project;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DartPackageNode_NEW) {
      DartPackageNode_NEW other = (DartPackageNode_NEW) obj;
      return Objects.equal(other.name, name) && Objects.equal(other.version, version);
    }
    return false;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return ICON;
  }

  @Override
  public String getLabel() {
    return name;
  }

  @Override
  public Object getParent() {
    return parent;
  }

  public IProject getProject() {
    return project;
  }

  @Override
  public IResource getResource() {
    return project;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, version);
  }

  @Override
  public String toString() {
    return name + " " + version + " @ " + project;
  }
}
