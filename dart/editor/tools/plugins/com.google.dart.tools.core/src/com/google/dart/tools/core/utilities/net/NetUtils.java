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
package com.google.dart.tools.core.utilities.net;

import com.google.dart.tools.core.DartCore;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * A collection of static networking utilities.
 * 
 * @coverage dart.tools.core.utilities
 */
public class NetUtils {
  private static String loopbackAddress;

  /**
   * Compares whether two uris are equal. This handles the case where file urls are specified
   * slightly differently (file:/ vs file:///).
   * 
   * @param url1
   * @param url2
   * @return
   */
  public static boolean compareUrls(String url1, String url2) {
    if (url1.equals(url2)) {
      return true;
    }

    URI u1 = URI.create(url1);
    URI u2 = URI.create(url2);

    return u1.equals(u2);
  }

  /**
   * Find and return an unused server socket port. Attempt to use preferredPort; if that is not
   * available then we return any unused port.
   * 
   * @param preferredPort
   * @return
   */
  public static int findUnusedPort(int preferredPort) {
    try {
      // Bind to any free port.
      ServerSocket ss = new ServerSocket(0);

      int port = ss.getLocalPort();

      ss.close();

      return port;
    } catch (IOException ioe) {

    }

    return -1;
  }

  /**
   * @return the user's IP address, if available (null otherwise)
   */
  public static String getIpAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return null;
    }
  }

  /**
   * Return the localhost address (127.0.0.1/localhost/::1).
   * 
   * @return
   */
  public static String getLoopbackAddress() {
    if (loopbackAddress == null) {
      // Initialize the loopback address.

      try {
        // localhost? 127.0.0.1? InetAddress.getByName("localhost")?
        // InetAddress.getLocalHost().getHostAddress?
        InetAddress address = InetAddress.getByName("localhost");

        loopbackAddress = address.getHostAddress();
      } catch (UnknownHostException e) {
        DartCore.logError(e);

        // Fallback to the "localhost" address.
        return "localhost";
      }
    }

    return loopbackAddress;
  }

  /**
   * Finds the first unused port given a range of port numbers
   * 
   * @param startPortNumber
   * @param endPortNumber
   * @return first unused port number found, or -1 if none is found
   */
  public static int getUnusedPort(int startPortNumber, int endPortNumber) {
    for (int portNo = startPortNumber; portNo <= endPortNumber; portNo++) {
      if (available(portNo)) {
        return portNo;
      }
    }
    return -1;
  }

  private static boolean available(int port) {
    Socket s = null;
    try {
      s = new Socket("localhost", port);
      return false;
    } catch (IOException e) {
      return true;
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (IOException e) {

        }
      }
    }
  }

  private NetUtils() {

  }

}
