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

import java.util.Comparator;

/**
 * A class used to represent meta-information about an open Chronium tab, including its WIP debugger
 * URL.
 * 
 * @see ChromiumConnector
 */
public class ChromiumTabInfo {

  static ChromiumTabInfo fromJson(JSONObject object) throws JSONException {
// {
//    "devtoolsFrontendUrl": "/devtools/devtools.html?host=&page=3",
//    "faviconUrl": "http://www.apple.com/favicon.ico",
//    "thumbnailUrl": "/thumb/http://www.apple.com/",
//    "title": "Apple",
//    "url": "http://www.apple.com/",
//    "webSocketDebuggerUrl": "ws:///devtools/page/3"
// }

    ChromiumTabInfo tab = new ChromiumTabInfo();

    tab.devtoolsFrontendUrl = JsonUtils.getString(object, "devtoolsFrontendUrl");
    tab.faviconUrl = JsonUtils.getString(object, "faviconUrl");
    tab.thumbnailUrl = JsonUtils.getString(object, "thumbnailUrl");
    tab.title = JsonUtils.getString(object, "title");
    tab.url = JsonUtils.getString(object, "url");
    tab.webSocketDebuggerUrl = JsonUtils.getString(object, "webSocketDebuggerUrl");

    return tab;
  }

  static Comparator<ChromiumTabInfo> getComparator() {
    return new Comparator<ChromiumTabInfo>() {
      @Override
      public int compare(ChromiumTabInfo o1, ChromiumTabInfo o2) {
        // Sort by the tab order.
        String url1 = o1.getWebSocketDebuggerUrl();
        String url2 = o2.getWebSocketDebuggerUrl();

        if (url1 == url2) {
          return 0;
        } else if (url1 == null) {
          return -1;
        } else if (url2 == null) {
          return 1;
        } else {
          return url1.compareTo(url2);
        }
      }
    };
  }

  private String devtoolsFrontendUrl;

  private String faviconUrl;

  private String thumbnailUrl;

  private String title;

  private String url;

  private String webSocketDebuggerUrl;

  private ChromiumTabInfo() {

  }

  public String getDevtoolsFrontendUrl() {
    return devtoolsFrontendUrl;
  }

  public String getFaviconUrl() {
    return faviconUrl;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public String getWebSocketDebuggerUrl() {
    return webSocketDebuggerUrl;
  }

  @Override
  public String toString() {
    return "[" + getTitle() + "," + getUrl() + "," + getWebSocketDebuggerUrl() + "]";
  }

  /**
   * Convert a 'ws:///devtools/page/3' websocket URL to a ws://host:port/devtools/page/3 url.
   */
  void patchUpUrl(String host, int port) {
    if (webSocketDebuggerUrl != null && webSocketDebuggerUrl.startsWith("ws:///")) {
      webSocketDebuggerUrl = "ws://" + host + ":" + port
          + webSocketDebuggerUrl.substring("ws:///".length());
    }
  }

}
