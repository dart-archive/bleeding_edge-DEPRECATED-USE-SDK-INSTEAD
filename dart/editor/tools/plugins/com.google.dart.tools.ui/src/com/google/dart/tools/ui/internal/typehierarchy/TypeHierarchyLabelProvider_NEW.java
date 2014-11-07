/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.tools.ui.internal.text.editor.ElementLabelProvider_NEW;
import com.google.dart.tools.ui.internal.typehierarchy.TypeHierarchyContentProvider_NEW.TypeItem;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the hierarchy viewers.
 */
public class TypeHierarchyLabelProvider_NEW extends LabelProvider implements
    org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider {
  private final Predicate<Object> lightPredicate;
  private final ElementLabelProvider_NEW elementLabelProvider = new ElementLabelProvider_NEW();

  public TypeHierarchyLabelProvider_NEW(Predicate<Object> lightPredicate) {
    this.lightPredicate = lightPredicate;
  }

  @Override
  public Image getImage(Object o) {
    TypeItem item = (TypeItem) o;
    return elementLabelProvider.getImage(item.element);
  }

  @Override
  public StyledString getStyledText(Object o) {
    if (o instanceof TypeItem) {
      TypeItem item = (TypeItem) o;
      StyledString styledString = item.toStyledString();
      if (lightPredicate.apply(o)) {
        styledString.setStyle(0, styledString.getString().length(), StyledString.QUALIFIER_STYLER);
      }
      return styledString;
    }
    return new StyledString("" + o);
  }

  @Override
  public String getText(Object o) {
    if (o instanceof TypeItem) {
      TypeItem item = (TypeItem) o;
      return item.toStyledString().getString();
    }
    return "" + o;
  }
}
