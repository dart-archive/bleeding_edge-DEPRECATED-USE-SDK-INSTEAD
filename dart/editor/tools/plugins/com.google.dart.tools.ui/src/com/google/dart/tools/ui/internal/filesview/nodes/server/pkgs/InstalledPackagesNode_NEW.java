/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.core.pub.PubCacheManager_NEW;
import com.google.dart.tools.core.pub.PubCacheManager_NEW.PackageInfo;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.server.IDartNode_NEW;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Represents the "Referenced Packages" node in the "Files" view.
 */
public class InstalledPackagesNode_NEW implements IDartNode_NEW {

  public static InstalledPackagesNode_NEW createInstance() {
    return new InstalledPackagesNode_NEW();
  }

  public InstalledPackagesNode_NEW() {
    PubCacheManager_NEW.getInstance();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.getImageDescriptor("icons/full/dart16/sdk.png"); //$NON-NLS-1$
  }

  @Override
  public String getLabel() {
    return "Referenced Packages";
  }

  public DartPackageNode_NEW getPackage(IProject project) {
    if (PubCacheManager_NEW.isPubCacheProject(project)) {
      PackageInfo[] infos = PubCacheManager_NEW.getInstance().getPackages();
      for (PackageInfo info : infos) {
        if (info.getProject() == project) {
          return new DartPackageNode_NEW(this, info.name, info.version, info.getProject());
        }
      }
    }
    return null;
  }

  public DartPackageNode_NEW[] getPackages() {
    PackageInfo[] infos = PubCacheManager_NEW.getInstance().getPackages();
    DartPackageNode_NEW[] nodes = new DartPackageNode_NEW[infos.length];
    for (int i = 0; i < infos.length; i++) {
      PackageInfo info = infos[i];
      nodes[i] = new DartPackageNode_NEW(this, info.name, info.version, info.getProject());
    }
    return nodes;
  }

  @Override
  public Object getParent() {
    return null;
  }

  @Override
  public IResource getResource() {
    return null;
  }

  @Override
  public String toString() {
    return getLabel();
  }
}
