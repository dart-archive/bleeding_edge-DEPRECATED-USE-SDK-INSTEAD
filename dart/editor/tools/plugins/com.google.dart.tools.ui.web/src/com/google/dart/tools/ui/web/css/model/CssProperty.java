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
package com.google.dart.tools.ui.web.css.model;

import com.google.dart.tools.ui.web.utils.Node;

/**
 * A css model property object. A property is a key = value.
 */
public class CssProperty {
  private Node key;
  private Node value;

  public CssProperty() {

  }

  public Node getKey() {
    return key;
  }

  public Node getValue() {
    return value;
  }

  public void setKey(Node key) {
    this.key = key;
  }

  public void setValue(Node value) {
    this.value = value;
  }

}
