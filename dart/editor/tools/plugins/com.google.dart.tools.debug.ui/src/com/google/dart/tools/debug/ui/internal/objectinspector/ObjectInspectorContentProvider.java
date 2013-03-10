/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * The content inspector for the object inspector.
 */
class ObjectInspectorContentProvider implements ITreeContentProvider {
  private static final Object[] EMPTY = new Object[0];

  public ObjectInspectorContentProvider() {

  }

  @Override
  public void dispose() {

  }

  @Override
  public Object[] getChildren(Object element) {
    IVariable variable = (IVariable) element;

    try {
      IValue value = variable.getValue();

      return value.getVariables();
    } catch (DebugException ex) {
      // TODO(devoncarew): determine the best way to present errors in the object inspector view
      ex.printStackTrace();

      return EMPTY;
    }
  }

  @Override
  public Object[] getElements(Object inputElement) {
    IValue value = (IValue) inputElement;

    if (value == null) {
      return EMPTY;
    } else {
      try {
        return value.getVariables();
      } catch (DebugException e) {
        // TODO(devoncarew): determine the best way to present errors in the object inspector view
        e.printStackTrace();

        return EMPTY;
      }
    }
  }

  @Override
  public Object getParent(Object element) {
    // TODO(devoncarew): implement - how to determine the parent of the value?

    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

  }

}
