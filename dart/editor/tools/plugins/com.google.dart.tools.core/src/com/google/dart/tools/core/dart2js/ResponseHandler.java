/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.dart2js;

import com.google.dart.tools.core.DartCore;

/**
 * Class for handling responses from requests made of a {@link JsonServer}. Subclasses should
 * implement {@link #processMessage(ResponseMessage)} and {@link #processDone(ResponseDone)}, and
 * optionally {@link #handleException(Response, Exception)}.
 */
public abstract class ResponseHandler {

  /**
   * Handle an exception that occurred while processing the specified response
   * 
   * @param response the response (not <code>null</code>)
   * @param exception the exception that occurred (not <code>null</code>)
   */
  public void handleException(Response response, Exception exception) {
    DartCore.logError("Exception processing response: " + response, exception);
  }

  /**
   * Process the specified response from {@link JsonServer}
   * 
   * @param response the response (not <code>null</code>)
   * @return <code>true</code> if this is the last response for this handler (e.g. "done")
   */
  public final boolean process(Response response) {
    boolean isDone = false;
    try {
      switch (response.getKind()) {
        case Done:
          isDone = true;
          processDone(response.createDoneResponse());
          break;

        case Message:
          processMessage(response.createMessageResponse());
          break;

        default:
          DartCore.logError("Unknown response kind: " + response);
          break;
      }
    } catch (Exception e) {
      handleException(response, e);
    }
    return isDone;
  }

  public void processDone(ResponseDone createDoneResponse) {
  }

  public void processMessage(ResponseMessage createMessageResponse) {
  }
}
