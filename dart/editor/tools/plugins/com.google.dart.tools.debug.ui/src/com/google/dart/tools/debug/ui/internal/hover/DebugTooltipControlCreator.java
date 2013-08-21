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

package com.google.dart.tools.debug.ui.internal.hover;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.widgets.Shell;

/**
 * An IInformationControlCreator implementation for Dart debugger tooltips.
 */
public class DebugTooltipControlCreator implements IInformationControlCreator {

  public static IInformationControlCreator newControlCreator() {
    return new DebugTooltipControlCreator(false);
  }

  public static IInformationControlCreator newControlCreatorResizeable() {
    return new DebugTooltipControlCreator(true);
  }

  private boolean resizable;

  private DebugTooltipControlCreator(boolean resizable) {
    this.resizable = resizable;
  }

  @Override
  public IInformationControl createInformationControl(Shell shell) {
    return new DebugTooltipControl(shell, resizable);
  }

}
