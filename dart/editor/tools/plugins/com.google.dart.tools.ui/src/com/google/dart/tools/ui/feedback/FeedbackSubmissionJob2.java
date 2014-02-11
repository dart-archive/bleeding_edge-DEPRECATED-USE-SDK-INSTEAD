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

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.actions.InstrumentedJob;
import com.google.dart.tools.ui.feedback.FeedbackUtils.Stats;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import userfeedback.Common.CommonData;
import userfeedback.Extension.ExtensionSubmit;
import userfeedback.Extension.PostedScreenshot;
import userfeedback.Math.Dimensions;
import userfeedback.Web.ProductSpecificData;
import userfeedback.Web.ProductSpecificData.Type;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A client for communicating with Google Feedback.
 */
public class FeedbackSubmissionJob2 extends InstrumentedJob {

  // The Google Feedback product identifier for Dart Editor
  public static final int DART_EDITOR_PRODUCT_ID = 97695;

  private static final String HTTP_POST = "POST";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String PROTOBUF_CONTENT = "application/x-protobuf";
  private static final int HTTP_STATUS_OK_NO_CONTENT = 204;

  /**
   * The default URL used to obtain the feedback token required when submitting feedback.
   */
  private static final String TOKEN_URL = "https://www.google.com/tools/feedback/submit_frame?useAnonymousFeedback=true";
  private static final String TOKEN_PREFIX = "GF_TOKEN = \"";
  private static final String TOKEN_SUFFIX = "\";";

  /**
   * The default URL used when submitting feedback.
   */
  private static final String SUBMIT_URL = "https://www.google.com/tools/feedback/anonymous_submit?at=";

  /**
   * The string contained in the response asserting that the feedback was accepted.
   */
  private static final String SUCCESS_RESPONSE = "\"success\":true";

  /**
   * The URL used to obtain the feedback token required when submitting feedback.
   */
  private final String tokenUrl;

  /**
   * The URL used when submitting feedback.
   */
  private final String submitUrl;

  /**
   * The feedback report to be submitted (not {@code null})
   */
  private final FeedbackReport report;

  /**
   * A flag indicating whether the log should be included in the feedback
   */
  private boolean includeLog;

  /**
   * A flag indicating whether the screenshot should be included in the feedback
   */
  private boolean includeScreenshot;

  /**
   * A flag indicating whether this feedback can be added to a public issue tracker
   */
  private boolean isPublic;

  /**
   * A flag indicating whether this feedback is a test and thus should be ignored
   */
  private boolean testFeedback;

  public FeedbackSubmissionJob2(FeedbackReport report, boolean includeLog,
      boolean includeScreenshot, boolean isPublic) {
    this(report, includeLog, includeScreenshot, isPublic, TOKEN_URL, SUBMIT_URL);
  }

  public FeedbackSubmissionJob2(FeedbackReport report, boolean includeLog,
      boolean includeScreenshot, boolean isPublic, String tokenUrl, String submitUrl) {
    super(FeedbackMessages.FeedbackSubmissionJob_sending_feedback_job_label);
    this.report = report;
    this.includeLog = includeLog;
    this.includeScreenshot = includeScreenshot;
    this.tokenUrl = tokenUrl;
    this.submitUrl = submitUrl;
    this.isPublic = isPublic;
  }

  /**
   * Return human readable version of the feedback data.
   */
  public String getDataAsText() {
    TreeMap<String, String> results = new TreeMap<String, String>();
    CommonData.Builder commonData = createCommonData();
    for (Entry<FieldDescriptor, Object> entry : commonData.getAllFields().entrySet()) {
      String key = entry.getKey().getName();
      Object value = entry.getValue();
      if (!key.equals("description") && !key.equals("product_specific_data")) {
        results.put(key, value != null ? value.toString() : "null");
      }
    }
    int dataIndex = 0;
    String logContents = null;
    while (dataIndex < commonData.getProductSpecificDataCount()) {
      ProductSpecificData data = commonData.getProductSpecificData(dataIndex);
      String key = data.getKey();
      if (!key.startsWith("log")) {
        results.put(key, data.getValue());
      } else if (key.equals("log")) {
        logContents = data.getValue();
      }
      dataIndex++;
    }

    @SuppressWarnings("resource")
    PrintStringWriter writer = new PrintStringWriter();
    for (Map.Entry<String, String> entry : results.entrySet()) {
      writer.print(entry.getKey());
      writer.print(" : ");
      writer.println(entry.getValue());
    }
    if (logContents != null) {
      writer.println();
      writer.println(logContents);
    }
    return writer.toString();
  }

  /**
   * Contact the feedback service to obtain a new feedback token for use when submitting feedback.
   * If there is no response or the token cannot be obtained for some reason, then an exception is
   * thrown. This method is automatically called by {@link #submitFeedback(IProgressMonitor)}.
   * 
   * @return the token (not {@code null})
   */
  public String getFeedbackToken() throws IOException {
    StringBuilder response = sendRequest(tokenUrl, null);
    int start = response.indexOf(TOKEN_PREFIX);
    if (start == -1) {
      throw new IOException("No feedback token found in response");
    }
    start += TOKEN_PREFIX.length();
    int end = response.indexOf(TOKEN_SUFFIX, start);
    if (end == -1 || end <= start) {
      throw new IOException("Malformed feedback token found in response");
    }
    return response.substring(start, end);
  }

