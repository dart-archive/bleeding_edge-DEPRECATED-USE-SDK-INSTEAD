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

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * CNF navigator label provider for dart elements.
 */
public class DartNavigatorLabelProvider extends WorkbenchLabelProvider implements
    ICommonLabelProvider, IStyledLabelProvider {

  @Override
  public String getDescription(Object elem) {
    //default
    return null;
  }

  @Override
  public StyledString getStyledText(Object element) {
    //TODO(pquitslund): add "ignored" resource styling
    return super.getStyledText(element);
  }

  @Override
  public void init(ICommonContentExtensionSite config) {
  }

  @Override
  public void restoreState(IMemento aMemento) {
  }

  @Override
  public void saveState(IMemento aMemento) {
  }

}
