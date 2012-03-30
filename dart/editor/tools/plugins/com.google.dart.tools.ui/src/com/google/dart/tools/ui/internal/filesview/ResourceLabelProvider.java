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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.preferences.DartBasePreferencePage;
import com.google.dart.tools.ui.internal.util.SWTUtil;
import com.google.dart.tools.ui.themes.Fonts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for resources in the {@link FilesView}.
 */
public class ResourceLabelProvider implements IStyledLabelProvider, ILabelProvider {

  private static final String IGNORE_FILE_ICON = "icons/full/dart16/dart_excl.png";
  private static final String IGNORE_FOLDER_ICON = "icons/full/dart16/flder_obj_excl.png";

  private final WorkbenchLabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();
  
  private Font boldFont;
  private Styler boldStyler;

  /**
   * Create a provider instance.
   * 
   * @param font the font that the receiving control will use to present text
   */
  ResourceLabelProvider(Font font) {

    boldFont = Fonts.getBoldFont(font);
    boldStyler = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
        textStyle.font = boldFont;
      }
    };
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    workbenchLabelProvider.addListener(listener);
  }

  @Override
  public void dispose() {
    workbenchLabelProvider.dispose();
    boldFont.dispose();
  }

  @Override
  public Image getImage(Object element) {

    if (element instanceof IResource) {
      IResource resource = (IResource) element;
      if (!DartCore.isAnalyzed(resource)) {
        if (resource instanceof IFile) {
          return DartToolsPlugin.getImage(IGNORE_FILE_ICON);
        }
        if (resource instanceof IFolder) {
          return DartToolsPlugin.getImage(IGNORE_FOLDER_ICON);
        }
      }
    }

    return workbenchLabelProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {

    if (element instanceof IResource) {
      IResource resource = (IResource) element;
      //un-analyzed resources are grey
      if (!DartCore.isAnalyzed(resource)) {
        return new StyledString(resource.getName(), StyledString.QUALIFIER_STYLER);
      }

      //resources defining libraries are bold
      DartElement dartElement = DartCore.create(resource);
      if (dartElement instanceof CompilationUnit) {
        if (((CompilationUnit) dartElement).definesLibrary()) {
          return new StyledString(resource.getName(), boldStyler);
        }
      }
    }

    return workbenchLabelProvider.getStyledText(element);
  }

  @Override
  public String getText(Object element) {
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
    Font newFont = JFaceResources.getFont(DartBasePreferencePage.BASE_FONT_KEY);
    boldFont = SWTUtil.changeFontSize(boldFont, newFont);
  }

}
