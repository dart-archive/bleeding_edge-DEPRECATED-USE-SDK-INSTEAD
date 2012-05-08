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
package com.google.dart.tools.search.internal.ui;

import com.google.dart.tools.search.internal.ui.util.ExceptionHandler;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Proxy that represents a sorter.
 */
class SorterDescriptor {

  public final static String SORTER_TAG = "sorter"; //$NON-NLS-1$
  private final static String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
  private final static String PAGE_ID_ATTRIBUTE = "pageId"; //$NON-NLS-1$
  private final static String ICON_ATTRIBUTE = "icon"; //$NON-NLS-1$
  private final static String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$
  private final static String LABEL_ATTRIBUTE = "label"; //$NON-NLS-1$
  private final static String TOOLTIP_ATTRIBUTE = "tooltip"; //$NON-NLS-1$

  private IConfigurationElement fElement;

  /**
   * Creates a new sorter node with the given configuration element.
   * 
   * @param element the configuration element
   */
  public SorterDescriptor(IConfigurationElement element) {
    fElement = element;
  }

  /**
   * Creates a new sorter from this node.
   * 
   * @return new sorter
   */
  public ViewerSorter createObject() {
    try {
      return (ViewerSorter) fElement.createExecutableExtension(CLASS_ATTRIBUTE);
    } catch (CoreException ex) {
      ExceptionHandler.handle(
          ex,
          SearchMessages.Search_Error_createSorter_title,
          SearchMessages.Search_Error_createSorter_message);
      return null;
    } catch (ClassCastException ex) {
      ExceptionHandler.displayMessageDialog(
          ex,
          SearchMessages.Search_Error_createSorter_title,
          SearchMessages.Search_Error_createSorter_message);
      return null;
    }
  }

  //---- XML Attribute accessors ---------------------------------------------

  /**
   * Returns the sorter's id.
   * 
   * @return the sorter's id.
   */
  public String getId() {
    return fElement.getAttribute(ID_ATTRIBUTE);
  }

  /**
   * Returns the sorter's image
   * 
   * @return the sorter's image
   */
  public ImageDescriptor getImage() {
    String imageName = fElement.getAttribute(ICON_ATTRIBUTE);
    if (imageName == null)
      return null;
    Bundle bundle = Platform.getBundle(fElement.getContributor().getName());
    return SearchPluginImages.createImageDescriptor(bundle, new Path(imageName), true);
  }

  /**
   * Returns the sorter's label.
   * 
   * @return the sorter's label.
   */
  public String getLabel() {
    return fElement.getAttribute(LABEL_ATTRIBUTE);
  }

  /**
   * Returns the sorter's tooltip.
   * 
   * @return the sorter's tooltip.
   */
  public String getToolTipText() {
    return fElement.getAttribute(TOOLTIP_ATTRIBUTE);
  }

  /**
   * Returns the sorter's page id
   * 
   * @return the page id
   */
  public String getPageId() {
    return fElement.getAttribute(PAGE_ID_ATTRIBUTE);
  }
}
