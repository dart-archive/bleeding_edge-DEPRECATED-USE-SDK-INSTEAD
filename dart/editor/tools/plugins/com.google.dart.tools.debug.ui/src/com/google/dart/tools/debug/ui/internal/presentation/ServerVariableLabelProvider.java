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

package com.google.dart.tools.debug.ui.internal.presentation;

import com.google.dart.tools.debug.core.server.ServerDebugValue;
import com.google.dart.tools.debug.core.server.ServerDebugVariable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.model.elements.VariableLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * A label provider for VM debugger objects.
 */
@SuppressWarnings("restriction")
public class ServerVariableLabelProvider extends VariableLabelProvider {

  @Override
  protected String getValueText(IVariable variable, IValue value, IPresentationContext context)
      throws CoreException {
    if (value instanceof ServerDebugValue) {
      ServerDebugValue val = (ServerDebugValue) value;

      return val.getDisplayString();
    } else {
      return super.getValueText(variable, value, context);
    }
  }

  @Override
  protected String getVariableName(IVariable variable, IPresentationContext context)
      throws CoreException {
    if (variable instanceof ServerDebugVariable) {
      ServerDebugVariable var = (ServerDebugVariable) variable;

      return var.getDisplayName();
    } else {
      return super.getVariableName(variable, context);
    }
  }

}
