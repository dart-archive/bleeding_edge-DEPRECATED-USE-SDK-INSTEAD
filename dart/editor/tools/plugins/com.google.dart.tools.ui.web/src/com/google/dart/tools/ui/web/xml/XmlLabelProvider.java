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

package com.google.dart.tools.ui.web.xml;

import com.google.dart.tools.core.html.XmlNode;
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for xml and html model elements.
 */
public class XmlLabelProvider extends LabelProvider {

  @Override
  public Image getImage(Object element) {
    return DartWebPlugin.getImage("xml_node.gif");
  }

  @Override
  public String getText(Object element) {
    XmlNode node = (XmlNode) element;

    return node.getLabel();
  }

}
