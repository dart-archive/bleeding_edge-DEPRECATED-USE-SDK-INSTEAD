/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.core.server;

import org.json.JSONObject;

/**
 * This class represents a VM value.
 */
public class VmValue {

  static VmValue createFrom(JSONObject obj) {
    if (obj == null) {
      return null;
    }

    // {"objectId":4,"kind":"object","text":"Instance of '_HttpServer@14117cc4'"}

    VmValue value = new VmValue();

    value.objectId = obj.optInt("objectId");
    value.kind = obj.optString("kind");
    value.text = obj.optString("text");

    return value;
  }

  private String text;

  private String kind;

  private int objectId;

  private VmValue() {

  }

  /**
   * @return one of "string", "integer", "object", "boolean".
   */
  public String getKind() {
    return kind;
  }

  public int getObjectId() {
    return objectId;
  }

  public String getText() {
    return text;
  }

  public boolean isObject() {
    return "object".equals(getKind());
  }

  public boolean isString() {
    return "string".equals(getKind());
  }

  @Override
  public String toString() {
    return getKind() + "," + getText();
  }

}
