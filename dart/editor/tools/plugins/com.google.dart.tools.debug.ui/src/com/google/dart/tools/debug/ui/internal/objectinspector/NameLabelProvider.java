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

import com.google.dart.tools.debug.ui.internal.presentation.DartDebugModelPresentation;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * The label provider for the 'name' column in the object inspector.
 */
class NameLabelProvider extends ColumnLabelProvider {
  private static DartDebugModelPresentation presentation = new DartDebugModelPresentation();

  public NameLabelProvider() {

  }

  @Override
  public Image getImage(Object element) {
    return presentation.getImage(element);
  }

  @Override
  public String getText(Object element) {
    IVariable variable = (IVariable) element;

    try {
      return variable.getName();
    } catch (DebugException e) {
      e.printStackTrace();

      return null;
    }
  }

}
