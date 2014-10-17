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
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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
      String len = getHeaderKey(CONTENT_LENGTH);

      try {
        return len == null ? -1 : Integer.parseInt(len);
      } catch (NumberFormatException nfe) {
        return -1;
      }
    }

    /**
     * Return the value for the given key; assume case-insensitively for the key.
     * 
     * @param key
     * @return
     */
    public String getHeaderKey(String key) {
      String value = headers.get(key);

      if (value != null) {
        return value;
      }

      for (String k : headers.keySet()) {
        if (k.equalsIgnoreCase(key)) {
          return headers.get(k);
        }
      }

      return null;
    }

    public List<int[]> getRanges() {
      // Range: bytes=0-99,500-1499,4000-
      if (headers.containsKey(RANGE)) {
        String rangeStr = getHeaderKey(RANGE);

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
    public static final int REDIRECT = 302; // "Found"
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

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String CACHE_CONTROL = "Cache-Control";
  private static final String USER_AGENT = "User-Agent";
  private static final String ACCEPT_RANGES = "Accept-Ranges";
  private static final String CONTENT_RANGE = "Content-Range";
  private static final String LAST_MODIFIED = "Last-Modified";

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
      {"/favicon.ico", TYPE_GIF, "/resources/favicon.ico"},
      {"/dart_16_16.gif", TYPE_GIF, "/resources/dart_16_16.gif"},
      {"/dart_32_32.gif", TYPE_GIF, "/resources/dart_32_32.gif"},
      {"/agent.html", TYPE_HTML, "agent.html"},
      {"/agent.js", TYPE_JS, "agent.js"},
      {"/apple-touch-icon-precomposed.png", TYPE_PNG, "/resources/apple-touch-icon-precomposed.png"}};

  @SuppressWarnings("unused")
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
        if (DartDebugCorePlugin.LOGGING) {
          System.out.println("resource server: socket closed early");
        }

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

        if (DartDebugCorePlugin.LOGGING) {
          System.out.println("resource server: " + header);

          if (response.responseCode != HttpResponse.OK) {
            System.out.println("       response: " + response);
          }
        }

        sendResponse(response);
      } else {
        // do nothing
      }

      socket.close();
    } catch (IOException ioe) {
      safeClose(socket);

      // ignore java.net.SocketException: Connection reset
      // ignore java.net.SocketException: Broken pipe
      if (!(ioe instanceof ConnectException) && !isConnectionReset(ioe)) {
        DartDebugCorePlugin.logError(ioe);
      }
    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);
    }
  }

  private HttpResponse addStandardResponseHeaders(HttpResponse response) {
    response.headers.put("Server", "Dart Editor v" + DartCore.getVersion());
    response.headers.put("Connection", "close");
    return response;
  }

  /**
   * Restrict the files which are legal to serve.
   * 
   * @param file
   * @return
   */
  private boolean canServeFile(File file) {
    return !file.getName().startsWith(".");
  }

  private HttpResponse createErrorResponse() {
    return createErrorResponse("");
  }

  private HttpResponse createErrorResponse(String message) {
    HttpResponse response = new HttpResponse();

    response.responseCode = HttpResponse.NOT_FOUND;
    response.responseText = "Not Found";

    response.headers.put(CONTENT_TYPE, "text/html");
    response.responseBodyText = "<html><head><title>404 Not Found</title></head><body>" + message
        + "</body></html>";
    response.headers.put(CONTENT_LENGTH, Integer.toString(response.responseBodyText.length()));

    return addStandardResponseHeaders(response);
  }

  private HttpResponse createGETResponse(HttpHeader header) throws IOException {
    boolean headOnly = false;

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

    // handle redirecting to mapped resources
    if (javaFile != null) {
      IResource mappedFile = locateMappedFile(javaFile);

      if (mappedFile != null) {
        return createRedirectResponse(mappedFile);
      }
    }

    // no longer running compilation server, instead generating js on launch
    // If a .dart.js file doesn't exist, check for a .dart file next to it.
//    if (javaFile == null && header.file.endsWith(".dart.js")) {
//      String dartFilePath = header.file.substring(0, header.file.length() - 3);
//      File dartFile = locateFile(dartFilePath);
//
//      if (dartFile != null) {
//        javaFile = new File(dartFile.getPath() + ".js");
//      }
//    }

    if (javaFile == null) {
      if (isInWorkspace(header.file)) {
        return createErrorResponse("File not found: " + header.file);
      } else {
        return createErrorResponse("File not found");
      }
    }

    if (!canServeFile(javaFile)) {
      return createErrorResponse("File not found: " + header.file);
    }

    HttpResponse response = new HttpResponse();

    try {
      // Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
      Date date = new Date(javaFile.lastModified());
      response.headers.put(LAST_MODIFIED, HttpResponse.RFC_1123_DATE_FORMAT.format(date));
    } catch (ArrayIndexOutOfBoundsException ex) {
      // This happens occasionally on Windows. 

    } catch (Throwable t) {
      DartDebugCorePlugin.logError(t);
    }

    // Content-Type: text/html[; charset=UTF-8]
    String contentType = getContentType(getFileExtension(javaFile.getName()));
    response.headers.put(CONTENT_TYPE, contentType);

    // Cache-control: no-cache
    response.headers.put(CACHE_CONTROL, "no-cache");

    // Content-Length: 438
    if (javaFile != null) {
      long length = javaFile.length();

//      if (javaScriptContent != null) {
//        length = javaScriptContent.length;
//      }

      response.headers.put(CONTENT_LENGTH, Long.toString(length));
    }

    if (!headOnly) {
//      if (javaScriptContent != null) {
//        response.responseBodyStream = new ByteArrayInputStream(javaScriptContent);
//      } else {
      List<int[]> ranges = header.getRanges();

      if (ranges != null) {
        byte[] rangeData = readRangeData(javaFile, ranges);

        response.responseBodyStream = new ByteArrayInputStream(rangeData);

        response.responseCode = HttpResponse.PARTIAL_CONTENT;
        response.responseText = "Partial Content";

        response.headers.put(CONTENT_LENGTH, Long.toString(rangeData.length));
        // Content-Range: bytes X-Y/Z
        int[] range = ranges.get(0);
        response.headers.put(CONTENT_RANGE, "bytes " + range[0] + "-" + range[1] + "/"
            + rangeData.length);
      } else {
        response.responseBodyStream = new FileInputStream(javaFile);
      }

      // Indicate that we support requesting a subset of the document.
      response.headers.put(ACCEPT_RANGES, "bytes");
//      }
    }

    addStandardResponseHeaders(response);

    return response;
  }

  @SuppressWarnings("unused")
  private HttpResponse createNotAllowedResponse() {
    HttpResponse response = new HttpResponse();

    response.responseCode = HttpResponse.UNAUTHORIZED;
    response.responseText = "Unauthorized";

    response.headers.put(CONTENT_TYPE, "text/html");
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

      return addStandardResponseHeaders(new HttpResponse());
    } else {
      // 404 NOT FOUND
      return createErrorResponse();
    }
  }

  private HttpResponse createRedirectResponse(IResource mappedFile) {
    // HTTP/1.1 302 Found
    // Location: http://www.iana.org/domains/example/

    HttpResponse response = new HttpResponse();

    response.responseCode = HttpResponse.REDIRECT;
    response.responseText = "Found";
    response.headers.put("Location", resourceServer.getUrlForResource(mappedFile));
    response.headers.put(CONTENT_LENGTH, Integer.toString(0));

    addStandardResponseHeaders(response);

    return response;
  }

  /**
   * @return the given string with any %20 sequences decoded
   */
  private String decodeWebChars(String line) {
    // GET /dart/test%C3%BCuuuu/swipe.html HTTP/1.1
    //   ==>
    // GET /dart/testüuuuu/swipe.html HTTP/1.1

    byte[] bytes = line.getBytes();
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] == '%' && (i + 2 < bytes.length)) {
        int val = hex2Int((char) bytes[i + 1], (char) bytes[i + 2]);

        out.write(val);

        i += 2;
      } else {
        out.write(bytes[i]);
      }
    }

    return new String(out.toByteArray(), Charsets.UTF_8);
  }

  /**
   * Combine the given *.dart.js file and the debugger JS agent.
   * 
   * @param dartJsFile
   * @return
   * @throws IOException
   */
