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

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.HTMLFile;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// GET /index.html HTTP/1.1
// Host: www.example.com

// HTTP/1.1 200 OK
// Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)
// Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
// Content-Length: 438
// Connection: close
// Content-Type: text/html; charset=UTF-8

/**
 * Handles an incoming http request, serving files from the workspace (or error pages) as necessary.
 */
class ResourceServerHandler implements Runnable {
  private static class HttpHeader {
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_POST = "POST";

    public String method;
    public String file;
    public String version;

    public Map<String, String> headers = new LinkedHashMap<String, String>();

    public int getContentLength() {
      String len = headers.get("Content-Length");

      try {
        return len == null ? -1 : Integer.parseInt(len);
      } catch (NumberFormatException nfe) {
        return -1;
      }
    }

    @Override
    public String toString() {
      return "[" + method + " " + file + " " + version + "]";
    }
  }

  private static class HttpResponse {
    public static final int OK = 200; // "OK"
    public static final int NOT_FOUND = 404; // "Not Found"
    public static final int UNAUTHORIZED = 401; // "Unauthorized"

    public static final DateFormat RFC_1123_DATE_FORMAT = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        Locale.US);

    public int responseCode = OK;
    public String responseText = "OK";

    public Map<String, String> headers = new LinkedHashMap<String, String>();

    // one or the other needs to be null
    public String responseBodyText;
    public InputStream responseBodyStream;

