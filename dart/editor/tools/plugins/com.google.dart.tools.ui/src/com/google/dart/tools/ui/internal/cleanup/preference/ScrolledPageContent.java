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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

public class ScrolledPageContent extends SharedScrolledComposite {

  private FormToolkit fToolkit;

  public ScrolledPageContent(Composite parent) {
    this(parent, SWT.V_SCROLL | SWT.H_SCROLL);
  }

  public ScrolledPageContent(Composite parent, int style) {
    super(parent, style);

    setFont(parent.getFont());

    fToolkit = DartToolsPlugin.getDefault().getDialogsFormToolkit();

    setExpandHorizontal(true);
    setExpandVertical(true);

    Composite body = new Composite(this, SWT.NONE);
    body.setFont(parent.getFont());
    setContent(body);
  }

  public void adaptChild(Control childControl) {
    fToolkit.adapt(childControl, true, true);
  }

  public Composite getBody() {
    return (Composite) getContent();
  }

}
