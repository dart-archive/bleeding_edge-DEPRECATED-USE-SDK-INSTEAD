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
package com.google.dart.tools.ui.internal.typehierarchy;

import com.google.common.base.Predicate;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the hierarchy viewers.
 */
public class HierarchyLabelProvider extends DartElementLabelProvider {

  private final Predicate<Object> lightPredicate;

  public HierarchyLabelProvider(Predicate<Object> lightPredicate) {
    this.lightPredicate = lightPredicate;
  }

  @Override
  public Image getImage(Object element) {
    if (lightPredicate.apply(element)) {
      ImageDescriptor desc = DartPluginImages.DESC_OBJS_CLASSALT;
      return DartToolsPlugin.getImageDescriptorRegistry().get(desc);
    }
    return super.getImage(element);
  }

  @Override
  public StyledString getStyledText(Object element) {
    StyledString styledText = super.getStyledText(element);
    if (lightPredicate.apply(element)) {
      styledText.setStyle(0, styledText.getString().length(), StyledString.QUALIFIER_STYLER);
    }
    return styledText;
  }

}
