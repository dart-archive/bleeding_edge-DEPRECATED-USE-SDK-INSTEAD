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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retrieve Webkit inspection protocol debugging information from a Chromium instance.
 */
public class ChromiumConnector {
  public static final String LOCALHOST_ADDRESS = "127.0.0.1";

  /**
   * Return the list of open tabs for this browser.
   * 
   * @param port
   * @return
   * @throws IOException
   */
  public static List<ChromiumTabInfo> getAvailableTabs(int port) throws IOException {
    return getAvailableTabs(null, port);
  }

  /**
   * Return the list of open tabs for this browser.
   * 
   * @param host
   * @param port
   * @return
   * @throws IOException
   */
  public static List<ChromiumTabInfo> getAvailableTabs(String host, int port) throws IOException {
//  GET /json 1.0
//
//  HTTP/1.1 200 OK
//  Content-Type:application/json; charset=UTF-8
//  Content-Length:871
//
//  [ {
//     "devtoolsFrontendUrl": "/devtools/devtools.html?host=&page=3",
//     "faviconUrl": "http://www.apple.com/favicon.ico",
//     "thumbnailUrl": "/thumb/http://www.apple.com/",
//     "title": "Apple",
//     "url": "http://www.apple.com/",
//     "webSocketDebuggerUrl": "ws:///devtools/page/3"
//  }, {
//     "devtoolsFrontendUrl": "/devtools/devtools.html?host=&page=2",
//     "faviconUrl": "http://www.yahoo.com/favicon.ico",
//     "thumbnailUrl": "/thumb/http://www.yahoo.com/",
//     "title": "Yahoo!",
//     "url": "http://www.yahoo.com/",
//     "webSocketDebuggerUrl": "ws:///devtools/page/2"
//  }, {
//     "devtoolsFrontendUrl": "/devtools/devtools.html?host=&page=1",
//     "faviconUrl": "http://www.gstatic.com/news/img/favicon.ico",
//     "thumbnailUrl": "/thumb/http://news.google.com/",
//     "title": "Google News",
//     "url": "http://news.google.com/",
//     "webSocketDebuggerUrl": "ws:///devtools/page/1"
//  } ]

    if (host == null) {
      host = LOCALHOST_ADDRESS;
    }

    URL chromiumUrl = new URL("http", host, port, "/json");

    URLConnection connection = chromiumUrl.openConnection();

    String text = readText(connection, connection.getInputStream());

    try {
      JSONArray arr = new JSONArray(text);

      List<ChromiumTabInfo> tabs = new ArrayList<ChromiumTabInfo>();

      for (int i = 0; i < arr.length(); i++) {
        JSONObject object = arr.getJSONObject(i);

        ChromiumTabInfo tab = ChromiumTabInfo.fromJson(object);

        tab.patchUpUrl(host, port);

        tabs.add(tab);
      }

      Collections.sort(tabs, ChromiumTabInfo.getComparator());

      return tabs;
    } catch (JSONException exception) {
      throw new IOException(exception);
    }
  }

  /**
   * Return the correct websocket URL for connecting to the Chromium instance at localhost and the
   * given port.
   * 
   * @param host
   * @param port
   * @param tab
   * @return
   */
  public static String getWebSocketURLFor(int port, int tab) {
    return getWebSocketURLFor(null, port, tab);
  }

  /**
   * Return the correct websocket URL for connecting to the Chromium instance at the given host and
   * port.
   * 
   * @param host
   * @param port
   * @param tab
   * @return
   */
  public static String getWebSocketURLFor(String host, int port, int tab) {
    if (host == null) {
      host = LOCALHOST_ADDRESS;
    }

    return "ws://" + host + ":" + port + "/devtools/page/" + tab;
  }

  private static String readText(URLConnection connection, InputStream in) throws IOException {
    // TODO(devoncarew): get the encoding from the stream?

    Reader reader = new InputStreamReader(in, "UTF-8");
    StringBuilder builder = new StringBuilder();

    char[] buffer = new char[1024];

    int count = reader.read(buffer);

    while (count != -1) {
      builder.append(buffer, 0, count);

      count = reader.read(buffer);
    }

    return builder.toString();
  }

  private ChromiumConnector() {

  }

}
