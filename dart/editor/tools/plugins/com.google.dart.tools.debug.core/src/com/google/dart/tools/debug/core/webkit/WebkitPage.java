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

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitConnection.NotificationHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A WIP page domain object.
 * 
 * @see http://code.google.com/chrome/devtools/docs/protocol/tot/page.html
 */
public class WebkitPage extends WebkitDomain {

  public static interface PageListener {
    public void domContentEventFired(int timestamp);

    public void frameDetached(String frameId);

    public void frameNavigated(String frameId, String url);

    public void loadEventFired(int timestamp);
  }

  public static abstract class PageListenerAdapter implements PageListener {
    @Override
    public void domContentEventFired(int timestamp) {

    }

    @Override
    public void frameDetached(String frameId) {

    }

    @Override
    public void frameNavigated(String frameId, String url) {

    }

    @Override
    public void loadEventFired(int timestamp) {

    }
  }

  private static final String PAGE_LOADEVENTFIRED = "Page.loadEventFired";
  private static final String PAGE_DOMCONTENTEVENTFIRED = "Page.domContentEventFired";
  private static final String PAGE_FRAMENAVIGATED = "Page.frameNavigated";
  private static final String PAGE_FRAMEDETACHED = "Page.frameDetached";

  private List<PageListener> listeners = new ArrayList<PageListener>();

  public WebkitPage(WebkitConnection connection) {
    super(connection);

    connection.registerNotificationHandler("Page.", new NotificationHandler() {
      @Override
      public void handleNotification(String method, JSONObject params) throws JSONException {
        handlePageNotification(method, params);
      }
    });
  }

  public void addPageListener(PageListener listener) {
    listeners.add(listener);
  }

  public void disable() throws IOException {
    sendSimpleCommand("Page.disable");
  }

  public void enable() throws IOException {
    sendSimpleCommand("Page.enable");
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

  public void removePageListener(PageListener listener) {
    listeners.remove(listener);
  }

  protected void handlePageNotification(String method, JSONObject params) throws JSONException {
    if (method.equals(PAGE_LOADEVENTFIRED)) {
      // { "timestamp": 12345678 }
      int timestamp = params.getInt("timestamp");

      for (PageListener listener : listeners) {
        listener.loadEventFired(timestamp);
      }
    } else if (method.equals(PAGE_DOMCONTENTEVENTFIRED)) {
      // { "timestamp": 12345678 }
      int timestamp = params.getInt("timestamp");

      for (PageListener listener : listeners) {
        listener.domContentEventFired(timestamp);
      }
    } else if (method.equals(PAGE_FRAMENAVIGATED)) {
      // {
      //   "frame": {
      //     "id":"8892.1","loaderId":"8892.2","securityOrigin":"http://www.cheese.com",
      //       "mimeType":"text/html","url":"http://www.cheese.com/"
      //   }
      // }
      JSONObject frame = params.getJSONObject("frame");

      String frameId = JsonUtils.getString(frame, "frameId");
      String url = JsonUtils.getString(frame, "url");

      for (PageListener listener : listeners) {
        listener.frameNavigated(frameId, url);
      }
    } else if (method.equals(PAGE_FRAMEDETACHED)) {
      // {"frameId":"9121.19"}
      String frameId = params.getString("frameId");

      for (PageListener listener : listeners) {
        listener.frameDetached(frameId);
      }
    } else {
      DartDebugCorePlugin.logInfo("unhandled notification: " + method);
    }
  }

}
