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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Each line of the given text is preceded by a bullet.
 */
public class BulletListBlock extends Composite {

  private StyledText fStyledText;
  private boolean fEnabled;
  private String fText;

  public BulletListBlock(Composite parent, int style) {
    super(parent, style);
    fEnabled = true;
    fText = ""; //$NON-NLS-1$

    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    setLayout(layout);

    createControl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getEnabled() {
    return fEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    fEnabled = enabled;
    configureStyledText(fText, fEnabled);
  }

  public void setText(String text) {
    fText = text;
    configureStyledText(fText, fEnabled);
  }

  private void configureStyledText(String text, boolean enabled) {
    if (fStyledText == null) {
      return;
    }

    fStyledText.setText(text);
    int count = fStyledText.getCharCount();
    if (count == 0) {
      return;
    }

    Color foreground = enabled ? null : Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);

    fStyledText.setStyleRange(new StyleRange(0, count, foreground, null));

    StyleRange styleRange = new StyleRange(0, count, foreground, null);
    styleRange.metrics = new GlyphMetrics(0, 0, 20);
    fStyledText.setLineBullet(0, fStyledText.getLineCount(), new Bullet(styleRange));

    fStyledText.setEnabled(enabled);
  }

  private Control createControl(Composite parent) {
    fStyledText = new StyledText(parent, SWT.FLAT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    fStyledText.setEditable(false);
    Cursor arrowCursor = fStyledText.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
    fStyledText.setCursor(arrowCursor);

    // Don't set caret to 'null' as this causes https://bugs.eclipse.org/293263
//		fStyledText.setCaret(null);

    final GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
    fStyledText.setLayoutData(data);
    configureStyledText(fText, fEnabled);

    return fStyledText;
  }
}
