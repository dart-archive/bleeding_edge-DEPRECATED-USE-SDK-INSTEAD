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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/tool/spec/generate_files".
 */
package com.google.dart.server.generated.types;

/**
 * An enumeration of the types of errors that can occur in the execution of the server.
 *
 * @coverage dart.server.generated.types
 */
public class RequestErrorCode {

  /**
   * An "analysis.getErrors" request specified a FilePath which does not match a file currently
   * subject to analysis.
   */
  public static final String GET_ERRORS_INVALID_FILE = "GET_ERRORS_INVALID_FILE";

  /**
   * An analysis.updateContent request contained a ChangeContentOverlay object which can't be
   * applied, due to an edit having an offset or length that is out of range.
   */
  public static final String INVALID_OVERLAY_CHANGE = "INVALID_OVERLAY_CHANGE";

  /**
   * One of the method parameters was invalid.
   */
  public static final String INVALID_PARAMETER = "INVALID_PARAMETER";

  /**
   * A malformed request was received.
   */
  public static final String INVALID_REQUEST = "INVALID_REQUEST";

  /**
   * The analysis server has already been started (and hence won't accept new connections).
   *
   * This error is included for future expansion; at present the analysis server can only speak to
   * one client at a time so this error will never occur.
   */
  public static final String SERVER_ALREADY_STARTED = "SERVER_ALREADY_STARTED";

  /**
   * An internal error occurred in the analysis server. Also see the server.error notification.
   */
  public static final String SERVER_ERROR = "SERVER_ERROR";

  /**
   * An "analysis.setPriorityFiles" request includes one or more files that are not being analyzed.
   *
   * This is a legacy error; it will be removed before the API reaches version 1.0.
   */
  public static final String UNANALYZED_PRIORITY_FILES = "UNANALYZED_PRIORITY_FILES";

  /**
   * A request was received which the analysis server does not recognize, or cannot handle in its
   * current configuation.
   */
  public static final String UNKNOWN_REQUEST = "UNKNOWN_REQUEST";

  /**
   * The analysis server was requested to perform an action which is not supported.
   *
   * This is a legacy error; it will be removed before the API reaches version 1.0.
   */
  public static final String UNSUPPORTED_FEATURE = "UNSUPPORTED_FEATURE";

}
