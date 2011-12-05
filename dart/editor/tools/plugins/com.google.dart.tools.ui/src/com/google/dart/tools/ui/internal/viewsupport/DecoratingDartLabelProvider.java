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

import com.google.dart.tools.ui.ProblemsLabelDecorator;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelDecorator;
import org.eclipse.ui.PlatformUI;

public class DecoratingDartLabelProvider extends DecoratingLabelProvider implements
    IRichLabelProvider {

  /**
   * Decorating label provider for Java. Combines a DartUILabelProvider with problem and override
   * indicator with the workbench decorator (label decorator extension point).
   * 
   * @param labelProvider the label provider to decorate
   */
  public DecoratingDartLabelProvider(DartUILabelProvider labelProvider) {
    this(labelProvider, true);
  }

  /**
   * Decorating label provider for Java. Combines a DartUILabelProvider (if enabled with problem
   * indicator) with the workbench decorator (label decorator extension point).
   * 
   * @param labelProvider the label provider to decorate
   * @param errorTick show error ticks
   */
  public DecoratingDartLabelProvider(DartUILabelProvider labelProvider, boolean errorTick) {
    this(labelProvider, errorTick, true);
  }

  /**
   * Decorating label provider for Java. Combines a DartUILabelProvider (if enabled with problem
   * indicator) with the workbench decorator (label decorator extension point).
   * 
   * @param labelProvider the label provider to decorate
   * @param errorTick show error ticks
   * @param flatPackageMode configure flat package mode
   */
  public DecoratingDartLabelProvider(DartUILabelProvider labelProvider, boolean errorTick,
      boolean flatPackageMode) {
    super(labelProvider, PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
    if (errorTick) {
      labelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
    }
    setFlatPackageMode(flatPackageMode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.dart.tools.ui.internal.viewsupport.IRichLabelProvider# getRichTextLabel
   * (Object)
   */
  @Override
  public ColoredString getRichTextLabel(Object element) {
    ILabelProvider labelProvider = getLabelProvider();
    if (labelProvider instanceof IRichLabelProvider) {
      // get a rich label from the label decorator
      IRichLabelProvider richLabelProvider = (IRichLabelProvider) labelProvider;
      ColoredString richLabel = richLabelProvider.getRichTextLabel(element);
      if (richLabel != null) {
        String decorated = null;
        ILabelDecorator labelDecorator = getLabelDecorator();
        if (labelDecorator != null) {
          if (labelDecorator instanceof LabelDecorator) {
            decorated = ((LabelDecorator) labelDecorator).decorateText(richLabel.getString(),
                element, getDecorationContext());
          } else {
            decorated = labelDecorator.decorateText(richLabel.getString(), element);
          }
        }
        if (decorated != null) {
          return ColoredDartElementLabels.decorateColoredString(richLabel, decorated,
              ColoredDartElementLabels.DECORATIONS_STYLE);
        }
        return richLabel;
      }
    }
    return null;
  }

  /**
   * Tells the label decorator if the view presents packages flat or hierarchical.
   * 
   * @param enable If set, packages are presented in flat mode.
   */
  public void setFlatPackageMode(boolean enable) {
    //TODO (pquitslund): dart has no packages so package representation logic should go away
//    if (enable) {
    setDecorationContext(DecorationContext.DEFAULT_CONTEXT);
//    } else {
//      setDecorationContext(HierarchicalDecorationContext.CONTEXT);
//    }
  }

}
