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
package com.google.dart.tools.search2.internal.ui.basic.views;

import com.google.dart.tools.search.ui.text.AbstractTextSearchViewPage;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

public class SetLayoutAction extends Action {

  private AbstractTextSearchViewPage fPage;
  private int fLayout;

  public SetLayoutAction(AbstractTextSearchViewPage page, String label, String tooltip, int layout) {
    super(label, IAction.AS_RADIO_BUTTON);
    fPage = page;
    setToolTipText(tooltip);
    fLayout = layout;
  }

  public void run() {
    fPage.setLayout(fLayout);
  }

  public int getLayout() {
    return fLayout;
  }
}
