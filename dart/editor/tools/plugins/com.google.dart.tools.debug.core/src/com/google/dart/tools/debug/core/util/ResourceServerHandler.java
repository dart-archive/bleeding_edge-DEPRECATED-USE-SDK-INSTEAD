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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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

    public String method;
    public String file;
    public String version;

    public Map<String, String> headers = new LinkedHashMap<String, String>();

    @Override
    public String toString() {
      return "[" + method + " " + file + " " + version + "]";
    }
  }

  private static class HttpResponse {
    public static final int OK = 200; // "OK"
    public static final int NOT_FOUND = 404; // "Not Found"

    public static final DateFormat RFC_1123_DATE_FORMAT = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

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
  private static final String[][] embeddedResources = new String[][] {{
      "/favicon.ico", TYPE_GIF, "/resources/dart_16_16.gif"}};

  private Socket socket;

  public ResourceServerHandler(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      if (isLocalAddress()) {
        HttpHeader header = parseHeader();

        if (header != null) {
          HttpResponse response = createResponse(header);

          sendResponse(response);
        }
      }

      socket.close();
    } catch (IOException ioe) {
      safeClose(socket);

      DartDebugCorePlugin.logError(ioe);
    }
  }

  private void addStandardResponseHeaders(HttpResponse response) {
    response.headers.put("Server", "Dart Editor v" + DartCore.getVersion());
    response.headers.put("Connection", "close");
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

  private HttpResponse createResponse(HttpHeader header) throws IOException {
    boolean headOnly = false;

    if (HttpHeader.METHOD_HEAD.equals(header.method)) {
      headOnly = true;
    } else if (!HttpHeader.METHOD_GET.equals(header.method)) {
      return createErrorResponse("Only GET requests are understood.");
    }

    if (header.file == null || header.file.length() == 0) {
      return createErrorResponse("No file specified.");
    }

    if (isSpecialResource(header.file)) {
      return serveEmbeddedResource(header.file, headOnly);
    }

    File javaFile = null;

    IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(header.file));

    if (resource instanceof IFile) {
      IFile file = (IFile) resource;

      if (file.getRawLocation() != null) {
        javaFile = file.getRawLocation().toFile();
      }
    } else if (resource == null) {
      javaFile = locateAbsoluteFile(header.file);
    }

    if (javaFile == null) {
      return createErrorResponse("File not found: " + header.file);
    }

    HttpResponse response = new HttpResponse();

    // Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
    Date date = new Date(javaFile.lastModified());
    response.headers.put("Last-Modified", HttpResponse.RFC_1123_DATE_FORMAT.format(date));

    // Content-Length: 438
    if (javaFile != null) {
      response.headers.put("Content-Length", Long.toString(javaFile.length()));
    }

    // Content-Type: text/html; charset=UTF-8
    String contentType = getContentType(getFileExtension(javaFile.getName()));

//      if (contentType.startsWith("text/")) {
//        response.headers.put("Content-Type", contentType + "; charset=" + file.getCharset());
//      } else {
    response.headers.put("Content-Type", contentType);
//      }

    // Cache-control: no-cache
    response.headers.put("Cache-control", "no-cache");

    if (!headOnly) {
      response.responseBodyStream = new FileInputStream(javaFile);
    }

    addStandardResponseHeaders(response);

    return response;
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

  private boolean isLocalAddress() {
    InetAddress remoteAddress = socket.getInetAddress();

    return remoteAddress.isAnyLocalAddress() || remoteAddress.isLoopbackAddress();
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

  private HttpHeader parseHeader() throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
        ISO_8859_1));

    HttpHeader header = new HttpHeader();

    String line = reader.readLine();

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

    line = reader.readLine();

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

      line = reader.readLine();
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
}
