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

package com.google.dart.tools.ui.web.html;

import com.google.dart.tools.core.html.XmlElement;
import com.google.dart.tools.core.html.XmlNode;
import com.google.dart.tools.ui.web.xml.XmlLabelProvider;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;

public class HtmlLabelProvider extends XmlLabelProvider implements IStyledLabelProvider {

  @Override
  public StyledString getStyledText(Object element) {
    XmlNode node = (XmlNode) element;

    StyledString string = new StyledString(node.getLabel());

    String auxText = getAuxText(node);

    if (auxText != null) {
      string.append(" - " + auxText, StyledString.QUALIFIER_STYLER);
    }

    return string;
  }

  private String getAuxText(XmlNode node) {
    if ("title".equalsIgnoreCase(node.getLabel())) {
      return trim(node.getContents());
    }

    if (node instanceof XmlElement) {
      XmlElement element = (XmlElement) node;

      if ("link".equalsIgnoreCase(element.getLabel())) {
        return element.getAttributeString("href");
      }

      if ("script".equalsIgnoreCase(element.getLabel())) {
        return element.getAttributeString("type");
      }

      if ("img".equalsIgnoreCase(element.getLabel()) && element.getAttributeString("src") != null) {
        return element.getAttributeString("src");
      }

      if (element.getAttributeString("id") != null) {
        return "#" + element.getAttributeString("id");
      }
    }

    return null;
  }

  private String trim(String str) {
    if (str == null) {
      return str;
    } else {
      return str.toString();
    }
  }

}
