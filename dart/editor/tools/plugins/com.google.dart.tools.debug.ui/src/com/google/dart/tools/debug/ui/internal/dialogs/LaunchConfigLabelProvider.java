/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.dialogs;

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for the manage launches dialog.
 * 
 * @see ManageLaunchesDialog
 */
class LaunchConfigLabelProvider extends LabelProvider implements
    DelegatingStyledCellLabelProvider.IStyledLabelProvider {

  private ILabelProvider delegateProvider;

  public LaunchConfigLabelProvider() {
    delegateProvider = DebugUITools.newDebugModelPresentation();
  }

  @Override
  public Image getImage(Object element) {
    return delegateProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    StyledString str = new StyledString();

    str.append(getText(element));

    if (element instanceof ILaunchConfiguration) {
      ILaunchConfiguration config = (ILaunchConfiguration) element;

      String appendText = getAppendText(config);

      if (appendText != null) {
        str.append(appendText, StyledString.QUALIFIER_STYLER);
      }
    }

    return str;
  }

  @Override
  public String getText(Object element) {
    if (element instanceof ILaunchConfiguration) {
      ILaunchConfiguration config = (ILaunchConfiguration) element;

      return config.getName();
    } else {
      return delegateProvider.getText(element);
    }
  }

  private String getAppendText(ILaunchConfiguration config) {
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(config);

    if (wrapper.getShouldLaunchFile()) {
      IResource resource = wrapper.getApplicationResource();

      if (resource != null) {
        return " - " + resource.getFullPath();
      }
    } else {
      String url = wrapper.getUrl();

      if (url != null && url.length() > 0) {
        return " - " + url;
      }
    }

    return null;
  }

}
