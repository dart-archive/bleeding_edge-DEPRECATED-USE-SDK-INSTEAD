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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class AppLabelProvider implements IStyledLabelProvider, ILabelProvider {
  private static final String LIBRARY_ICON = "icons/full/dart16/dart_library.png"; //$NON-NLS-1$
  private static final String APP_ICON = "icons/full/dart16/app_icon.png"; //$NON-NLS-1$
  private static final String IGNORE_FILE_ICON = "icons/full/dart16/dart_excl.png"; //$NON-NLS-1$

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
      ElementTreeNode treeNode = (ElementTreeNode) element;
      if (treeNode.isApp()) {
        return DartToolsPlugin.getImage(APP_ICON);
      }
      element = treeNode.getModelElement();
    }
    // Return a different icon for library units.
    if (element instanceof CompilationUnit) {
      CompilationUnit cu = (CompilationUnit) element;
      IResource res = cu.getResource();
      if (res != null && !DartCore.isAnalyzed(res)) {
        if (res instanceof IFile) {
          return DartToolsPlugin.getImage(IGNORE_FILE_ICON);
        }
      }
      if (cu.definesLibrary()) {
        return DartToolsPlugin.getImage(LIBRARY_ICON);
      }
    }
    return workbenchLabelProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    if (element instanceof ElementTreeNode) {
      element = ((ElementTreeNode) element).getModelElement();
    }
    if (element instanceof DartProject) {
      return new StyledString(((DartProject) element).getElementName());
    } else if (element instanceof DartLibrary) {
      DartLibrary lib = (DartLibrary) element;
      Styler style;
      try {
        IResource res = lib.getCorrespondingResource();
        style = res == null || DartCore.isAnalyzed(res) ? null : StyledString.QUALIFIER_STYLER;
      } catch (DartModelException es) {
        style = null;
      }
      return new StyledString(lib.getDisplayName(), style);
    } else if (element instanceof CompilationUnit) {
      CompilationUnit cu = (CompilationUnit) element;
      IResource res = cu.getResource();
      Styler style = res == null || DartCore.isAnalyzed(res) ? null : StyledString.QUALIFIER_STYLER;
      StyledString name = new StyledString(cu.getElementName(), style);
      if (cu.definesLibrary()) {
        name = name.append(" - " + cu.getLibrary().getDisplayName(), StyledString.QUALIFIER_STYLER);
      }
      return name;
    }
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
      CompilationUnit cu = (CompilationUnit) element;
      String name = cu.getElementName();
      if (cu.definesLibrary()) {
        name = name + " - " + cu.getLibrary().getDisplayName();
      }
      return name;
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
