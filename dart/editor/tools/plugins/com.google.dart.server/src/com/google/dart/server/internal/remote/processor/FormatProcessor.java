/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.ExtendedRequestErrorCode;
import com.google.dart.server.FormatConsumer;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.generated.types.SourceEdit;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

/**
 * Instances of {@code FormatProcessor} translate JSON result objects for a given
 * {@link FormatConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class FormatProcessor extends ResultProcessor {
  private final FormatConsumer consumer;

  public FormatProcessor(FormatConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject, RequestError requestError) {
    if (resultObject != null) {
      try {
        List<SourceEdit> edits = SourceEdit.fromJsonArray(resultObject.get("edits").getAsJsonArray());
        int selectionOffset = resultObject.get("selectionOffset").getAsInt();
        int selectionLength = resultObject.get("selectionLength").getAsInt();
        consumer.computedFormat(edits, selectionOffset, selectionLength);
      } catch (Exception exception) {
        // catch any exceptions in the formatting of this response
        String message = exception.getMessage();
        String stackTrace = null;
        if (exception.getStackTrace() != null) {
          stackTrace = ExceptionUtils.getStackTrace(exception);
        }
        requestError = new RequestError(
            ExtendedRequestErrorCode.INVALID_SERVER_RESPONSE,
            message != null ? message : "",
            stackTrace);
      }
    }
    if (requestError != null) {
      consumer.onError(requestError);
    }
  }
}
