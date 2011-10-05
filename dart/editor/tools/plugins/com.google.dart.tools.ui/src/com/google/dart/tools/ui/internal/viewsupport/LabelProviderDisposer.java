/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

/**
 * Helper class to manage images that should be disposed when a control is disposed
 * contol.addWidgetListener(new LabelProviderDisposer(myLabelProvider));
 */
public class LabelProviderDisposer implements DisposeListener {

  private ILabelProvider fLabelProvider;

  public LabelProviderDisposer(ILabelProvider labelProvider) {
    fLabelProvider = labelProvider;
  }

  @Override
  public void widgetDisposed(DisposeEvent e) {
    fLabelProvider.dispose();
  }
}
