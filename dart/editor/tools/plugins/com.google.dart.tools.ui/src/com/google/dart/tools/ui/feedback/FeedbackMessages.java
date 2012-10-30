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

import org.eclipse.osgi.util.NLS;

public class FeedbackMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.feedback.FeedbackMessages"; //$NON-NLS-1$

  public static String FeedbackButtonControl_Text;

  public static String FeedbackControlContribution_control_tootip;
  public static String FeedbackDialog_Comments_Label;
  public static String FeedbackDialog_Description_Text;
  public static String FeedbackDialog_error_submitting_detail;

  public static String FeedbackDialog_error_submitting_label;
  public static String FeedbackDialog_feedback_sent_details;
  public static String FeedbackDialog_feedback_sent_label;

  public static String FeedbackDialog_error_opening_log_label;
  public static String FeedbackDialog_error_opening_log_detail;

  public static String FeedbackDialog_error_opening_screenshot_label;
  public static String FeedbackDialog_error_opening_screenshot_detail;

  public static String FeedbackDialog_OK_Button_Text;
  public static String FeedbackDialog_Title;
  public static String FeedbackDialog_send_additional_data_optin_Text;
  public static String FeedbackDialog_link_text;
  public static String FeedbackDialog_send_screenshot_optin_Text;
  public static String FeedbackDialog_link_screenshot_text;

  public static String FeedbackSubmissionJob_job_starting_progress_text_text;
  public static String FeedbackSubmissionJob_job_starting_progress_text_jpeg;
  public static String FeedbackSubmissionJob_sending_feedback_job_label;
  public static String FeedbackSubmissionJob_unreachable_server_error_text;

  public static String LogViewer_LogViewer_title;

  static {
    NLS.initializeMessages(BUNDLE_NAME, FeedbackMessages.class);
  }

  private FeedbackMessages() {
  }
}
