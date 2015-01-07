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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.CreateContextConsumer;
import com.google.dart.server.ExtendedRequestErrorCode;
import com.google.dart.server.generated.types.RequestError;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Instances of the class {@code CreateContextProcessor} process the result of an
 * {@code execution.createContext} request.
 */
public class CreateContextProcessor extends ResultProcessor {
  /**
   * The consumer that will be notified when a result is processed.
   */
  private CreateContextConsumer consumer;

  /**
   * Initialize a newly create result processor to process the result of an
   * {@code execution.createContext} request.
   * 
   * @param consumer the consumer that will be notified when a result is processed
   */
  public CreateContextProcessor(CreateContextConsumer consumer) {
    this.consumer = consumer;
  }

  /**
   * Process the given result.
   * 
   * @param resultObject the result object to be processed
   */
  public void process(JsonObject resultObject, RequestError requestError) {
    if (resultObject != null) {
      try {
        String contextId = resultObject.get("id").getAsString();
        consumer.computedExecutionContext(contextId);
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
