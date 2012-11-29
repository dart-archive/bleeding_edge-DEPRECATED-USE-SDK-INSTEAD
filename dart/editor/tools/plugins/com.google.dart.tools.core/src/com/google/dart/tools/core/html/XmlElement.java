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

package com.google.dart.tools.core.html;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class XmlElement extends XmlNode {
  private List<XmlAttribute> attributes = new ArrayList<XmlAttribute>();

  public XmlElement() {

  }

  public XmlElement(String label) {
    super(label);
  }

  public List<XmlAttribute> getAttributes() {
    return attributes;
  }

  public String getAttributeString(String name) {
    for (XmlAttribute attr : attributes) {
      if (name.equals(attr.getName())) {
        return attr.getValue();
      }
    }

    return null;
  }

  void addAttribute(XmlAttribute attribute) {
    attributes.add(attribute);
  }

  void appendContents(String value) {
    if (getContents() == null) {
      setContents(value);
    } else {
      setContents(getContents() + value);
    }
  }

}
