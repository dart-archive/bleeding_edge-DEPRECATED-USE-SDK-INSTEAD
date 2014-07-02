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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.ErrorSeverity;
import com.google.dart.server.ErrorType;
import com.google.dart.server.Location;
import com.google.dart.server.error.AngularCode;
import com.google.dart.server.error.CompileTimeErrorCode;
import com.google.dart.server.error.ErrorCode;
import com.google.dart.server.error.HintCode;
import com.google.dart.server.error.HtmlWarningCode;
import com.google.dart.server.error.ParserErrorCode;
import com.google.dart.server.error.PolymerCode;
import com.google.dart.server.error.PubSuggestionCode;
import com.google.dart.server.error.ResolverErrorCode;
import com.google.dart.server.error.ScannerErrorCode;
import com.google.dart.server.error.StaticTypeWarningCode;
import com.google.dart.server.error.StaticWarningCode;
import com.google.dart.server.error.TodoCode;
import com.google.dart.server.internal.AnalysisErrorImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;

/**
 * Processor for "analysis.errors" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationAnalysisErrorsProcessor extends NotificationProcessor {
  /**
   * Return the {@link ErrorCode} code for the given name, {@code null} if cannot be parsed.
   */
  @VisibleForTesting
  public static ErrorCode getErrorCode(String errorName) {
    String errorCodeClassName = StringUtils.substringBefore(errorName, ".");
    String errorCodeName = StringUtils.substringAfter(errorName, ".");
    if (errorCodeClassName.equals("AngularCode")) {
      return AngularCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("CompileTimeErrorCode")) {
      return CompileTimeErrorCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("HintCode")) {
      return HintCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("HtmlWarningCode")) {
      return HtmlWarningCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("ParserErrorCode")) {
      return ParserErrorCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("PolymerCode")) {
      return PolymerCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("PubSuggestionCode")) {
      return PubSuggestionCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("ResolverErrorCode")) {
      return ResolverErrorCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("ScannerErrorCode")) {
      return ScannerErrorCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("StaticTypeWarningCode")) {
      return StaticTypeWarningCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("StaticWarningCode")) {
      return StaticWarningCode.valueOf(errorCodeName);
    }
    if (errorCodeClassName.equals("TodoCode")) {
      return TodoCode.valueOf(errorCodeName);
    }
    return null;
  }

  public NotificationAnalysisErrorsProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    String file = paramsObject.get("file").getAsString();
    // prepare error objects iterator
    JsonElement errorsElement = paramsObject.get("errors");
    Iterator<JsonElement> errorElementIterator = errorsElement.getAsJsonArray().iterator();
    // convert errors
    List<AnalysisError> analysisErrors = Lists.newArrayList();
    while (errorElementIterator.hasNext()) {
      JsonObject errorObject = errorElementIterator.next().getAsJsonObject();
      AnalysisError analysisError = constructAnalysisError(errorObject);
      if (analysisError != null) {
        analysisErrors.add(analysisError);
      }
    }
    // notify listener
    getListener().computedErrors(
        file,
        analysisErrors.toArray(new AnalysisError[analysisErrors.size()]));
  }

  private AnalysisError constructAnalysisError(JsonObject errorObject) {
    ErrorCode errorCode = getErrorCode(errorObject.get("errorCode"));
    if (errorCode != null) {
      ErrorSeverity errorSeverity = ErrorSeverity.valueOf(errorObject.get("severity").getAsString());
      ErrorType errorType = ErrorType.valueOf(errorObject.get("type").getAsString());
      Location location = constructLocation(errorObject.get("location").getAsJsonObject());
      String message = errorObject.get("message").getAsString();
      String correction = safelyGetAsString(errorObject, "correction");
      return new AnalysisErrorImpl(
          errorCode,
          errorSeverity,
          errorType,
          location,
          message,
          correction);
    }
    return null;
  }

  /**
   * Attempt to convert the given {@link JsonElement} into an {@link ErrorCode}.
   * 
   * @return the {@link ErrorCode} or {@code null}
   */
  private ErrorCode getErrorCode(JsonElement element) {
    String errorName = element.getAsString();
    return getErrorCode(errorName);
  }
}
