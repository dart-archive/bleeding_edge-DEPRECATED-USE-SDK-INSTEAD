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

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    public static final String METHOD_POST = "POST";

    private static final String RANGE = "Range";

    public String method;
    public String file;
    public String version;

    public Map<String, String> headers = new LinkedHashMap<String, String>();

    public int getContentLength() {
      String len = headers.get(CONTENT_LENGTH);

      try {
        return len == null ? -1 : Integer.parseInt(len);
      } catch (NumberFormatException nfe) {
        return -1;
      }
    }

    public List<int[]> getRanges() {
      // Range: bytes=0-99,500-1499,4000-
      if (headers.containsKey(RANGE)) {
        String rangeStr = headers.get(RANGE);

        if (rangeStr.startsWith("bytes=")) {
          rangeStr = rangeStr.substring("bytes=".length());

          String[] strs = rangeStr.split(",");

          List<int[]> result = new ArrayList<int[]>();

          for (String str : strs) {
            int index = str.indexOf('-');

            try {
              if (index == 0) {
                result.add(new int[] {0, Integer.parseInt(str.substring(1))});
              } else if (index == str.length() - 1) {
                result.add(new int[] {Integer.parseInt(str.substring(0, index)), -1});
              } else if (index != -1) {
                result.add(new int[] {
                    Integer.parseInt(str.substring(0, index)),
                    Integer.parseInt(str.substring(index + 1))});
              }
            } catch (NumberFormatException nfe) {

            }
          }

          return result;
        }
      }

      return null;
    }

    @Override
    public String toString() {
      return "[" + method + " " + file + " " + version + "]";
    }

    void parseGetParams() {
      // Check for a GET request that contains getter parameters.
      if (file != null && file.indexOf('?') != -1) {
        @SuppressWarnings("unused")
        String params = file.substring(file.indexOf('?') + 1);

        file = file.substring(0, file.indexOf('?'));
      }
    }
  }

  private static class HttpResponse {
    public static final int OK = 200; // "OK"
    public static final int PARTIAL_CONTENT = 206; // "Partial Content"
    public static final int NOT_FOUND = 404; // "Not Found"
    public static final int UNAUTHORIZED = 401; // "Unauthorized"

    public static final DateFormat RFC_1123_DATE_FORMAT = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z",
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

  public static final String CONTENT_LENGTH = "Content-Length";

  private static final String TYPE_OCTET = "application/octet-stream";

  private static final String TYPE_HTML = "text/html";
  private static final String TYPE_PLAIN = "text/plain";
  private static final String TYPE_CSS = "text/css";
  private static final String TYPE_JS = "text/javascript";
  private static final String TYPE_DART = "application/dart";
  private static final String TYPE_XML = "text/xml";

  private static final String TYPE_JPEG = "image/jpeg";
  private static final String TYPE_GIF = "image/gif";
  private static final String TYPE_PNG = "image/png";

  private static Map<String, String> contentMappings = new HashMap<String, String>();
  private static Map<String, String> extraMappings;

  static {
    contentMappings.put("htm", TYPE_HTML);
    contentMappings.put("html", TYPE_HTML);
    contentMappings.put("txt", TYPE_PLAIN);
    contentMappings.put("css", TYPE_CSS);
    contentMappings.put("js", TYPE_JS);
    contentMappings.put("xml", TYPE_XML);

    contentMappings.put("dart", TYPE_DART);

    contentMappings.put("jpeg", TYPE_JPEG);
    contentMappings.put("jpg", TYPE_JPEG);
    contentMappings.put("gif", TYPE_GIF);
    contentMappings.put("png", TYPE_PNG);

    setupExtraMappings();
  }

  private static final String CRLF = "\r\n";

  private static byte[] AGENT_CONTENT;

  /**
   * Special resources to serve - i.e. non-workspace resources.
   */
  private static final String[][] embeddedResources = new String[][] {
      {"/favicon.ico", TYPE_GIF, "/resources/dart_16_16.gif"},
      {"/dart_32_32.gif", TYPE_GIF, "/resources/dart_32_32.gif"},
      {"/agent.html", TYPE_HTML, "agent.html"}, {"/agent.js", TYPE_JS, "agent.js"}};

  private static byte[] getJSAgentContent() {
    if (AGENT_CONTENT == null) {
      try {
        AGENT_CONTENT = ByteStreams.toByteArray(ResourceServer.class.getResourceAsStream("agent.js"));
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);

        AGENT_CONTENT = new byte[0];
      }
    }

    return AGENT_CONTENT;
  }

  private static void setupExtraMappings() {
    extraMappings = new HashMap<String, String>();

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          ResourceServerHandler.class.getResourceAsStream("mime.txt")));

      String line = reader.readLine();

      while (line != null) {
        String[] strs = line.split(" ");

        extraMappings.put(strs[0], strs[1]);

        line = reader.readLine();
      }

      reader.close();
    } catch (IOException ioe) {
      DartDebugCorePlugin.logError(ioe);
    }
  }

  private ResourceServer resourceServer;

  private Socket socket;

  public ResourceServerHandler(ResourceServer resourceServer, Socket socket) {
    this.resourceServer = resourceServer;
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      DataInputStream in = new DataInputStream(socket.getInputStream());

      HttpHeader header = parseHeader(in);

      if (header == null) {
        safeClose(socket);
      } else if (isAllowableConnection(socket, header)) {
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

      // ignore java.net.SocketException: Connection reset
      if (!(ioe instanceof ConnectException) && !isConnectionReset(ioe)) {
        DartDebugCorePlugin.logError(ioe);
      }
    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);
    }
  }

  private void addStandardResponseHeaders(HttpResponse response) {
    response.headers.put("Server", "Dart Editor v" + DartCore.getVersion());
    response.headers.put("Connection", "close");
  }

  /**
   * Restrict the files which are legal to serve.
   * 
   * @param file
   * @return
   */
  private boolean canServeFile(File file) {
    if (file.getName().startsWith(".")) {
      return false;
    }

    File parentFile = file.getParentFile();

    return parentFile == null ? true : canServeFile(parentFile);
  }

  private HttpResponse createErrorResponse(String message) {
    HttpResponse response = new HttpResponse();

    response.responseCode = HttpResponse.NOT_FOUND;
    response.responseText = "Not Found";

    response.headers.put("Content-Type", "text/html");
    response.responseBodyText = "<html><head><title>404 Not Found</title></head><body>" + message
        + "</body></html>";
    response.headers.put(CONTENT_LENGTH, Integer.toString(response.responseBodyText.length()));

    addStandardResponseHeaders(response);

    return response;
  }

  private HttpResponse createGETResponse(HttpHeader header) throws IOException {
    boolean headOnly = false;
    byte[] javaScriptContent = null;

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

      javaScriptContent = getCombinedContentAndAgent(javaFile);
    }

    HttpResponse response = new HttpResponse();

    try {
      // Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
      Date date = new Date(javaFile.lastModified());
      response.headers.put("Last-Modified", HttpResponse.RFC_1123_DATE_FORMAT.format(date));
    } catch (Throwable t) {
      // Some (bad?) file times can cause exceptions to be thrown from the formatter. 
      DartDebugCorePlugin.logError(t);
    }

    // Content-Type: text/html[; charset=UTF-8]
    String contentType = getContentType(getFileExtension(javaFile.getName()));
    response.headers.put("Content-Type", contentType);

    // Cache-control: no-cache
    response.headers.put("Cache-control", "no-cache");

    // Content-Length: 438
    if (javaFile != null) {
      long length = javaFile.length();

      if (javaScriptContent != null) {
        length = javaScriptContent.length;
      }

      response.headers.put(CONTENT_LENGTH, Long.toString(length));
    }

    if (!headOnly) {
      if (javaScriptContent != null) {
        response.responseBodyStream = new ByteArrayInputStream(javaScriptContent);
      } else {
        List<int[]> ranges = header.getRanges();

        if (ranges != null) {
          byte[] rangeData = readRangeData(javaFile, ranges);

          response.responseBodyStream = new ByteArrayInputStream(rangeData);

          response.responseCode = HttpResponse.PARTIAL_CONTENT;
          response.responseText = "Partial Content";

          response.headers.put(CONTENT_LENGTH, Long.toString(rangeData.length));
          // Content-Range: bytes X-Y/Z
          int[] range = ranges.get(0);
          response.headers.put("Content-Range", "bytes " + range[0] + "-" + range[1] + "/"
              + rangeData.length);
        } else {
          response.responseBodyStream = new FileInputStream(javaFile);
        }

        // Indicate that we support requesting a subset of the document.
        response.headers.put("Accept-Ranges", "bytes");
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
    response.headers.put(CONTENT_LENGTH, Integer.toString(response.responseBodyText.length()));

    addStandardResponseHeaders(response);

    return response;
  }

  private HttpResponse createPOSTResponse(HttpHeader header, DataInputStream in) throws IOException {
    // This 200000 value matches Jetty's setting.
    final int MAX_POST = 200000 + 100;

    String file = header.file;

    int length = header.getContentLength();

    if (length > MAX_POST) {
      return createErrorResponse("Invalid POST length");
    }

    byte[] data = new byte[length];

    in.readFully(data);

    String str = new String(data);
    str = URLDecoder.decode(str, "UTF-8");

    if ("/log".equals(file)) {
      handleLoggingPost(str);
    }

    HttpResponse response = new HttpResponse();

    addStandardResponseHeaders(response);

    return response;
  }

  /**
   * Combine the given *.dart.js file and the debugger JS agent.
   * 
   * @param dartJsFile
   * @return
   * @throws IOException
   */
  private byte[] getCombinedContentAndAgent(File dartJsFile) throws IOException {
    // If we can find the source map token, then insert our debugger agent just before it.
    // Otherwise, append the debugger agent to the end of the file content.

    final String SRC_MAP_TOKEN = "//@ sourceMappingURL=";

    String content = Files.toString(dartJsFile, Charsets.UTF_8);
    String agent = new String(getJSAgentContent());

    if (content.indexOf(SRC_MAP_TOKEN) != -1) {
      int index = content.indexOf(SRC_MAP_TOKEN);

      content = content.substring(0, index) + agent + content.substring(index);
    } else {
      content += agent;
    }

    return content.getBytes(Charsets.UTF_8);
  }

  private String getContentType(String extension) {
    if (extension != null) {
      extension = extension.toLowerCase();

      if (contentMappings.containsKey(extension)) {
        return contentMappings.get(extension);
      }

      if (extraMappings.containsKey(extension)) {
        return extraMappings.get(extension);
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

  private void handleLoggingPost(String data) throws IOException {
    try {
      JSONObject obj = new JSONObject(data);

      // {"response":["\"logging from sunflower\""],"cmd":"remote console.log","type":""}

      if (obj.has("message")) {
        JSONArray arr = obj.optJSONArray("message");

        if (arr != null) {
          for (int i = 0; i < arr.length(); i++) {
            DartCore.getConsole().println(stripQuotes(arr.getString(i)));
          }
        } else {
          String log = obj.getString("message");

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

  private boolean isConnectionReset(IOException ioe) {
    // ignore java.net.SocketException: Connection reset

    if (ioe instanceof SocketException) {
      return "Connection reset".equals(ioe.getMessage());
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

  @SuppressWarnings("deprecation")
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

        header.parseGetParams();
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

  private byte[] readRangeData(File file, List<int[]> ranges) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    RandomAccessFile f = new RandomAccessFile(file, "r");

    int maxLength = (int) f.length() - 1;

    for (int[] range : ranges) {
      if (range[1] > maxLength || range[1] == -1) {
        range[1] = maxLength;
      }

      if (range[0] >= range[1]) {
        continue;
      }

      f.seek(range[0]);

      int count = range[1] - range[0] + 1;

      byte[] temp = new byte[count];

      f.readFully(temp);

      out.write(temp);
    }

    return out.toByteArray();
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

    String content = resourceServer.getAvailableAppsContent();
    byte[] bytes = content.getBytes("UTF-8");

    response.headers.put(CONTENT_LENGTH, Integer.toString(bytes.length));
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

        response.headers.put(CONTENT_LENGTH, Integer.toString(conn.getContentLength()));
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