//  private byte[] getCombinedContentAndAgent(File dartJsFile) throws IOException {
//    // If we can find the source map token, then insert our debugger agent just before it.
//    // Otherwise, append the debugger agent to the end of the file content.
//
//    final String SRC_MAP_TOKEN = "//@ sourceMappingURL=";
//
//    String content = Files.toString(dartJsFile, Charsets.UTF_8);
//    String agent = new String(getJSAgentContent());
//
//    if (content.indexOf(SRC_MAP_TOKEN) != -1) {
//      int index = content.indexOf(SRC_MAP_TOKEN);
//
//      content = content.substring(0, index) + agent + content.substring(index);
//    } else {
//      content += agent;
//    }
//
//    return content.getBytes(Charsets.UTF_8);
//  }

  private String getContentType(String extension) {
    if (extension != null) {
      extension = extension.toLowerCase();

      if (contentMappings.containsKey(extension)) {
        return serverHtmlAsUtf8(contentMappings.get(extension));
      }

      if (extraMappings.containsKey(extension)) {
        return serverHtmlAsUtf8(extraMappings.get(extension));
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

  /**
   * Given two hex chars, return the resulting (byte) value. Ex. '%20' ==> 32.
   */
  private int hex2Int(char high, char low) {
    StringBuffer buf = new StringBuffer();
    buf.append(high);
    buf.append(low);

    try {
      return Integer.parseInt(buf.toString(), 16);
    } catch (NumberFormatException nfe) {
      return '%';
    }
  }

  private boolean isAllowableConnection(Socket connection, HttpHeader header) {
    InetAddress remoteAddress = connection.getInetAddress();

    if (isLocalAddress(remoteAddress)) {
      return true;
    }

    if (!RemoteConnectionPreferenceManager.getManager().canConnectRemote()) {
      return false;
    }

    // User-Agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/536.8 (KHTML, like Gecko) Chrome/20.0.1110.0 (Dart) Safari/536.8
    if (DartDebugCorePlugin.getPlugin().getUserAgentManager() != null) {
      String userAgent = header.getHeaderKey(USER_AGENT);

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
    // ignore java.net.SocketException: Broken pipe
    // ignore java.net.SocketException: Software caused connection abort: socket write error

    if (ioe instanceof SocketException) {
      String message = ioe.getMessage().toLowerCase();

      if (message == null) {
        return false;
      }

      return message.contains("connection rese") || message.contains("connection abort")
          || message.contains("broken pipe");
    }

    return false;
  }

  private boolean isInWorkspace(String filePath) {
    IPath path = Path.fromPortableString(filePath);
    if (path.segmentCount() == 0) {
      return false;
    }

    IProject project;
    try {
      // This can throw errors on some path inputs.
      project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
    } catch (Throwable t) {
      return false;
    }
    if (!project.exists() || !project.isOpen()) {
      return false;
    }
    return true;
  }

//  private boolean isFileJsArtifact(File javaFile) {
//    return javaFile.getName().endsWith(".dart.js");
//  }

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

  private File locateFile(String filePath) {
    IPath path = Path.fromPortableString(filePath);

    if (path.segmentCount() == 0) {
      return null;
    }

    IProject project;

    try {
      // This can throw errors on some path inputs.
      project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
    } catch (Throwable t) {
      return null;
    }

    if (!project.exists() || !project.isOpen()) {
      return null;
    }

    IPath projectLocation = project.getLocation();

    if (projectLocation == null) {
      return null;
    }

    IPath childPath = path.removeFirstSegments(1);

    File file = new File(projectLocation.toFile(), childPath.toOSString());

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      // We don't use "packages" directories with the Analysis Server.
    } else {
      if (!file.exists() && childPath.toString().contains(DartCore.PACKAGES_DIRECTORY_PATH)) {
        int packagesIndex = childPath.toString().indexOf(DartCore.PACKAGES_DIRECTORY_PATH);
        String pathString = childPath.toString().substring(
            packagesIndex + DartCore.PACKAGES_DIRECTORY_PATH.length());
        IFileInfo fileInfo = DartCore.getProjectManager().resolveUriToFileInfo(
            project,
            DartCore.PACKAGE_SCHEME_SPEC + pathString);
        if (fileInfo != null) {
          file = fileInfo.getFile();
        }
      }
    }

    return file.exists() ? file : null;
  }

  private IResource locateMappedFile(File file) {
    IResource resource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
        Path.fromOSString(file.getAbsolutePath()));

    if (resource instanceof IFile) {
      IFile resourceFile = (IFile) resource;

      String mappingPath = DartCore.getResourceRemapping(resourceFile);

      if (mappingPath != null) {
        IResource mappedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(
            Path.fromPortableString(mappingPath));

        if (mappedResource != null && mappedResource.exists()) {
          return mappedResource;
        }
      }
    }

    return null;
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
      header.file = decodeWebChars(strs[1]);
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

    f.close();

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

    // HTTP/1.1 200 OK
    builder.append("HTTP/1.1 " + response.responseCode + " " + response.responseText + CRLF);

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
    response.headers.put(CONTENT_TYPE, "text/html; charset=UTF-8");
    response.headers.put(CACHE_CONTROL, "no-cache");
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
        response.headers.put(CONTENT_TYPE, resourceInfo[1]);

        if (!headOnly) {
          response.responseBodyStream = conn.getInputStream();
        }

        addStandardResponseHeaders(response);

        return response;
      }
    }

    return null;
  }

  private String serverHtmlAsUtf8(String mimeType) {
    if (TYPE_HTML.equals(mimeType) || TYPE_DART.equals(mimeType)) {
      return mimeType + "; charset=utf-8";
    } else {
      return mimeType;
    }
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
