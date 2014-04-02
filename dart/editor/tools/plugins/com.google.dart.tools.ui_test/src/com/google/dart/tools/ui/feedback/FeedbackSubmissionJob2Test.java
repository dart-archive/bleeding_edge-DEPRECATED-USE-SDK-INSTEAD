/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.feedback;

import com.google.dart.engine.utilities.os.OSUtilities;
import com.google.dart.tools.ui.feedback.FeedbackUtils.Stats;

import static com.google.dart.tools.ui.feedback.LogReaderTest.EOL;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_CONTENTS;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_EXCEPTION;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_MESSAGE;
import static com.google.dart.tools.ui.feedback.LogReaderTest.LOG_SESSION_START;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import userfeedback.Common.CommonData;
import userfeedback.Extension.ExtensionSubmit;
import userfeedback.Web.ProductSpecificData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class FeedbackSubmissionJob2Test extends TestCase {

  private final class MockProgressMonitor implements IProgressMonitor {
    boolean done;
    boolean begin;
    boolean worked;

    @Override
    public void beginTask(String name, int totalWork) {
      begin = true;
    }

    @Override
    public void done() {
      done = true;
    }

    @Override
    public void internalWorked(double work) {
    }

    @Override
    public boolean isCanceled() {
      return false;
    }

    @Override
    public void setCanceled(boolean value) {
    }

    @Override
    public void setTaskName(String name) {
    }

    @Override
    public void subTask(String name) {
    }

    @Override
    public void worked(int work) {
      worked = true;
    }
  }

  // Feedback content
  private static final String IDE_VERSION = "ideVersion";
  private static final String FEEDBACK_TEXT = "feedbackText";
  private static final String SPARSE_MAP_KEY = "test-option-key";
  private static final String SPARSE_MAP_VALUE = "test-option-value";
  private static final String TOKEN_RESPONSE_GOOD = "header stuff to be ignored\n"
      + "          var GF_TOKEN = \"some-token\";\n" // valid token
      + "trailing stuff to be ignored";

  public static FeedbackReport newTestFeedbackReport(String logContents) {
    return newTestFeedbackReport(FEEDBACK_TEXT, null, logContents);
  }

  private static FeedbackReport newTestFeedbackReport(String feedbackText, Image screenshot,
      String logContents) {
    Map<String, String> sparseOptionsMap = new HashMap<String, String>();
    sparseOptionsMap.put(SPARSE_MAP_KEY, SPARSE_MAP_VALUE);
    sparseOptionsMap.put("experimental/altKeyBindings", "true");
    FeedbackReport report = new FeedbackReport(//
        feedbackText,
        "Editor",
        "osDetails",
        IDE_VERSION,
        new Stats(1, 2, 3, 4, 5, 6, "indexStats", true),
        logContents,
        screenshot,
        true,
        true,
        sparseOptionsMap);
    report.setUserEmail("username@gmail.com");
    return report;
  }

  private ServerSocket serverSocket;
  private Throwable serverException;

  private boolean sawReturn = false;
  boolean errorLogged;
  private static final String ISO_8859_1 = "ISO-8859-1";
  private static final String CRLF = "\r\n";
  private static final String FEEDBACK_LOG_1 = LOG_CONTENTS;

  private static final String FEEDBACK_LOG_2 = LOG_EXCEPTION + EOL + EOL + LOG_MESSAGE + EOL + EOL
      + LOG_CONTENTS;

  private static final String FEEDBACK_LOG_3 = LOG_EXCEPTION + EOL + EOL + LOG_MESSAGE;

  /**
   * Test by submitting feedback to the real Google Feedback server. This test is not run by default
   * and is not part of the test suite. All other tests send feedback to a local mock of the Google
   * Feedback server.
   */
  public static void main(String[] args) {
    Display display = new Display();
    try {
      Image tinyScreenshot = new Image(display, 50, 50);
      try {
        GC gc = new GC(display);
        gc.copyArea(tinyScreenshot, 0, 0);
        gc.dispose();

        // Show screenshot for debugging
//        Shell shell = new Shell(display);
//        shell.setLayout(new RowLayout());
//        new Label(shell, SWT.NONE).setText("Feedback image: ");
//        new Label(shell, SWT.NONE).setImage(tinyScreenshot);
//        shell.open();
//        while (!shell.isDisposed()) {
//          if (!display.readAndDispatch()) {
//            display.sleep();
//          }
//        }
//        shell.dispose();

        // Send feedback
        FeedbackSubmissionJob2 job = new FeedbackSubmissionJob2(newTestFeedbackReport(
            "Test feedback with a tiny screenshot " + System.currentTimeMillis(),
            tinyScreenshot,
            FEEDBACK_LOG_1), true, true, true);
        job.setTestFeedback(true);
        try {
          job.submitFeedback(new NullProgressMonitor());
          System.out.println("submitFeedback successful");
        } catch (IOException e) {
          System.out.println("submitFeedback failed");
          e.printStackTrace(System.out);
        }

      } finally {
        tinyScreenshot.dispose();
      }
    } finally {
      display.dispose();
    }
  }

  private String tokenResponse;

  private String submitResponse;
  private final BlockingQueue<Object> requestQueue = new ArrayBlockingQueue<Object>(4);
  private final Object NO_CONTENT_SENT = new Object();

  public void test_getDataAsText() throws Exception {
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    String text = job.getDataAsText();
    System.out.println(text);
    assertTrue(text.contains("OS"));
    assertTrue(text.contains("altKeyBindings"));
    assertTrue(text.contains("!SESSION"));
  }

  public void test_getDataAsText_noLog() throws Exception {
    FeedbackSubmissionJob2 job = newTestFeedbackClient(false, FEEDBACK_LOG_1, true, true);
    String text = job.getDataAsText();
    assertTrue(text.contains("OS"));
    assertTrue(text.contains("altKeyBindings"));
    assertFalse(text.contains("!SESSION"));
  }

  public void test_getFeedbackToken_invalidResponse1() throws Exception {
    tokenResponse = "header stuff to be ignored\n" // header
        + "          var GF_TOKEN = \"some-token\n" // token missing closing "
        + "trailing stuff to be ignored";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    try {
      assertEquals("some-token", job.getFeedbackToken());
      fail("Expected IOException");
    } catch (IOException e) {
      // expected
    }
    assertGetTokenProcessed();
  }

  public void test_getFeedbackToken_invalidResponse2() throws Exception {
    tokenResponse = "header stuff to be ignored\n" // header
        + "          garbled stuff here\n" // garbled token
        + "trailing stuff to be ignored";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    try {
      assertEquals("some-token", job.getFeedbackToken());
      fail("Expected IOException");
    } catch (IOException e) {
      // expected
    }
    assertGetTokenProcessed();
  }

  public void test_getFeedbackToken_validResponse() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    assertEquals("some-token", job.getFeedbackToken());
    assertGetTokenProcessed();
  }

  public void test_submitFeedback_noLog() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(false, FEEDBACK_LOG_1, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    ExtensionSubmit feedback = waitForFeedbackReceived();
    CommonData commonData = feedback.getCommonData();
    assertContainsProductSpecificData(commonData, "log", null);
    assertContainsProductSpecificData(commonData, "logEntryPrevious2", null);
    assertContainsProductSpecificData(commonData, "logEntryPrevious1", null);
    assertContainsProductSpecificData(commonData, "logEntryStart", null);
    assertContainsProductSpecificData(commonData, "logEntry1", null);
    assertContainsProductSpecificData(commonData, "logEntry2", null);
  }

  public void test_submitFeedback_private() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, false);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    ExtensionSubmit feedback = waitForFeedbackReceived();
    CommonData commonData = feedback.getCommonData();
    assertContainsProductSpecificData(commonData, "public", "false");
  }

  public void test_submitFeedback_public() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    ExtensionSubmit feedback = waitForFeedbackReceived();
    CommonData commonData = feedback.getCommonData();
    assertContainsProductSpecificData(commonData, "public", "true");
  }

  public void test_submitFeedback_success1() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    assertSubmitFeedbackProcessed();
  }

  public void test_submitFeedback_success2() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    assertSubmitFeedbackProcessed();
  }

  public void test_submitFeedback_withLog1() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    ExtensionSubmit feedback = waitForFeedbackReceived();
    CommonData commonData = feedback.getCommonData();
    assertContainsProductSpecificData(commonData, "log", FEEDBACK_LOG_1);
    assertContainsProductSpecificData(commonData, "logEntryPrevious2", null);
    assertContainsProductSpecificData(commonData, "logEntryPrevious1", null);
    assertContainsProductSpecificData(commonData, "logEntryStart", LOG_SESSION_START);
    assertContainsProductSpecificData(commonData, "logEntry1", LOG_MESSAGE);
    assertContainsProductSpecificData(commonData, "logEntry2", LOG_EXCEPTION);
  }

  public void test_submitFeedback_withLog2() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_2, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    ExtensionSubmit feedback = waitForFeedbackReceived();
    CommonData commonData = feedback.getCommonData();
    assertContainsProductSpecificData(commonData, "log", FEEDBACK_LOG_2);
    assertContainsProductSpecificData(commonData, "logEntryPrevious2", LOG_EXCEPTION);
    assertContainsProductSpecificData(commonData, "logEntryPrevious1", LOG_MESSAGE);
    assertContainsProductSpecificData(commonData, "logEntryStart", LOG_SESSION_START);
    assertContainsProductSpecificData(commonData, "logEntry1", LOG_MESSAGE);
    assertContainsProductSpecificData(commonData, "logEntry2", LOG_EXCEPTION);
  }

  public void test_submitFeedback_withLog3() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "header stuff \"success\":true trailing stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_3, true, true);
    job.submitFeedback(new NullProgressMonitor());
    assertGetTokenProcessed();
    ExtensionSubmit feedback = waitForFeedbackReceived();
    CommonData commonData = feedback.getCommonData();
    assertContainsProductSpecificData(commonData, "log", FEEDBACK_LOG_3);
    assertContainsProductSpecificData(commonData, "logEntryPrevious2", null);
    assertContainsProductSpecificData(commonData, "logEntryPrevious1", null);
    assertContainsProductSpecificData(commonData, "logEntryStart", null);
    assertContainsProductSpecificData(commonData, "logEntry1", LOG_EXCEPTION);
    assertContainsProductSpecificData(commonData, "logEntry2", LOG_MESSAGE);
  }

  public void test_submitFeedback_withProgress1() throws Exception {
    //TODO (danrubel): Investigate and fix
    if (OSUtilities.isLinux()) {
      return;
    }
    tokenResponse = TOKEN_RESPONSE_GOOD;
    submitResponse = "";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    MockProgressMonitor monitor = new MockProgressMonitor();
    IStatus result = job.run(monitor);
    assertGetTokenProcessed();
    assertSubmitFeedbackProcessed();
    assertTrue(monitor.begin);
    assertTrue(monitor.worked);
    assertTrue(monitor.done);
    assertTrue(result.isOK());
  }

  public void test_submitFeedback_withProgress2() throws Exception {
    tokenResponse = "random stuff";
    FeedbackSubmissionJob2 job = newTestFeedbackClient(true, FEEDBACK_LOG_1, true, true);
    MockProgressMonitor monitor = new MockProgressMonitor();
    IStatus result = job.run(monitor);
    assertFalse(result.isOK());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    startServer();
  }

  @Override
  protected void tearDown() throws Exception {
    stopServer();
    if (serverException != null) {
      serverException.printStackTrace();
      fail(getClass().getSimpleName() + " Server Exception");
    }
    super.tearDown();
  }

  private void assertContainsProductSpecificData(CommonData commonData, String key,
      String expectedValue) {
    List<String> foundValues = getProductSpecificData(commonData, key);
    for (String value : foundValues) {
      if (value.equals(expectedValue)) {
        return;
      }
    }
    if (foundValues.isEmpty()) {
      if (expectedValue == null) {
        return;
      }
      fail("Failed to find key: " + key);
    } else {
      fail("Expected value: " + expectedValue + " but found: " + foundValues);
    }
  }

  private void assertGetTokenProcessed() throws InterruptedException {
    String request = (String) requestQueue.poll(100, TimeUnit.MILLISECONDS);
    assertNotNull(request);

    Object content = requestQueue.poll(100, TimeUnit.MILLISECONDS);
    assertSame(NO_CONTENT_SENT, content);
  }

  private void assertSubmitFeedbackProcessed() throws InterruptedException {
    ExtensionSubmit feedback = waitForFeedbackReceived();
    assertEquals(FeedbackSubmissionJob2.DART_EDITOR_PRODUCT_ID, feedback.getProductId());
    CommonData commonData = feedback.getCommonData();
    assertEquals(IDE_VERSION, commonData.getProductVersion());
    assertEquals(FEEDBACK_TEXT, commonData.getDescription());
    assertContainsProductSpecificData(commonData, "option/" + SPARSE_MAP_KEY, SPARSE_MAP_VALUE);
  }

  private List<String> getProductSpecificData(CommonData commonData, String key) {
    List<String> foundValues = new ArrayList<String>();
    for (ProductSpecificData data : commonData.getProductSpecificDataList()) {
      if (data.getKey().equals(key)) {
        String value = data.getValue();
        if (value != null) {
          foundValues.add(value);
        }
      }
    }
    return foundValues;
  }

  private FeedbackSubmissionJob2 newTestFeedbackClient(boolean includeLog, String logContents,
      boolean includeScreenshot, boolean isPublic) {
    return new FeedbackSubmissionJob2(
        newTestFeedbackReport(logContents),
        includeLog,
        includeScreenshot,
        isPublic,
        "http://127.0.0.1:" + serverSocket.getLocalPort() + "/token",
        "http://127.0.0.1:" + serverSocket.getLocalPort() + "/feedback?") {
      @Override
      protected void logError(IOException e) {
        // ignored
      }
    };
  }

  private void processRequest(Socket socket) throws IOException {
    StringBuilder request = new StringBuilder();
    Object parseResult = NO_CONTENT_SENT;
    int responseCode;
    String responseText;

    // Get the request
    InputStream in = socket.getInputStream();
    try {
      String line = readLine(in);
      if (line == null) {
        return;
      }
      request.append(line);
      request.append(CRLF);

      if (line.startsWith("GET /token ") && tokenResponse != null) {
        responseCode = HttpURLConnection.HTTP_OK;
        responseText = tokenResponse;
      } else if (line.startsWith("POST /feedback?some-token") && submitResponse != null) {
        if (submitResponse.length() == 0) {
          responseCode = 204; // HTTP_STATUS_OK_NO_CONTENT
        } else {
          responseCode = HttpURLConnection.HTTP_OK;
        }
        responseText = submitResponse;
      } else {
        responseCode = 404;
        responseText = "Unrecognized Request: " + line;
      }

      // Consume remaining input
      while (true) {
        try {
          line = readLine(in);
        } catch (IOException e) {
          // ignore additional input
          break;
        }
        if (line == null || line.isEmpty()) {
          break;
        }
        request.append(line);
        request.append(CRLF);
        // Read following content as bytes not string
        if (line.startsWith("Content-Length:")) {
          int byteCount = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
          byte[] content = new byte[byteCount];
          in.skip(3); // LF CR LF
          if (in.read(content) != byteCount) {
            throw new RuntimeException("Failed to read content");
          }

          // Echo bytes for debugging
//          for (int count = 0; count < byteCount; count++) {
//            int value = content[count];
//            if (value < 0) {
//              value += 0x100;
//            }
//            String text = Integer.toHexString(value).toUpperCase();
//            if (text.length() < 2) {
//              request.append("0");
//            }
//            request.append(text);
//            request.append(" ");
//            if (count % 16 == 15) {
//              request.append(CRLF);
//            }
//          }
//          if (byteCount % 16 != 0) {
//            request.append(CRLF);
//          }

          try {
            parseResult = ExtensionSubmit.parseFrom(content);
          } catch (IOException e) {
            serverException = e;
          }
          break;
        }
      }

      // Echo the request for debugging
//      System.out.println("==================================");
//      System.out.println(request);

      // Build a response
      StringBuilder builder = new StringBuilder();
      builder.append("HTTP/1.1 " + responseCode + " " + "OK" + CRLF); // HTTP/1.1 200 OK
      builder.append(CRLF);
      builder.append(responseText);
      builder.append(CRLF);

      // Send response
      OutputStream out = socket.getOutputStream();
      try {
        out.write(builder.toString().getBytes(ISO_8859_1));
        out.flush();
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }

    requestQueue.add(request.toString());
    requestQueue.add(parseResult);
  }

  private String readLine(InputStream in) throws IOException {
    StringBuilder result = new StringBuilder(80);
    while (true) {
      int ch = in.read();
      if (ch == -1) { // EOS
        break;
      } else if (ch == '\r') { // CR
        sawReturn = true;
        break;
      } else if (ch == '\n') { // LF
        if (sawReturn) {
          sawReturn = false;
        } else {
          break;
        }
      } else {
        sawReturn = false;
        result.append((char) ch);
      }
    }
    return result.toString();
  }

  private void startServer() throws IOException {
    final String serverName = getClass().getSimpleName() + " Server";
    serverSocket = new ServerSocket(0);
    new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          Socket socket;
          try {
            socket = serverSocket.accept();
          } catch (IOException e) {
            // socket closed as part of teardown
            break;
          }
          try {
            processRequest(socket);
          } catch (Throwable e) {
            serverException = e;
          }
        }
      }
    }, serverName).start();
  }

  private void stopServer() {
    try {
      serverSocket.close();
    } catch (IOException exception) {
      // ignored
    }
  }

  private ExtensionSubmit waitForFeedbackReceived() throws InterruptedException {
    String request = (String) requestQueue.poll(100, TimeUnit.MILLISECONDS);
    assertNotNull(request);
    assertTrue(request.indexOf("Content-Type: application/x-protobuf") > 0);
    Object content = requestQueue.poll(100, TimeUnit.MILLISECONDS);
    assertNotSame(NO_CONTENT_SENT, content);
    return (ExtensionSubmit) content;
  }
}
