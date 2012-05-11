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

package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A web server that serves up workspace resources.
 */
public class ResourceServer implements IResourceResolver {
  private ServerSocket serverSocket;
  private ExecutorService threadPool;

  private Set<String> previousAgents = new HashSet<String>();

  /**
   * Create a ResourceServer; serve its resources from any free port.
   * 
   * @throws IOException
   */
  public ResourceServer() throws IOException {
    this(0);
  }

  /**
   * Create a ResourceServer; serve its resources from the given port.
   * 
   * @throws IOException
   */
  public ResourceServer(int port) throws IOException {
    serverSocket = new ServerSocket(port);
    threadPool = Executors.newCachedThreadPool();

    new Thread(new Runnable() {
      @Override
      public void run() {
        startServer();
      }
    }).start();
  }

  public int getPort() {
    return serverSocket.getLocalPort();
  }

  @Override
  public String getUrlForResource(IFile file) {
    try {
      URI fileUri = file.getLocation().toFile().toURI();

      String pathSegment = fileUri.getPath();

      // localhost? 127.0.0.1? serverSocket.getInetAddress().getHostAddress()?
      URI uri = new URI(
          "http",
          null,
          "127.0.0.1",
          serverSocket.getLocalPort(),
          pathSegment,
          null,
          null);
      return uri.toString();
    } catch (URISyntaxException e) {
      DartDebugCorePlugin.logError(e);

      return null;
    }
  }

  /**
   * Close the resource server.
   */
  public void shutdown() {
    try {
      serverSocket.close();
    } catch (IOException exception) {
      DartDebugCorePlugin.logError(exception);
    }
  }

  protected void loadingContentFrom(String hostAddress, String userAgent) {
    if (!previousAgents.contains(userAgent)) {
      previousAgents.add(userAgent);

      DartCore.getConsole().println(
          "Remote connection from " + hostAddress + " [" + userAgent + "]");
    }
  }

  private void startServer() {
    try {
      while (true) {
        Socket socket = serverSocket.accept();

        threadPool.execute(new ResourceServerHandler(this, socket));
      }
    } catch (IOException e) {
      // The server socket was closed by the shutdown() call.

    }
  }

}
