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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.pub.PubCacheManager;
import com.google.dart.tools.core.pub.PubspecConstants;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.resource.ImageDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the Installed Packages Node in the Files View.
 */
public class InstalledPackagesNode implements IDartNode {

  public static InstalledPackagesNode createInstance() {
    return new InstalledPackagesNode();
  }

  private DartPackageNode[] packages;
  private HashMap<String, Object> localPackages;

  public InstalledPackagesNode() {

    this.localPackages = PubCacheManager.getInstance().getLocalPackages();
    if (localPackages.isEmpty()) {
      PubCacheManager.getInstance().updatePackagesList(2000);
    } else {
      packages = getPackages();
    }
  }

  @Override
  public IFileStore getFileStore() {
    return null;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.getImageDescriptor("icons/full/dart16/sdk.png"); //$NON-NLS-1$
  }

  @Override
  public String getLabel() {
    return "Installed Packages";
  }

  @SuppressWarnings("unchecked")
  public DartPackageNode[] getPackages() {
    List<DartPackageNode> nodes = new ArrayList<DartPackageNode>();
    if (localPackages != null && !localPackages.isEmpty()) {
      for (String packageName : localPackages.keySet()) {
        Map<String, Object> map = (Map<String, Object>) localPackages.get(packageName);
        if (map != null) {
          String location = (String) map.get(PubspecConstants.LOCATION);
          String version = (String) map.get(PubspecConstants.VERSION);
          if (location != null && version != null) {
            nodes.add(new DartPackageNode(this, packageName, version, location));
          }
        }
      }
    }
    this.packages = nodes.toArray(new DartPackageNode[nodes.size()]);
    return this.packages;
  }

  @Override
  public Object getParent() {
    return null;
  }

  @Override
  public String toString() {
    return getLabel();
  }

  public void updatePackages(Map<String, Object> added) {
    localPackages.putAll(added);
  }

}
