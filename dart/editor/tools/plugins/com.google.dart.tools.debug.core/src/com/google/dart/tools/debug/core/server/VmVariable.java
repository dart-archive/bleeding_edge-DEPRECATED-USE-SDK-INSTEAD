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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a VM variable.
 */
public class VmVariable {

  static List<VmVariable> createFrom(JSONArray arr) throws JSONException {
    if (arr == null) {
      return null;
    }

    List<VmVariable> variables = new ArrayList<VmVariable>();

    for (int i = 0; i < arr.length(); i++) {
      variables.add(createFrom(arr.getJSONObject(i)));
    }

    return variables;
  }

  static VmVariable createFrom(JSONObject obj) {
    // {"name":"server","value":{"objectId":4,"kind":"object","text":"Instance of '_HttpServer@14117cc4'"}}
    VmVariable var = new VmVariable();

    var.name = obj.optString("name");
    var.value = VmValue.createFrom(obj.optJSONObject("value"));

    return var;
  }

  static VmVariable createFromException(VmValue exception) {
    VmVariable variable = new VmVariable();

    variable.name = "<exception>";
    variable.value = exception;
    variable.isException = true;

    return variable;
  }

  private VmValue value;

  private String name;

  private boolean isException;

  private VmVariable() {

  }

  public boolean getIsException() {
    return isException;
  }

  public String getName() {
    return name;
  }

  public VmValue getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "[" + getName() + "," + getValue() + "]";
  }

}
