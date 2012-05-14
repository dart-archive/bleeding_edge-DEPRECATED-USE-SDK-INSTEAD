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
package com.google.dart.tools.deploy;

import com.google.dart.tools.ui.feedback.FeedbackControlContribution;
import com.google.dart.tools.ui.omni.OmniBoxControlContribution;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Contributes Dart Toolbar controls (search box, feedback button, ...).
 */
public class DartEditorToolbarControlContribution extends WorkbenchWindowControlContribution {

  private static final int VERTICAL_NUDGE = Util.isLinux() ? 0 : 1;

  private FeedbackControlContribution feedbackButton;
  private OmniBoxControlContribution searchBox;

  @Override
  public void dispose() {
    if (searchBox != null) {
      searchBox.dispose();
    }
  }

  @Override
  protected Control createControl(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(4, VERTICAL_NUDGE).spacing(4, 0).applyTo(
        composite);

    searchBox = new OmniBoxControlContribution(this);
    searchBox.createControl(composite);

    feedbackButton = new FeedbackControlContribution(this);
    feedbackButton.createControl(composite);

    return composite;
  }
}
