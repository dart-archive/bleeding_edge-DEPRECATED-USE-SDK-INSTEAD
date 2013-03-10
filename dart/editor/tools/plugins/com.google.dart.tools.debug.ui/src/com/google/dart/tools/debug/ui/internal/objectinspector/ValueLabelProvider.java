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
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * The label provider for the 'values' column in the object inspector.
 */
class ValueLabelProvider extends DelegatingStyledCellLabelProvider {

  static class ValueStyledLabelProvider extends LabelProvider implements IStyledLabelProvider {
    public ValueStyledLabelProvider() {

    }

    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public StyledString getStyledText(Object element) {
      StyledString str = new StyledString();

      try {
        IVariable variable = (IVariable) element;
        IValue value = variable.getValue();

        str.append(value.getValueString());

        // TODO(devoncarew): add a suitable styled decoration
        //str.append(' ');
        //str.append(value.getReferenceTypeName(), StyledString.DECORATIONS_STYLER);
      } catch (DebugException e) {
        e.printStackTrace();
      }

      return str;
    }

    @Override
    public String getText(Object element) {
      try {
        IVariable variable = (IVariable) element;
        IValue value = variable.getValue();

        return value.getValueString();
      } catch (DebugException e) {
        e.printStackTrace();

        return null;
      }
    }
  }

  public ValueLabelProvider() {
    super(new ValueStyledLabelProvider());
  }

}
