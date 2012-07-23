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

import com.google.common.io.CharStreams;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
    }, "Web Server Dispatch").start();
  }

  /**
   * @return the user's IP address, if available (null otherwise)
   */
  public String getLocalAddress() {
    return NetUtils.getIpAddress();
  }

  public int getPort() {
    return serverSocket.getLocalPort();
  }

  @Override
  public String getUrlForFile(File file) {
    return getUrlForUri(file.toURI());
  }

  @Override
  public String getUrlForResource(IResource resource) {
    URI fileUri = resource.getLocation().toFile().toURI();

    return getUrlForUri(fileUri);
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

  protected String getAvailableAppsContent() throws IOException {
    InputStream in = ResourceServer.class.getResourceAsStream("template.html");

    String template = CharStreams.toString(new InputStreamReader(in));

    List<HTMLFile> appFiles = getAllHtmlFiles();

    // Sort by project name, then html file name
    Collections.sort(appFiles, new Comparator<HTMLFile>() {
      @Override
      public int compare(HTMLFile o1, HTMLFile o2) {
        String str1 = o1.getDartProject().getElementName() + " " + o1.getElementName();
        String str2 = o2.getDartProject().getElementName() + " " + o2.getElementName();

        return str1.compareToIgnoreCase(str2);
      }
    });

    if (appFiles.size() == 0) {
      template = replaceTemplate(template, "count", "No");
      template = replaceTemplate(template, "apps", "");
    } else {
      template = replaceTemplate(template, "count", Integer.toString(appFiles.size()));

      StringBuilder builder = new StringBuilder();

      for (HTMLFile htmlFile : appFiles) {
        try {
          if (htmlFile.getReferencedLibraries().length < 1) {
            continue;
          }

          IResource htmlResource = htmlFile.getCorrespondingResource();
          DartLibrary library = htmlFile.getReferencedLibraries()[0];
          IResource libraryResource = library.getCorrespondingResource();

          String href = "<a href=\"" + getPathFor(htmlFile) + "\">";

          builder.append("<div class=\"app\"><table><tr>");
          builder.append("<td rowspan=2>" + href
              + "<img src=\"dart_32_32.gif\" width=32 height=32></a></td>");
          builder.append("<td class=\"title\">" + htmlResource.getProject().getName() + " - "
              + href + htmlFile.getElementName() + "</a></td</tr>");
          builder.append("<tr><td class=\"info\">"
              + webSafe(libraryResource.getFullPath().toOSString()) + "</td></tr>");
          builder.append("</table></div>");
        } catch (DartModelException dme) {

        }
      }

      template = replaceTemplate(template, "apps", builder.toString());
    }

    return template;
  }

  protected void loadingContentFrom(String hostAddress, String userAgent) {
    if (!previousAgents.contains(userAgent)) {
      previousAgents.add(userAgent);

      DartCore.getConsole().println(
          "Remote connection from " + hostAddress + " [" + userAgent + "]");
    }
  }

  private List<HTMLFile> getAllHtmlFiles() {
    Set<HTMLFile> files = new HashSet<HTMLFile>();

    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      DartProject dartProject = DartCore.create(project);

      if (dartProject != null) {
        try {
          for (DartLibrary library : dartProject.getDartLibraries()) {
            List<HTMLFile> htmlFiles = library.getChildrenOfType(HTMLFile.class);

            files.addAll(htmlFiles);
          }
        } catch (DartModelException ex) {

        }
      }
    }

    return new ArrayList<HTMLFile>(files);
  }

  private String getPathFor(HTMLFile htmlFile) throws IOException {
    try {
      String url = getUrlForResource(htmlFile.getCorrespondingResource());

      return URI.create(url).getPath();
    } catch (DartModelException ex) {
      throw new IOException(ex);
    }
  }

  private String getUrlForUri(URI fileUri) {
    try {
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

  private String replaceTemplate(String template, String target, String replace) {
    target = "${" + target + "}";

    int index = template.indexOf(target);

    String template1 = template.substring(0, index);
    String template2 = template.substring(index + target.length());

    return template1 + replace + template2;
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

  private String webSafe(String s) {
    StringBuffer out = new StringBuffer();

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      if (c > 127 || c == '"' || c == '<' || c == '>') {
        out.append("&#" + (int) c + ";");
      } else {
        out.append(c);
      }
    }

    return out.toString();
  }

}
