/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.ui.omni.OmniElement;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.IViewCategory;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * Element for views.
 */
public class ViewElement extends OmniElement {

  private final IViewDescriptor viewDescriptor;
  private String secondaryId;
  private boolean multiInstance;
  private String contentDescription;

  private String category;

  /* package */ViewElement(IViewDescriptor viewDescriptor, ViewProvider viewProvider) {
    super(viewProvider);
    this.viewDescriptor = viewDescriptor;

    IViewCategory[] categories = PlatformUI.getWorkbench().getViewRegistry().getCategories();
    for (int i = 0; i < categories.length; i++) {
      IViewDescriptor[] views = categories[i].getViews();
      for (int j = 0; j < views.length; j++) {
        if (views[j] == viewDescriptor) {
          category = categories[i].getLabel();
          return;
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ViewElement other = (ViewElement) obj;
    if (secondaryId == null) {
      if (other.secondaryId != null) {
        return false;
      }
    } else if (!secondaryId.equals(other.secondaryId)) {
      return false;
    }
    if (viewDescriptor == null) {
      if (other.viewDescriptor != null) {
        return false;
      }
    } else if (!viewDescriptor.equals(other.viewDescriptor)) {
      return false;
    }
    return true;
  }

  @Override
  public void execute(String text) {
    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    if (activePage != null) {
      try {
        activePage.showView(viewDescriptor.getId(), secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
      } catch (PartInitException e) {
      }
    }
  }

  @Override
  public String getId() {
    if (secondaryId == null) {
      return viewDescriptor.getId();
    }
    return viewDescriptor.getId() + ':' + secondaryId;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return viewDescriptor.getImageDescriptor();
  }

  @Override
  public String getLabel() {
    String label = viewDescriptor.getLabel();

    if (isMultiInstance() && contentDescription != null) {
      label = label + " (" + contentDescription + ')'; //$NON-NLS-1$
    }

    if (category != null) {
      label = label + separator + category;
    }
    return label;
  }

  /**
   * @return The primary id of the view
   */
  public String getPrimaryId() {

    return viewDescriptor.getId();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((secondaryId == null) ? 0 : secondaryId.hashCode());
    result = prime * result + ((viewDescriptor == null) ? 0 : viewDescriptor.hashCode());
    return result;
  }

  /**
   * @return Returns the multiInstance.
   */
  public boolean isMultiInstance() {
    return multiInstance;
  }

  /**
   * @param contentDescription The contentDescription to set.
   */
  public void setContentDescription(String contentDescription) {
    this.contentDescription = contentDescription;
  }

  /**
   * @param multiInstance The multiInstance to set.
   */
  public void setMultiInstance(boolean multiInstance) {
    this.multiInstance = multiInstance;
  }

  /**
   * @param secondaryId The secondaryId to set.
   */
  public void setSecondaryId(String secondaryId) {
    this.secondaryId = secondaryId;
  }

}
