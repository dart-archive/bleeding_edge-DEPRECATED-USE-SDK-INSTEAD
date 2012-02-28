/*
 * Copyright (c) 2011, the Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.presentation;

import com.google.dart.tools.debug.core.dartium.DartiumDebugValue;
import com.google.dart.tools.debug.core.dartium.DartiumDebugVariable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.model.elements.VariableLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * A rich label provider for debug variables and values.
 */
@SuppressWarnings("restriction")
public class DartiumVariableLabelProvider extends VariableLabelProvider {

  @Override
  protected String getValueText(IVariable variable, IValue value, IPresentationContext context)
      throws CoreException {
    if (value instanceof DartiumDebugValue) {
      DartiumDebugValue dartiumValue = (DartiumDebugValue) value;

      return dartiumValue.getDisplayString();
    } else {
      return super.getValueText(variable, value, context);
    }
  }

  @Override
  protected String getVariableName(IVariable variable, IPresentationContext context)
      throws CoreException {
    if (variable instanceof DartiumDebugVariable) {
      DartiumDebugVariable dartiumVariable = (DartiumDebugVariable) variable;

      return dartiumVariable.getDisplayName();
    } else {
      return super.getVariableName(variable, context);
    }
  }

}
