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
package com.google.dart.tools.ui.internal.appsview;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class AppLabelProvider implements IStyledLabelProvider, ILabelProvider {

  public WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

  AppLabelProvider(Font font) {
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    workbenchLabelProvider.addListener(listener);
  }

  @Override
  public void dispose() {
    workbenchLabelProvider.dispose();
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof ElementTreeNode) {
      element = ((ElementTreeNode) element).getModelElement();
    }
    return workbenchLabelProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    return workbenchLabelProvider.getStyledText(element);
  }

  @Override
  public String getText(Object element) {
    if (element instanceof ElementTreeNode) {
      element = ((ElementTreeNode) element).getModelElement();
    }
    if (element instanceof DartProject) {
      return ((DartProject) element).getElementName();
    } else if (element instanceof DartLibrary) {
      return ((DartLibrary) element).getDisplayName();
    } else if (element instanceof CompilationUnit) {
      return ((CompilationUnit) element).getElementName();
    }
    return workbenchLabelProvider.getText(element);
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return workbenchLabelProvider.isLabelProperty(element, property);
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    workbenchLabelProvider.removeListener(listener);
  }

  void updateFont(Font font) {
  }

}
