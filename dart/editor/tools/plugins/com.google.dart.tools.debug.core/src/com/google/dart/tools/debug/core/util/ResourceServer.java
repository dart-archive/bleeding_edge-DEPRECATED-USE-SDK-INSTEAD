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
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

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

  @Override
  public String getUrlRegexForResource(IResource resource) {
    IProject project = resource.getProject();
    File projectDir = project.getLocation().toFile();

    return projectDir.getName() + "/" + resource.getProjectRelativePath().toPortableString();
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

    List<IFile> files = getAllHtmlFiles();

    // Sort by project name, then html file name
    Collections.sort(files, new Comparator<IFile>() {
      @Override
      public int compare(IFile o1, IFile o2) {
        String str1 = o1.getFullPath().toPortableString();
        String str2 = o2.getFullPath().toPortableString();

        return str1.compareToIgnoreCase(str2);
      }
    });

    if (files.size() == 0) {
      template = replaceTemplate(template, "count", "No");
      template = replaceTemplate(template, "apps", "");
    } else {
      template = replaceTemplate(template, "count", Integer.toString(files.size()));

      StringBuilder builder = new StringBuilder();

      for (IFile file : files) {
        String hrefStart = "<a href=\"" + getPathFor(file) + "\">";

        builder.append("<div class=\"app\"><table><tr>");
        builder.append("<td rowspan=2>" + hrefStart
            + "<img src=\"dart_16_16.gif\" width=16 height=16></a></td>");
        builder.append("<td class=\"title\">" + hrefStart
            + webSafe(file.getFullPath().toOSString()) + "</a></td</tr>");
        builder.append("</table></div>");
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

  private List<IFile> getAllHtmlFiles() {
    final List<IFile> files = new ArrayList<IFile>();

    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (DartProjectNature.hasDartNature(project)) {
        try {
          project.accept(new IResourceVisitor() {
            @Override
            public boolean visit(IResource resource) throws CoreException {
              if (resource instanceof IFile && DartCore.isHTMLLikeFileName(resource.getName())) {
                files.add((IFile) resource);
              }

              return true;
            }
          });
        } catch (CoreException e) {

        }
      }
    }

    return files;
  }

  private String getPathFor(IFile file) throws IOException {
    String url = getUrlForResource(file);

    return URI.create(url).getPath();
  }

  private String getUrlForUri(URI fileUri) {
    try {
      String pathSegment = fileUri.getPath();

      URI uri = new URI(
          "http",
          null,
          NetUtils.getLoopbackAddress(),
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
