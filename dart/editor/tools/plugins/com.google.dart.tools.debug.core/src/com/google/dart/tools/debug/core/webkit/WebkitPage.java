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
 * A WIP page domain object.
 */
public class WebkitPage extends WebkitDomain {

  public WebkitPage(WebkitConnection connection) {
    super(connection);
  }

  public void navigate(String url) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Page.navigate");
      request.put("params", new JSONObject().put("url", url));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void reload() throws IOException {
    sendSimpleCommand("Page.reload");
  }

}
