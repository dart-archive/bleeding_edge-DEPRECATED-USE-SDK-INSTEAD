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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.services.refactoring.ParameterInfo;

public interface IParameterListChangeListener {

  /**
   * Empty implementation of {@link IParameterListChangeListener}.
   */
  class Empty implements IParameterListChangeListener {
    @Override
    public void parameterAdded(ParameterInfo parameter) {
    }

    @Override
    public void parameterChanged(ParameterInfo parameter) {
    }

    @Override
    public void parameterListChanged() {
    }
  }

  /**
   * Gets fired when the given parameter has been added
   * 
   * @param parameter the parameter that has been added.
   */
  void parameterAdded(ParameterInfo parameter);

  /**
   * Gets fired when the given parameter has changed
   * 
   * @param parameter the parameter that has changed.
   */
  void parameterChanged(ParameterInfo parameter);

  /**
   * Gets fired if the parameter list got modified by reordering or removing parameters (note that
   * adding is handled by <code>parameterAdded</code>))
   */
  void parameterListChanged();
}
