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
package com.google.dart.tools.ui.internal.viewsupport;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

public class ColoringLabelProvider extends DecoratingStyledCellLabelProvider implements
    ILabelProvider {

  public static final Styler HIGHLIGHT_STYLE = StyledString.createColorRegistryStyler(
      null,
      ColoredViewersManager.HIGHLIGHT_BG_COLOR_NAME);
  public static final Styler HIGHLIGHT_WRITE_STYLE = StyledString.createColorRegistryStyler(
      null,
      ColoredViewersManager.HIGHLIGHT_WRITE_BG_COLOR_NAME);

  public static final Styler INHERITED_STYLER = StyledString.createColorRegistryStyler(
      ColoredViewersManager.INHERITED_COLOR_NAME,
      null);

  public ColoringLabelProvider(IStyledLabelProvider labelProvider) {
    this(labelProvider, null, null);
  }

  public ColoringLabelProvider(IStyledLabelProvider labelProvider, ILabelDecorator decorator,
      IDecorationContext decorationContext) {
    super(labelProvider, decorator, decorationContext);
  }

  @Override
  public void dispose() {
    super.dispose();
    ColoredViewersManager.uninstall(this);
  }

  @Override
  public String getText(Object element) {
    return getStyledText(element).getString();
  }

  @Override
  public void initialize(ColumnViewer viewer, ViewerColumn column) {
    ColoredViewersManager.install(this);
    setOwnerDrawEnabled(ColoredViewersManager.showColoredLabels());

    super.initialize(viewer, column);
  }

  public void update() {
    ColumnViewer viewer = getViewer();

    if (viewer == null) {
      return;
    }

    boolean needsUpdate = false;

    boolean showColoredLabels = ColoredViewersManager.showColoredLabels();
    if (showColoredLabels != isOwnerDrawEnabled()) {
      setOwnerDrawEnabled(showColoredLabels);
      needsUpdate = true;
    } else if (showColoredLabels) {
      needsUpdate = true;
    }
    if (needsUpdate) {
      fireLabelProviderChanged(new LabelProviderChangedEvent(this));
    }
  }

  @Override
  protected StyleRange prepareStyleRange(StyleRange styleRange, boolean applyColors) {
    if (!applyColors && styleRange.background != null) {
      styleRange = super.prepareStyleRange(styleRange, applyColors);
      styleRange.borderStyle = SWT.BORDER_DOT;
      return styleRange;
    }
    return super.prepareStyleRange(styleRange, applyColors);
  }
}
