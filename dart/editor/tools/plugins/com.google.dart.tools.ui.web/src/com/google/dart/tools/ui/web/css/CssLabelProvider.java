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

package com.google.dart.tools.ui.web.css;

import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.css.model.CssProperty;
import com.google.dart.tools.ui.web.utils.Node;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for css model elements.
 */
public class CssLabelProvider extends LabelProvider implements IStyledLabelProvider {

  @Override
  public Image getImage(Object element) {
    if (element instanceof CssProperty) {
      return DartWebPlugin.getImage("protected_co.gif");
    } else {
      return DartWebPlugin.getImage("public_co.gif");
    }
  }

  @Override
  public StyledString getStyledText(Object element) {
    Node node = (Node) element;

    StyledString string = new StyledString(node.getLabel());

    String auxText = getAuxText(node);

    if (auxText != null) {
      string.append(" - " + auxText, StyledString.QUALIFIER_STYLER);
    }

    return string;
  }

  private String getAuxText(Node node) {

    return null;
  }

}
