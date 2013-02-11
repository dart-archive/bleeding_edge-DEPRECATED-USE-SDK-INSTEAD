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
package com.google.dart.eclipse.ui.internal.navigator;

import com.google.dart.tools.ui.internal.filesview.ResourceLabelProvider;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * CNF navigator label provider for dart elements.
 */
public class DartNavigatorLabelProvider extends LabelProvider implements ICommonLabelProvider,
    IStyledLabelProvider {

  private final ResourceLabelProvider resourceLabelProvider = ResourceLabelProvider.createInstance();

  @Override
  public void addListener(ILabelProviderListener listener) {
    resourceLabelProvider.addListener(listener);
  }

  @Override
  public void dispose() {
    resourceLabelProvider.dispose();
  }

  @Override
  public String getDescription(Object elem) {
    //default
    return null;
  }

  @Override
  public final Image getImage(Object element) {
    return resourceLabelProvider.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    return resourceLabelProvider.getStyledText(element);
  }

  @Override
  public String getText(Object element) {
    return resourceLabelProvider.getText(element);
  }

  @Override
  public void init(ICommonContentExtensionSite config) {
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return resourceLabelProvider.isLabelProperty(element, property);
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    resourceLabelProvider.removeListener(listener);
  }

  @Override
  public void restoreState(IMemento memento) {
  }

  @Override
  public void saveState(IMemento memento) {
  }

}
