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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;

import java.util.Arrays;
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

  private List<DartDirectoryNode> children;

  public DartSdkNode() {
    children = Arrays.asList(
        DartDirectoryNode.createLibNode(this),
        DartDirectoryNode.createPkgNode(this));
  }

  public DartDirectoryNode[] getChildDirectories() {
    return children.toArray(new DartDirectoryNode[children.size()]);
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return DartToolsPlugin.getImageDescriptor("icons/full/dart16/sdk.png");
  }

  @Override
  public String getLabel() {
    return "Dart SDK";
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
