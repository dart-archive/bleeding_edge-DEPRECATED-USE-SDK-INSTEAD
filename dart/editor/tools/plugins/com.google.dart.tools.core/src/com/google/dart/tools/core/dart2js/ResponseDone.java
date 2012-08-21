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

package com.google.dart.tools.core.dart2js;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A JSON server response indicating that the compile completed.
 */
public class ResponseDone {
  private boolean success;

  ResponseDone(JSONObject object) throws JSONException {
    this.success = object.getBoolean("result");
  }

  public boolean isSuccess() {
    return success;
  }

}
