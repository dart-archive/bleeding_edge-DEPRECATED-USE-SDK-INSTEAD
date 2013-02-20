/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A WIP Worker domain object.
 */
@WebkitUnsupported
public class WebkitWorker extends WebkitDomain {

  public static interface WorkerListener {
    void disconnectedFromWorker();

    void dispatchMessageFromWorker(int workerId, JSONObject message);

    void workerCreated(int workerId, String url, boolean inspectorConnected);

    void workerTerminated(int workerId);
  }

  private List<WorkerListener> listeners = new ArrayList<WorkerListener>();

  private static final String WORKER_CREATED = "workerCreated";

  private static final String WORKER_TERMINATED = "workerTerminated";

  private static final String DISPATCH_MESSAGE_FROM_WORKER = "dispatchMessageFromWorker";

  private static final String DISCONNECTED_FROM_WORKER = "disconnectedFromWorker";

  public WebkitWorker(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("Worker.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handleWorkerNotification(method, params);
      }
    });
  }

  public void addWorkerListener(WorkerListener listener) {
    listeners.add(listener);
  }

  public void connectToWorker(int workerId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Worker.connectToWorker");
      request.put("params", new JSONObject().put("workerId", workerId));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void disable() throws IOException {
    sendSimpleCommand("Worker.disable");
  }

  public void disconnectFromWorker(int workerId) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Worker.disconnectFromWorker");
      request.put("params", new JSONObject().put("workerId", workerId));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void enable() throws IOException {
    sendSimpleCommand("Worker.enable");
  }

  public void removeWorkerListener(WorkerListener listener) {
    listeners.remove(listener);
  }

  public void sendMessageToWorker(int workerId, JSONObject message) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Worker.sendMessageToWorker");
      request.put("params", new JSONObject().put("message", message));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  public void setAutoconnectToWorkers(boolean value) throws IOException {
    try {
      JSONObject request = new JSONObject();

      request.put("method", "Worker.setAutoconnectToWorkers");
      request.put("params", new JSONObject().put("value", value));

      connection.sendRequest(request);
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  protected void handleWorkerNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(WORKER_CREATED)) {
      int workerId = params.getInt("workerId");
      String url = params.optString("url");
      boolean inspectorConnected = params.getBoolean("inspectorConnected");

      for (WorkerListener listener : listeners) {
        listener.workerCreated(workerId, url, inspectorConnected);
      }
    } else if (method.equals(WORKER_TERMINATED)) {
      int workerId = params.getInt("workerId");

      for (WorkerListener listener : listeners) {
        listener.workerTerminated(workerId);
      }
    } else if (method.equals(DISPATCH_MESSAGE_FROM_WORKER)) {
      int workerId = params.getInt("workerId");
      JSONObject message = params.getJSONObject("message");

      for (WorkerListener listener : listeners) {
        listener.dispatchMessageFromWorker(workerId, message);
      }
    } else if (method.equals(DISCONNECTED_FROM_WORKER)) {
      for (WorkerListener listener : listeners) {
        listener.disconnectedFromWorker();
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

}
