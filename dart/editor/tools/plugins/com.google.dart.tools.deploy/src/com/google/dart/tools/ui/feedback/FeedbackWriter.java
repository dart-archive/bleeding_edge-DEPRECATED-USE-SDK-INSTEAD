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

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Prints formatted representations of user feedback to an output stream.
 */
public class FeedbackWriter {

  private static final String GROUP_DELIMETER = "////////////////////////////////////////////////////////////////////////////////////"; //$NON-NLS-1$

  private final FeedbackReport feedback;

  private final boolean sendAdditionData;

  /**
   * Create a writer for this report. The default is to not send additional editor data.
   * 
   * @param feedback the report
   */
  public FeedbackWriter(FeedbackReport feedback) {
    this(feedback, false);
  }

  /**
   * Create a writer for this report.
   * 
   * @param feedback the report
   * @param sendAdditionData whether to send additional editor log data
   */
  public FeedbackWriter(FeedbackReport feedback, boolean sendAdditionData) {
    this.feedback = feedback;
    this.sendAdditionData = sendAdditionData;
  }

  /**
   * Write feedback to the given output stream.
   * 
   * @param out the output stream
   */
  public void writeFeedback(OutputStream out) {
    PrintWriter writer = new PrintWriter(out);
    doWrite(writer);
    writer.flush();
  }

  /**
   * Write feedback details to the given writer. NOTE: these details will only be sent if the user
   * elects to opt-in.
   * 
   * @param writer the writer
   */
  void writeDetails(PrintWriter writer) {
    writer.println("Editor: " + feedback.getEditorVersion()); //$NON-NLS-1$
    writer.println("OS: " + feedback.getOsDetails()); //$NON-NLS-1$
    writer.println("JVM: " + System.getProperties().getProperty("java.version")); //$NON-NLS-1$
    writer.println();
    writer.println(feedback.getOptionsText());
    if (sendAdditionData) {
      writeGroupDelim(writer);
      writer.println(feedback.getLogContents());
    }
  }

  /**
   * Write user generated feedback text to the given writer.
   * 
   * @param writer the writer
   */
  void writeFeedbackText(PrintWriter writer) {
    writer.println(feedback.getFeedbackText());
  }

  private void doWrite(PrintWriter writer) {
    writeFeedbackText(writer);
    writeGroupDelim(writer);
    writeDetails(writer);
  }

  private void writeGroupDelim(PrintWriter writer) {
    writer.println(GROUP_DELIMETER);
  }

}
