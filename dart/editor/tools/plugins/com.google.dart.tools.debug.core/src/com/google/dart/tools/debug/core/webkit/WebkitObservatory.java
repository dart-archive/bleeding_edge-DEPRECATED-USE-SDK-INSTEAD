/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * A webkit domain class to connect to the Dart Observatory.
 */
public class WebkitObservatory extends WebkitDomain {

  private int commandId = 1;

  public WebkitObservatory(WebkitConnection connection) {
    super(connection);

    // The domain is currently "Dart". Something like "DartObservatory" would make more sense. 
    connection.registerNotificationHandler("Dart.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleObservatoryNotification(method, params);
      }
    });
  }

  /**
   * Send the Observatory command to list the isolates.
   * 
   * @throws IOException
   */
  public void listIsolates() throws IOException {
    sendObservatoryCommand("/isolates/");
  }

  protected void handleObservatoryNotification(String method, JSONObject params)
      throws JSONException {
    System.out.println("ping from observatory: " + method + ", params=" + params.toString());
  }

  private void sendObservatoryCommand(String query) throws IOException {
//    "name": "Dart.observatoryQuery",
//    "params": [
//        { "name": "id", "type": "string" },
//        { "name": "query", "type": "string" }
//    ]

    // TODO: The observatory documentation says 'parameters', but it actually expects 'params'.
    // TODO: 'id' must be a string; but the WIP id is an int.

    int id = commandId++;

    try {
      JSONObject command = new JSONObject();
      command.put("method", "Dart.observatoryQuery");

      JSONObject parameters = new JSONObject();
      parameters.put("id", Integer.toString(id));
      parameters.put("query", query);
      command.put("params", parameters);

      connection.sendRequest(command);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }
}
