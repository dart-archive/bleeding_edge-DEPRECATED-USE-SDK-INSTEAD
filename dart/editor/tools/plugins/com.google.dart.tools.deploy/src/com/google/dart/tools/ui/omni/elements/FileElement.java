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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.omni.OmniBoxImages;
import com.google.dart.tools.ui.omni.OmniElement;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Element for files.
 */
public class FileElement extends OmniElement {

  /**
   * The associated file.
   */
  private final IFile file;

  public FileElement(FileProvider provider, IFile resource) {
    super(provider);
    this.file = resource;
  }

  @Override
  public void execute(String text) {
    try {
      final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window == null) {
        throw new ExecutionException("no active workbench window"); //$NON-NLS-1$
      }
      final IWorkbenchPage page = window.getActivePage();
      if (page == null) {
        throw new ExecutionException("no active workbench page"); //$NON-NLS-1$
      }
      IDE.openEditor(page, file);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    } catch (ExecutionException e) {
      DartToolsPlugin.log(e);
    }
  }

  @Override
  public String getDetailedLabel() {
    StringBuffer result = new StringBuffer();
    result.append(file.getName());

    //only cache for duplicates
    if (isDuplicate()) {
      detailOffset = result.length();
    }

    IContainer container = file.getParent();
    if (container != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(container.getName());
    }
    return result.toString();
  }

  @Override
  public String getId() {
    return file.getName();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return OmniBoxImages.getFileImageDescriptor(file);
  }

  @Override
  public String getLabel() {
    return isDuplicate() ? getDetailedLabel() : getBasicLabel();
  }

  @Override
  public FileProvider getProvider() {
    return (FileProvider) super.getProvider();
  }

  private String getBasicLabel() {
    StringBuffer result = new StringBuffer();
    result.append(file.getName());
    return result.toString();
  }

}
