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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

/**
 * CNF navigator content provider for dart elements.
 */
public class DartNavigatorContentProvider implements ICommonContentProvider {

  private static Object[] NONE = new Object[0];

  @Override
  public void dispose() {
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    return NONE;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  @Override
  public Object getParent(Object element) {
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void init(ICommonContentExtensionSite aConfig) {
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

  @Override
  public void restoreState(IMemento aMemento) {
  }

  @Override
  public void saveState(IMemento aMemento) {
  }

}