  /**
   * Set a flag indicating that the feedback is only a test.
   */
  public void setTestFeedback(boolean isTest) {
    testFeedback = isTest;
  }

  /**
   * Submit the feedback. If the feedback was not received for any reason, an exception is thrown.
   * 
   * @param monitor TODO
   */
  public void submitFeedback(IProgressMonitor monitor) throws IOException {
    monitor.worked(1);
    String token = getFeedbackToken();
    monitor.worked(1);
    ExtensionSubmit.Builder builder = createFeedback();
    monitor.worked(1);
    StringBuilder response = sendRequest(submitUrl + token, builder.build());
    if (response.length() > 0 && response.indexOf(SUCCESS_RESPONSE) < 0) {
      throw new IOException("Submit failed with response: " + response);
    }
    // Feedback submitted successfully
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor, UIInstrumentationBuilder instrumentation) {
    monitor.beginTask("Sending Feedback", 4);
    try {
      submitFeedback(monitor);
    } catch (IOException e) {
      logError(e);
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, 0, "Send Feedback Failed", e);
    } finally {
      monitor.done();
    }
    return Status.OK_STATUS;
  }

  protected void logError(IOException e) {
    DartCore.logError("Failed to send feedback", e);
  }

  private CommonData.Builder createCommonData() {
    CommonData.Builder commonData = CommonData.newBuilder();

    String userEmail = report.getUserEmail();
    if (userEmail != null) {
      // Setting this field sends an email to the user with a link that is not useful
//      commonData.setUserEmail(userEmail);
      commonData.addProductSpecificData(createEntry("userEmail", userEmail));
    }

    commonData.setDescription(report.getFeedbackText());
    commonData.setProductVersion(report.getProductVersion());

    // Fields for possible future use
//    commonData.setCountryCode("US");
//    commonData.setReportType(ReportType.WEB_FEEDBACK);
//    commonData.setProductSpecificContext("feedback-context");

    commonData.addProductSpecificData(createEntry("productName", report.getProductName()));
    commonData.addProductSpecificData(createEntry("public", isPublic ? "true" : "false"));
    String logContents = report.getLogContents();
    if (includeLog && logContents != null) {
      commonData.addProductSpecificData(createEntry("log", logContents));
      List<LogEntry> entries = report.getLogEntries();
      if (entries != null) {

        // Extract session start with previous and following log entries
        int index = entries.size();
        while (--index >= 0) {
          if (entries.get(index).isSessionStart()) {
            break;
          }
        }
        if (index >= 2) {
          commonData.addProductSpecificData(createEntry(
              "logEntryPrevious2",
              entries.get(index - 2).getContent()));
        }
        if (index >= 1) {
          commonData.addProductSpecificData(createEntry(
              "logEntryPrevious1",
              entries.get(index - 1).getContent()));
        }
        if (index >= 0) {
          commonData.addProductSpecificData(createEntry(
              "logEntryStart",
              entries.get(index).getContent()));
        }
        if (index + 1 < entries.size()) {
          commonData.addProductSpecificData(createEntry(
              "logEntry1",
              entries.get(index + 1).getContent()));
        }
        if (index + 2 < entries.size()) {
          commonData.addProductSpecificData(createEntry(
              "logEntry2",
              entries.get(index + 2).getContent()));
        }

        // Look for crash in log
        for (LogEntry entry : entries) {
          if (entry.isCrashMessage()) {
            commonData.addProductSpecificData(createEntry("logCrash", entry.getContent()));
            break;
          }
        }

      }

    }
    if (testFeedback) {
      commonData.addProductSpecificData(createEntry("test", "true"));
    }
    commonData.addProductSpecificData(createEntry("OS", report.getOsDetails()));
    commonData.addProductSpecificData(createEntry("WS", FeedbackUtils.getWS()));
    commonData.addProductSpecificData(createEntry("JVM", report.getJvmDetails()));
    commonData.addProductSpecificData(createEntry("SDK", report.isSdkInstalled()));
    IProduct product = Platform.getProduct();
    if (product != null) {
      commonData.addProductSpecificData(createEntry("App", product.getApplication()));
      commonData.addProductSpecificData(createEntry("AppId", product.getId()));
      commonData.addProductSpecificData(createEntry("AppName", product.getName()));
      Bundle bundle = product.getDefiningBundle();
      if (bundle != null) {
        commonData.addProductSpecificData(createEntry("AppBundleId", bundle.getSymbolicName()));
        Version version = bundle.getVersion();
        if (version != null) {
          commonData.addProductSpecificData(createEntry("AppBundleVersion", version.toString()));
        }
      }
    }
    commonData.addProductSpecificData(createEntry("Dartium", report.isDartiumInstalled()));

    Stats stats = report.getStats();
    commonData.addProductSpecificData(createEntry("memoryMax", stats.maxMem));
    commonData.addProductSpecificData(createEntry("memoryTotal", stats.totalMem));
    commonData.addProductSpecificData(createEntry("memoryFree", stats.freeMem));
    commonData.addProductSpecificData(createEntry("numThreads", stats.numThreads));
    commonData.addProductSpecificData(createEntry("numProjects", stats.numProjects));
    commonData.addProductSpecificData(createEntry("numEditors", stats.numEditors));
    commonData.addProductSpecificData(createEntry("autoRunPubEnabled", stats.autoRunPubEnabled));
    commonData.addProductSpecificData(createEntry("indexStats", stats.indexStats));

    Map<String, String> optionsMap = report.getSparseOptionsMap();
    if (optionsMap != null) {
      for (Entry<String, String> entry : optionsMap.entrySet()) {
        commonData.addProductSpecificData(createEntry("option/" + entry.getKey(), entry.getValue()));
      }
    }

    return commonData;
  }

  private ProductSpecificData.Builder createEntry(String key, boolean value) {
    ProductSpecificData.Builder entry = ProductSpecificData.newBuilder();
    entry.setKey(key);
    entry.setValue(value ? "true" : "false");
    return entry;
  }

  private ProductSpecificData.Builder createEntry(String key, long value) {
    ProductSpecificData.Builder entry = ProductSpecificData.newBuilder();
    entry.setKey(key);
    entry.setValue(Long.toString(value));
    entry.setType(Type.NUMBER);
    return entry;
  }

  private ProductSpecificData.Builder createEntry(String key, String value) {
    ProductSpecificData.Builder entry = ProductSpecificData.newBuilder();
    entry.setKey(key);
    entry.setValue(value);
    return entry;
  }

  private ExtensionSubmit.Builder createFeedback() {
    ExtensionSubmit.Builder submit = ExtensionSubmit.newBuilder();
    submit.setProductId(DART_EDITOR_PRODUCT_ID);
    submit.setCommonData(createCommonData());
    if (includeScreenshot) {
      Image image = report.getImage();
      if (image != null) {
        submit.setScreenshot(createScreenshot(image));
      }
    }

    // Fields for possible future use
//    submit.setCategoryTag("category");
//    submit.setBucket("bucket");
//    submit.setTypeId(27);

    return submit;
  }

  private PostedScreenshot.Builder createScreenshot(Image image) {
    ByteArrayOutputStream outs = new ByteArrayOutputStream();
    ImageLoader loader = new ImageLoader();
    loader.data = new ImageData[] {image.getImageData()};
    loader.save(outs, SWT.IMAGE_PNG);
    byte[] data = outs.toByteArray();

    PostedScreenshot.Builder screenshot = PostedScreenshot.newBuilder();
    screenshot.setMimeType("image/png");
    screenshot.setBinaryContent(ByteString.copyFrom(data));
    Dimensions.Builder dimensions = Dimensions.newBuilder();
    dimensions.setHeight(image.getBounds().height);
    dimensions.setWidth(image.getBounds().width);
    screenshot.setDimensions(dimensions);
    return screenshot;
  }

  /**
   * Send a request to the specified URL and return the response. An exception is thrown if the
   * request cannot be sent.
   * 
   * @param url the URL spec to which the request is sent
   * @param body the body of the feedback message or {@code null} if this is a token request
   * @return the response (not {@code null})
   * @exception IOException thrown if there is a problem sending the request
   */
  private StringBuilder sendRequest(String url, ExtensionSubmit body) throws MalformedURLException,
      IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    StringBuilder response;
    try {
      connection.setUseCaches(false);
      connection.setAllowUserInteraction(false);
      if (body != null) {
        byte[] content = body.toByteArray();
        connection.setRequestMethod(HTTP_POST);
        connection.setRequestProperty(CONTENT_TYPE, PROTOBUF_CONTENT);
        connection.setRequestProperty("Content-Length", Integer.toString(content.length));
        connection.setDoOutput(true);

        // Echo bytes for debugging
//        for (int count = 0; count < content.length; count++) {
//          int value = content[count];
//          if (value < 0) {
//            value += 0x100;
//          }
//          String text = Integer.toHexString(value).toUpperCase();
//          if (text.length() < 2) {
//            System.out.print("0");
//          }
//          System.out.print(text);
//          System.out.print(" ");
//          if (value < 0) {
//            System.out.println("<<< less than zero");
//            throw new RuntimeException();
//          }
//          if (count % 16 == 15) {
//            System.out.println();
//          }
//        }
//        if (content.length % 16 != 0) {
//          System.out.println();
//        }

        OutputStream out = connection.getOutputStream();
        try {
          out.write(content);
        } finally {
          out.close();
        }
      }
      int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HTTP_STATUS_OK_NO_CONTENT) {
        throw new IOException("Failed to contact feedback: " + responseCode);
      }
      InputStream in = connection.getInputStream();
      response = new StringBuilder();
      try {
        byte[] temp = new byte[8192];
        while (true) {
          int len = in.read(temp);
          if (len == -1) {
            break;
          }
          response.append(new String(temp, 0, len));
        }
      } finally {
        in.close();
      }
    } finally {
      connection.disconnect();
    }
    return response;
  }
}
