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
package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

/**
 * A dialog to choose a main launch configuration target.
 */
public class AppSelectionDialog extends FilteredResourcesSelectionDialog {
  public static class HtmlResourceFilter implements IResourceFilter {
    @Override
    public boolean matches(IResource resource) {
      return DartCore.isHTMLLikeFileName(resource.getName());
    }
  }

  private IResourceFilter filter;

  /**
   * Create a new AppSelectionDialog.
   * 
   * @param shell
   * @param container
   */
  public AppSelectionDialog(Shell shell, IContainer container, IResourceFilter filter) {
    super(shell, false, container, IResource.FILE);

    this.filter = filter;
  }

  @Override
  protected ItemsFilter createFilter() {
    final ItemsFilter delegateFilter = super.createFilter();

    return new ResourceFilter() {
      @Override
      public boolean isConsistentItem(Object item) {
        return delegateFilter.isConsistentItem(item);
      }

      @Override
      public boolean matchItem(Object item) {
        if (!(item instanceof IResource)) {
          return false;
        }

        IResource resource = (IResource) item;

        if (!delegateFilter.matchItem(resource)) {
          return false;
        }

        return filter.matches(resource);
      }
    };
  }

}
