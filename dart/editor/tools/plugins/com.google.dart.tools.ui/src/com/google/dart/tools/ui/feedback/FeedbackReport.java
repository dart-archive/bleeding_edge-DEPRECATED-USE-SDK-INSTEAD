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
package com.google.dart.tools.ui.feedback;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.feedback.FeedbackUtils.Stats;

import org.eclipse.swt.graphics.Image;

import java.util.List;
import java.util.Map;

/**
 * An object representing a user feedback report.
 */
public class FeedbackReport {

  private String feedbackText;
  private final String osDetails;
  private final String productVersion;
  private final Stats stats;
  private final String logContents;
  private final String productName;

  private final Image screenshotImage;

  private final boolean isSdkInstalled;
  private final boolean isDartiumInstalled;
  private final Map<String, String> sparseOptionsMap;
  private String userEmail;

  /**
   * Create a new feedback instance with default values.
   */
  public FeedbackReport(String productName, Image screenshotImage) {
    this(
        "",
        productName,
        FeedbackUtils.getOSName(),
        FeedbackUtils.getEditorVersionDetails(),
        FeedbackUtils.getStats(),
        LogReader.readLogSafely(),
        screenshotImage,
        FeedbackUtils.isSdkInstalled(),
        FeedbackUtils.isDartiumInstalled(),
        FeedbackUtils.getSparseOptionsMap());
  }

  /**
   * Create a new feedback instance.
   * 
   * @param feedbackText the user feedback
   * @param osDetails OS details
   * @param productVersion the version of Dart Editor or Dart Plugins
   * @param logContents system log contents
   * @param isSdkInstalled {@code true} if the product has an installed SDK
   * @param isDartiumInstalled {@code true} if the product has the Dartium web browser installed
   */
  public FeedbackReport(String feedbackText, String productName, String osDetails,
      String productVersion, Stats stats, String logContents, Image screenshotImage,
      boolean isSdkInstalled, boolean isDartiumInstalled, Map<String, String> sparseOptionsMap) {
    this.feedbackText = feedbackText;
    this.productName = productName;
    this.osDetails = osDetails;
    this.productVersion = productVersion;
    this.stats = stats;
    this.logContents = logContents;
    this.screenshotImage = screenshotImage;
    this.isSdkInstalled = isSdkInstalled;
    this.isDartiumInstalled = isDartiumInstalled;
    this.sparseOptionsMap = sparseOptionsMap;
  }

  /**
   * Get any additional details that will be sent with this feedback report if the user chooses to
   * opt-in.
   * 
   * @return a detail string suitable for preview
   */
  public String getDetailString(boolean sendLogData) {
    PrintStringWriter writer = new PrintStringWriter();
    new FeedbackWriter(this, sendLogData, false).writeDetails(writer);
    writer.flush();
    return writer.toString();
  }

  public List<LogEntry> getLogEntries() {
    return LogReader.parseEntries(logContents);
  }

  /**
   * Get any interesting preferences and options that have been set to non-default values
   * 
   * @return text describing preferences and options with non-default values
   */
  public String getOptionsText() {
    StringBuilder msg = new StringBuilder();
    msg.append("SDK installed: " + isSdkInstalled() + "\n");
    msg.append("Dartium installed: " + isDartiumInstalled() + "\n");

    if (DartCoreDebug.EXPERIMENTAL) {
      msg.append("Experimental: true\n");
    }

    return msg.toString().trim();
  }

  public String getProductName() {
    return productName;
  }

  public Stats getStats() {
    return stats;
  }

  /**
   * Answer information about the current session or an empty string if none.
   */
  public String getStatsText() {
    return stats != null ? stats.toString() : "";
  }

  /**
   * Update the user supplied feedback text.
   * 
   * @param feedbackText the updated feedback text
   */
  public void setFeedbackText(String feedbackText) {
    this.feedbackText = feedbackText;
  }

  public void setUserEmail(String emailAddress) {
    userEmail = emailAddress;
  }

  String getFeedbackText() {
    return feedbackText;
  }

  Image getImage() {
    return screenshotImage;
  }

  String getJvmDetails() {
    return System.getProperties().getProperty("java.version");
  }

  String getLogContents() {
    return logContents;
  }

  String getOsDetails() {
    return osDetails;
  }

  String getProductVersion() {
    return productVersion;
  }

  Map<String, String> getSparseOptionsMap() {
    return sparseOptionsMap;
  }

  String getUserEmail() {
    return userEmail;
  }

  boolean isDartiumInstalled() {
    return isDartiumInstalled;
  }

  boolean isSdkInstalled() {
    return isSdkInstalled;
  }
}
