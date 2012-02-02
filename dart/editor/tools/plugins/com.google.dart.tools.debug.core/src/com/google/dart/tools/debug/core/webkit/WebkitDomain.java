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

package com.google.dart.tools.debug.core.webkit;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * The parent class of the WIP domain (debugger, page, ...) classes.
 */
public abstract class WebkitDomain {
  protected WebkitConnection connection;

  public WebkitDomain(WebkitConnection connection) {
    this.connection = connection;
  }

  public WebkitConnection getConnection() {
    return connection;
  }

  protected void sendSimpleCommand(String command) throws IOException {
    try {
      connection.sendRequest(new JSONObject().put("method", command));
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

}