    @Override
    public String toString() {
      return "[" + responseCode + " " + responseText + "]";
    }
  }

  private static final String ISO_8859_1 = "ISO-8859-1";
  private static final String US_ASCII = "US-ASCII";

  private static final String TYPE_OCTET = "application/octet-stream";

  private static final String TYPE_HTML = "text/html";
  private static final String TYPE_PLAIN = "text/plain";
  private static final String TYPE_CSS = "text/css";
  private static final String TYPE_JS = "text/javascript";
  private static final String TYPE_XML = "text/xml";

  private static final String TYPE_JPEG = "image/jpeg";
  private static final String TYPE_GIF = "image/gif";
  private static final String TYPE_PNG = "image/png";

  private static Map<String, String> contentMappings = new HashMap<String, String>();

  static {
    contentMappings.put("htm", TYPE_HTML);
    contentMappings.put("html", TYPE_HTML);
    contentMappings.put("txt", TYPE_PLAIN);
    contentMappings.put("css", TYPE_CSS);
    contentMappings.put("js", TYPE_JS);
    contentMappings.put("xml", TYPE_XML);

    contentMappings.put("dart", TYPE_PLAIN);

    contentMappings.put("jpeg", TYPE_JPEG);
    contentMappings.put("jpg", TYPE_JPEG);
    contentMappings.put("gif", TYPE_GIF);
    contentMappings.put("png", TYPE_PNG);
  }

  private static final String CRLF = "\r\n";

  /**
   * Special resources to serve - i.e. non-workspace resources.
   */
  private static final String[][] embeddedResources = new String[][] {
      {"/favicon.ico", TYPE_GIF, "/resources/dart_16_16.gif"},
      {"/agent.html", TYPE_HTML, "agent.html"}, {"/agent.js", TYPE_JS, "agent.js"}};

  private ResourceServer resourceServer;
  private Socket socket;

  public ResourceServerHandler(ResourceServer resourceServer, Socket socket) {
    this.resourceServer = resourceServer;
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
//      BufferedReader reader = new BufferedReader(new InputStreamReader(
//          socket.getInputStream(),
//          ISO_8859_1));
      DataInputStream in = new DataInputStream(socket.getInputStream());

      HttpHeader header = parseHeader(in);

      if (header != null && isAllowableConnection(socket, header)) {
        HttpResponse response;

        if (HttpHeader.METHOD_GET.equals(header.method)
            || HttpHeader.METHOD_HEAD.equals(header.method)) {
          response = createGETResponse(header);
        } else if (HttpHeader.METHOD_POST.equals(header.method)) {
          response = createPOSTResponse(header, in);
        } else {
          response = createErrorResponse("Request type " + header.method + " not supported.");
        }

        sendResponse(response);
      } else {
        sendResponse(createNotAllowedResponse());
      }

      socket.close();
    } catch (IOException ioe) {
      safeClose(socket);

      DartDebugCorePlugin.logError(ioe);
    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);
    }
  }

  private void addStandardResponseHeaders(HttpResponse response) {
    response.headers.put("Server", "Dart Editor v" + DartCore.getVersion());
    response.headers.put("Connection", "close");
  }

  private boolean canServeFile(File file) {
    if (file.getName().startsWith(".")) {
      return false;
    }

    // TODO(devoncarew): restrict the set of allowable root directories

    return true;
  }

  private HttpResponse createErrorResponse(String message) {
    HttpResponse response = new HttpResponse();

    response.responseCode = HttpResponse.NOT_FOUND;
    response.responseText = "Not Found";

    response.headers.put("Content-Type", "text/html");
    response.responseBodyText = "<html><head><title>404 Not Found</title></head><body>" + message
        + "</body></html>";
    response.headers.put("Content-Length", Integer.toString(response.responseBodyText.length()));

    addStandardResponseHeaders(response);

    return response;
  }

  private HttpResponse createGETResponse(HttpHeader header) throws IOException {
    boolean headOnly = false;
    boolean appendJSAgent = false;

    if (HttpHeader.METHOD_HEAD.equals(header.method)) {
      headOnly = true;
    }

    if (header.file == null || header.file.length() == 0) {
      return createErrorResponse("No file specified.");
    }

    if ("/".equals(header.file)) {
      return serveAvailableApps(header);
    }

    if (isSpecialResource(header.file)) {
      return serveEmbeddedResource(header.file, headOnly);
    }

    File javaFile = locateFile(header.file);

    // If a .dart.js file doesn't exist, check for a .dart file next to it.
    if (javaFile == null && header.file.endsWith(".dart.js")) {
      String dartFilePath = header.file.substring(0, header.file.length() - 3);
      File dartFile = locateFile(dartFilePath);

      if (dartFile != null) {
        javaFile = new File(dartFile.getPath() + ".js");
      }
    }

    if (javaFile == null) {
      return createErrorResponse("File not found: " + header.file);
    }

    if (!canServeFile(javaFile)) {
      return createErrorResponse("File not found: " + header.file);
    }

    if (isFileJsArtifact(javaFile)) {
      CompilationServer.getServer().recompileJavaScriptArtifact(javaFile);

      // If the compilation failed, return a FNF response.
      if (!javaFile.exists()) {
        return createErrorResponse("File not found: " + header.file);
      }

      appendJSAgent = true;
    }

    HttpResponse response = new HttpResponse();

    // Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
    Date date = new Date(javaFile.lastModified());
    response.headers.put("Last-Modified", HttpResponse.RFC_1123_DATE_FORMAT.format(date));

    // Content-Type: text/html; charset=UTF-8
    String contentType = getContentType(getFileExtension(javaFile.getName()));

//      if (contentType.startsWith("text/")) {
//        response.headers.put("Content-Type", contentType + "; charset=" + file.getCharset());
//      } else {
    response.headers.put("Content-Type", contentType);
//      }

    // Cache-control: no-cache
    response.headers.put("Cache-control", "no-cache");

    // Content-Length: 438
    if (javaFile != null) {
      long length = javaFile.length();

      if (appendJSAgent) {
        length += getJSAgentContent().length;
      }

      response.headers.put("Content-Length", Long.toString(length));
    }

    if (!headOnly) {
      if (appendJSAgent) {
        response.responseBodyStream = new SequenceInputStream(
            new FileInputStream(javaFile),
            new ByteArrayInputStream(getJSAgentContent()));
      } else {
        response.responseBodyStream = new FileInputStream(javaFile);
      }
    }

    addStandardResponseHeaders(response);

    return response;
  }

  private HttpResponse createNotAllowedResponse() {
    HttpResponse response = new HttpResponse();

    response.responseCode = HttpResponse.UNAUTHORIZED;
    response.responseText = "Unauthorized";

    response.headers.put("Content-Type", "text/html");
    response.responseBodyText = "<html><head><title>401 Unauthorized</title></head><body>User agent not allowed.</body></html>";
    response.headers.put("Content-Length", Integer.toString(response.responseBodyText.length()));

    addStandardResponseHeaders(response);

    return response;
  }

  private HttpResponse createPOSTResponse(HttpHeader header, DataInputStream in) throws IOException {
    String file = header.file;

    int length = header.getContentLength();

    if (length > 20000) {
      return createErrorResponse("Invalid POST length");
    }

    byte[] data = new byte[length];

    in.readFully(data);

    String str = new String(data);
    str = URLDecoder.decode(str);

    if ("/log".equals(file)) {
      handleLoggingPost(str);
    }

    HttpResponse response = new HttpResponse();

    addStandardResponseHeaders(response);

    return response;
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

  private String getAvailableAppsContent() throws IOException {
    StringBuilder builder = new StringBuilder();

    InputStream in = ResourceServer.class.getResourceAsStream("template.html");

    String template = CharStreams.toString(new InputStreamReader(in));

    final String appsTag = "${apps}";

    int index = template.indexOf(appsTag);

    String template1 = template.substring(0, index);
    String template2 = template.substring(index + appsTag.length());

    builder.append(template1);

    List<HTMLFile> appFiles = getAllHtmlFiles();

    Collections.sort(appFiles, new Comparator<HTMLFile>() {
      @Override
      public int compare(HTMLFile o1, HTMLFile o2) {
        return o1.getElementName().compareToIgnoreCase(o2.getElementName());
      }
    });

    if (appFiles.size() == 0) {
      builder.append("The editor does not contain any web applications.");
    } else {
      for (HTMLFile appFile : appFiles) {
        builder.append("<div class=\"app\">");
        builder.append("<a href=\"" + getPathFor(appFile) + "\">");
        builder.append("<img src=\"favicon.ico\" width=16 height=16>");
        builder.append("</a>&nbsp;");
        builder.append("<a href=\"" + getPathFor(appFile) + "\">");
        builder.append(appFile.getElementName());
        builder.append("</a>");
        builder.append("</div>");
      }
    }

    builder.append(template2);

    return builder.toString();
  }

  private String getContentType(String extension) {
    if (extension != null) {
      extension = extension.toLowerCase();

      if (contentMappings.containsKey(extension)) {
        return contentMappings.get(extension);
      }
    }

    return TYPE_OCTET;
  }

  private String getFileExtension(String name) {
    int index = name.lastIndexOf('.');

    if (index != -1) {
      return name.substring(index + 1);
    } else {
      return null;
    }
  }

  private byte[] getJSAgentContent() throws IOException {
    return ByteStreams.toByteArray(ResourceServer.class.getResourceAsStream("agent.js"));
  }

  private String getPathFor(HTMLFile htmlFile) throws IOException {
    try {
      String url = resourceServer.getUrlForResource((IFile) htmlFile.getCorrespondingResource());

      return URI.create(url).getPath();
    } catch (DartModelException ex) {
      throw new IOException(ex);
    }
  }

  private void handleLoggingPost(String data) throws IOException {
    try {
      JSONObject obj = new JSONObject(data);

      // {"response":["\"logging from sunflower\""],"cmd":"remote console.log","type":""}

      if (obj.has("response")) {
        JSONArray arr = obj.optJSONArray("response");

        if (arr != null) {
          for (int i = 0; i < arr.length(); i++) {
            DartCore.getConsole().println(stripQuotes(arr.getString(i)));
          }
        } else {
          String log = obj.getString("response");

          DartCore.getConsole().println(log);
        }
      }
    } catch (JSONException ex) {
      throw new IOException(ex);
    }
  }

  private boolean isAllowableConnection(Socket connection, HttpHeader header) {
    InetAddress remoteAddress = connection.getInetAddress();

    if (isLocalAddress(remoteAddress)) {
      return true;
    }

    // User-Agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1110.0 (Dart) Safari/536.8
    if (DartDebugCorePlugin.getPlugin().getUserAgentManager() != null) {
      String userAgent = header.headers.get("User-Agent");

      boolean allowed = DartDebugCorePlugin.getPlugin().getUserAgentManager().allowUserAgent(
          remoteAddress,
          userAgent);

      if (allowed) {
        resourceServer.loadingContentFrom(remoteAddress.getHostAddress(), userAgent);
      }

      return allowed;
    }

    return false;
  }

  private boolean isFileJsArtifact(File javaFile) {
    return javaFile.getName().endsWith(".dart.js");
  }

  private boolean isLocalAddress(InetAddress address) {
    return address.isAnyLocalAddress() || address.isLoopbackAddress();
  }

  private boolean isSpecialResource(String path) {
    for (String[] resourceInfo : embeddedResources) {
      if (resourceInfo[0].equals(path)) {
        return true;
      }
    }

    return false;
  }

  private File locateAbsoluteFile(String path) {
    File file = new File(path);

    if (file.exists() && !file.isDirectory()) {
      return file;
    } else {
      return null;
    }
  }

  private File locateFile(String filePath) {
    File javaFile = null;

    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(filePath));

    if (resource instanceof IFile) {
      IFile file = (IFile) resource;

      if (file.getRawLocation() != null) {
        javaFile = file.getRawLocation().toFile();
      }
    } else if (resource == null) {
      javaFile = locateAbsoluteFile(filePath);
    }

    return javaFile;
  }

  private HttpHeader parseHeader(DataInputStream in) throws IOException {
    HttpHeader header = new HttpHeader();

    String line = in.readLine();

    if (line == null) {
      return null;
    }

    // GET /index.html HTTP/1.1

    String[] strs = line.split(" ");

    if (strs.length > 2) {
      header.method = strs[0];
      header.file = strs[1];
      header.version = strs[2];

      if (header.file != null) {
        header.file = URLDecoder.decode(header.file, ISO_8859_1);
      }
    }

    line = in.readLine();

    while (line != null) {
      if (line.isEmpty()) {
        break;
      }

      int index = line.indexOf(':');

      if (index == -1) {
        header.headers.put(line, "");
      } else {
        header.headers.put(line.substring(0, index), line.substring(index + 1).trim());
      }

      line = in.readLine();
    }

    return header;
  }

  private void safeClose(Socket socket) {
    try {
      socket.close();
    } catch (IOException e) {

    }
  }

  private void sendResponse(HttpResponse response) throws IOException {
    OutputStream out = socket.getOutputStream();

    StringBuilder builder = new StringBuilder();

    // HTTP/1.0 200 OK
    builder.append("HTTP/1.0 " + response.responseCode + " " + response.responseText + CRLF);

    for (String key : response.headers.keySet()) {
      builder.append(key + ": " + response.headers.get(key) + CRLF);
    }

    builder.append(CRLF);

    out.write(builder.toString().getBytes(ISO_8859_1));

    if (response.responseBodyText != null) {
      out.write(response.responseBodyText.getBytes(US_ASCII));
    } else if (response.responseBodyStream != null) {
      byte[] buffer = new byte[2048];

      int count = response.responseBodyStream.read(buffer);

      while (count != -1) {
        out.write(buffer, 0, count);

        count = response.responseBodyStream.read(buffer);
      }

      response.responseBodyStream.close();
    }

    out.flush();
    out.close();
  }

  private HttpResponse serveAvailableApps(HttpHeader header) throws IOException {
    HttpResponse response = new HttpResponse();

    String content = getAvailableAppsContent();
    byte[] bytes = content.getBytes("UTF-8");

    response.headers.put("Content-Length", Integer.toString(bytes.length));
    response.headers.put("Content-Type", "text/html; charset=UTF-8");
    response.headers.put("Cache-control", "no-cache");
    response.responseBodyStream = new ByteArrayInputStream(bytes);

    addStandardResponseHeaders(response);

    return response;
  }

  private HttpResponse serveEmbeddedResource(String path, boolean headOnly) throws IOException {
    for (String[] resourceInfo : embeddedResources) {
      if (resourceInfo[0].equals(path)) {
        HttpResponse response = new HttpResponse();

        URL url = ResourceServerHandler.class.getResource(resourceInfo[2]);

        URLConnection conn = url.openConnection();

        response.headers.put("Content-Length", Integer.toString(conn.getContentLength()));
        response.headers.put("Content-Type", resourceInfo[1]);
        response.headers.put("Cache-control", "no-cache");

        if (!headOnly) {
          response.responseBodyStream = conn.getInputStream();
        }

        addStandardResponseHeaders(response);

        return response;
      }
    }

    return null;
  }

  private String stripQuotes(String str) {
    if (str.length() > 1 && str.startsWith("\"") && str.endsWith("\"")) {
      str = str.substring(1, str.length() - 1);

      // TODO(devoncarew): better handling of escaped values in the string ('\')
      str = str.replaceAll("\\\\\"", "\"");

      return str;
    } else {
      return str;
    }
  }

}
