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
package com.google.dart.tools.ui.feedback;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Shell;

/**
 * A lightweight FeedbackReport screenshot viewer.
 */
public class ScreenshotViewer extends Shell {

  private Image screenshot;

  public ScreenshotViewer(Shell parent, final Image screenshot) {
    super(parent, SWT.CLOSE | SWT.RESIZE | SWT.TITLE);
    this.screenshot = screenshot;
    setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).margins(0, 0).create());

    ScrolledComposite scrolledComposite = new ScrolledComposite(this, SWT.BORDER | SWT.H_SCROLL
        | SWT.V_SCROLL);
    scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);

    scrolledComposite.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        e.gc.drawImage(screenshot, 0, 0);
      }
    });
    createContents();
    centerShell(parent);
  }

  @Override
  protected void checkSubclass() {
    // Disable the check that prevents subclassing of SWT components
  }

  protected void createContents() {
    setText(FeedbackMessages.LogViewer_LogViewer_title);
    setSize(screenshot.getImageData().width, screenshot.getImageData().height + 25);
  }

  private void centerShell(Shell parent) {
    Point parentSize = parent.getSize();
    Point size = getSize();

    Point location = parent.getLocation();

    setLocation(location.x + (parentSize.x - size.x) / 2, location.y + 50 + (parentSize.y - size.y)
        / 2);
  }

}
