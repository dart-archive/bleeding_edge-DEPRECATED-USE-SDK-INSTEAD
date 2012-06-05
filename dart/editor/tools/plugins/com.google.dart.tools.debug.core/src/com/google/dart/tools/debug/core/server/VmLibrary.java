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
import java.util.Collections;
import java.util.List;

/**
 * The representation of a VM library.
 */
public class VmLibrary {

  static List<VmLibrary> createFrom(JSONArray arr) throws JSONException {
    if (arr == null) {
      return Collections.emptyList();
    }

    List<VmLibrary> result = new ArrayList<VmLibrary>();

    for (int i = 0; i < arr.length(); i++) {
      result.add(VmLibrary.createFrom(arr.getJSONObject(i)));
    }

    return result;
  }

  static VmLibrary createFrom(JSONObject obj) {
    VmLibrary lib = new VmLibrary();

    lib.id = obj.optInt("id");
    lib.url = obj.optString("url");

    return lib;
  }

  private int id;

  private String url;

  private VmLibrary() {

  }

  public int getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String toString() {
    return "[" + id + "," + url + "]";
  }

}
