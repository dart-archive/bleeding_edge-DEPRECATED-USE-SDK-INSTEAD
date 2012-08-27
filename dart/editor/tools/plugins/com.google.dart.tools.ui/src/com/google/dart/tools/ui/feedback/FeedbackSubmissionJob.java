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

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Submits feedback as a POST to an appengine back-end.
 * <p>
 * The flow here is:
 * <li>ping the server. this ensures that there is a route to the server, and resolves any http
 * forwarding
 * <li>submit feedback
 */
public class FeedbackSubmissionJob extends Job {

  private final FeedbackWriter writer;

  public FeedbackSubmissionJob(FeedbackWriter writer) {
    super(FeedbackMessages.FeedbackSubmissionJob_sending_feedback_job_label);
    this.writer = writer;
  }

  public String[] getBaseUrlDomains() {
    String domains = getResourceString("baseURLs"); //$NON-NLS-1$

    return FeedbackUtils.splitString(domains, ",", true); //$NON-NLS-1$
  }

  public String[] getFeedbackSubmissionUrls() {
    String urls = getResourceString("feedbackURLs"); //$NON-NLS-1$

    return FeedbackUtils.splitString(urls, ",", true); //$NON-NLS-1$
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    final URL serverURL = pingServer();

    // If we can't reach the server, return an error code
    if (serverURL == null) {
      //TODO (pquitslund): consider a retry
      return DartToolsPlugin.createErrorStatus(FeedbackMessages.FeedbackSubmissionJob_unreachable_server_error_text);
    }

    try {
      // Attempt to upload.
      //long startTime = System.currentTimeMillis();
      submitFeedback(serverURL, monitor);

//      if (Activator.DEBUG) {
//        Activator.log((dataLength / 1024) + "K feedback data uploaded in "
//            + (System.currentTimeMillis() - startTime) + " ms.");
//      }
    } catch (Throwable exception) {
      // this should not be show to the user
      DartToolsPlugin.log("error sending feedback", exception);
      monitor.done();
    }

    return Status.OK_STATUS;
  }

  private String getResourceString(String key) {
    return Platform.getResourceString(DartToolsPlugin.getDefault().getBundle(), "%" + key); //$NON-NLS-1$
  }

  /**
   * Ping the usage profiler server - make sure that we have a network connection and the server is
   * reachable. This method returns the server URL, taking into account any redirects.
   * 
   * @return the URL to use if the server is reachable, null otherwise
   */
  private URL pingServer() {
    String[] urls = getFeedbackSubmissionUrls();

    for (int i = 0; i < urls.length; i++) {
      try {
        String url = urls[i];

        URL respondedUrl = pingUrl(url);

        if (respondedUrl != null) {
          if (verifyUrl(respondedUrl)) {
            return respondedUrl;
          }
        }
      } catch (IOException ioe) {
        // ignore this -
      }
    }
    return null;
  }

  private URL pingUrl(String urlText) throws IOException {
    URL url = new URL(urlText);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setInstanceFollowRedirects(true);

    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      return null;
    }

    connection.disconnect();

    URL redirectedURL = connection.getURL();

    return redirectedURL;
  }

  private void safeClose(InputStream in) {
    try {
      in.close();
    } catch (IOException exception) {

    }
  }

  private void safeClose(OutputStream out) {
    try {
      out.close();
    } catch (IOException exception) {

    }
  }

  private int submitFeedback(URL serverURL, IProgressMonitor monitor) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);

    writer.writeFeedback(bout);

    byte[] data = bout.toByteArray();
    String crc32Hash = FeedbackUtils.calculateCRC32(data);

    monitor.beginTask(
        FeedbackMessages.FeedbackSubmissionJob_job_starting_progress_text,
        data.length);

    HttpURLConnection connection = (HttpURLConnection) serverURL.openConnection();

    connection.setInstanceFollowRedirects(false);
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestMethod("POST"); //$NON-NLS-1$
    connection.setFixedLengthStreamingMode(data.length);
    connection.setUseCaches(false);
    connection.setAllowUserInteraction(false);
    connection.setRequestProperty("Connection", "close"); //$NON-NLS-1$ //$NON-NLS-2$
    connection.setRequestProperty("Content-Type", "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$

    if (crc32Hash != null) {
      connection.setRequestProperty("X-Signature", crc32Hash); //$NON-NLS-1$
    }

    DataOutputStream out = new DataOutputStream(connection.getOutputStream());

    final int packetSize = 1500;

    for (int i = 0; i < data.length; i += packetSize) {
      int writeSize = Math.min(data.length - i, packetSize);

      out.write(data, i, writeSize);

      monitor.worked(writeSize);
    }

    out.flush();
    safeClose(out);

    InputStream in = connection.getInputStream();
    byte[] temp = new byte[8192];
    String str = null;
    int len = in.read(temp);
    if (len > 0) {
      str = new String(temp, 0, len);
    }
    safeClose(in);

    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      DartToolsPlugin.log("Error sending feedback:" + connection.getResponseMessage() + "\n" + str);//$NON-NLS-1$ //$NON-NLS-2$
    }

    connection.disconnect();

    return data.length;
  }

  /**
   * Verify that the URL is in the correct domain - it may have come from a URL redirection.
   * 
   * @param url
   * @return
   */
  private boolean verifyUrl(URL url) {
    String urlHost = url.getHost();

    if (urlHost == null) {
      return false;
    }

    // Make sure the url is in the correct domain - i.e. that it ends with "google.com".
    String[] domains = getBaseUrlDomains();

    for (int i = 0; i < domains.length; i++) {
      if (urlHost.endsWith("." + domains[i])) { //$NON-NLS-1$
        return true;
      }
    }

    return false;
  }

}
