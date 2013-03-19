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

package com.google.dart.tools.debug.core.util;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;

public class HttpUrlConnectorTest extends TestCase {

  public void test_localHostConnect_fail() throws IOException {
    int port = ResourceServerManager.getServer().getPort();

    HttpUrlConnector connector = new HttpUrlConnector(null, port, "/dssdf");

    InputStream in = connector.getInputStream();

    try {
      assertNotNull(in);
      assertEquals(404, connector.getStatusCode());
      assertEquals("Not Found", connector.getStatusText());
    } finally {
      in.close();
    }
  }

  public void test_localHostConnect_succeed() throws IOException {
    int port = ResourceServerManager.getServer().getPort();

    HttpUrlConnector connector = new HttpUrlConnector(null, port, "/favicon.ico");

    InputStream in = connector.getInputStream();

    try {
      assertNotNull(in);
      assertEquals(200, connector.getStatusCode());
      assertEquals("OK", connector.getStatusText());
      assertTrue(connector.getContentLength() > 0);
    } finally {
      in.close();
    }
  }

}
