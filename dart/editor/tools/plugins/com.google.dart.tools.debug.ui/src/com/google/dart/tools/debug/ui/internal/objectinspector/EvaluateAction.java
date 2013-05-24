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

import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * An evaluate action for the object inspector.
 */
class EvaluateAction extends Action {
  private ObjectInspectorView objectInspectorView;

  public EvaluateAction(ObjectInspectorView objectInspectorView) {
    super("Evaluate Selection", DartDebugUIPlugin.getImageDescriptor("obj16/variable_tab.gif"));

    this.objectInspectorView = objectInspectorView;

    setEnabled(objectInspectorView.getPrintItAction().isEnabled());

    objectInspectorView.getPrintItAction().addPropertyChangeListener(new IPropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        setEnabled(((Boolean) event.getNewValue()).booleanValue());
      }
    });
  }

  @Override
  public void run() {
    objectInspectorView.performEvaulation();
  }
}
