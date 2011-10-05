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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.actions.SelectionDispatchAction;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Dart element hyperlink.
 */
public class DartElementHyperlink implements IHyperlink {

  private final DartElement element;
  private final SelectionDispatchAction openAction;
  private final IRegion region;

  /**
   * Creates a new Dart element hyperlink.
   */
  public DartElementHyperlink(DartElement element, IRegion region,
      SelectionDispatchAction openAction) {
    Assert.isNotNull(element);
    Assert.isNotNull(region);
    Assert.isNotNull(openAction);
    this.element = element;
    this.region = region;
    this.openAction = openAction;
  }

  @Override
  public IRegion getHyperlinkRegion() {
    return region;
  }

  @Override
  public String getHyperlinkText() {
    return openAction.getToolTipText();
  }

  @Override
  public String getTypeLabel() {
    return null;
  }

  @Override
  public void open() {
    openAction.run(new StructuredSelection(element));
  }
}
