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
package com.google.dart.tools.internal.corext.util;

import org.w3c.dom.Element;

public class QualifiedTypeNameHistory extends History {

  private static final String NODE_ROOT = "qualifiedTypeNameHistroy"; //$NON-NLS-1$
  private static final String NODE_TYPE_INFO = "fullyQualifiedTypeName"; //$NON-NLS-1$
  private static final String NODE_NAME = "name"; //$NON-NLS-1$

  private static QualifiedTypeNameHistory fgInstance;

  public static int getBoost(String fullyQualifiedTypeName, int min, int max) {
    float position = getDefault().getNormalizedPosition(fullyQualifiedTypeName);
    int dist = max - min;
    return Math.round(position * dist) + min;
  }

  public static QualifiedTypeNameHistory getDefault() {
    if (fgInstance == null) {
      fgInstance = new QualifiedTypeNameHistory("QualifiedTypeNameHistory.xml"); //$NON-NLS-1$
    }

    return fgInstance;
  }

  public static void remember(String fullyQualifiedTypeName) {
    getDefault().accessed(fullyQualifiedTypeName);
  }

  public QualifiedTypeNameHistory(String fileName) {
    super(fileName, NODE_ROOT, NODE_TYPE_INFO);
    load();
  }

  @Override
  protected Object createFromElement(Element element) {
    return element.getAttribute(NODE_NAME);
  }

  @Override
  protected Object getKey(Object object) {
    return object;
  }

  @Override
  protected void setAttributes(Object object, Element element) {
    element.setAttribute(NODE_NAME, (String) object);
  }

}
