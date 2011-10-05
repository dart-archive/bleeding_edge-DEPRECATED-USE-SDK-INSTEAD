/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.ui.internal.client;

/**
 * Dart launch configuration constants
 */
public interface ILaunchConstants {

  /**
   * The id for the Dart JavaScript launch configuration type
   */
  public static final String LAUNCH_CONFIG_TYPE = "com.google.dart.tools.debug.core.clientLaunchConfig"; //$NON-NLS-1$

  /**
   * The launch type launch configuration attribute key
   */
  public static final String ATTR_LAUNCH_TYPE = "launchType";

  /**
   * A constant indicating that the Dart application should be launched as a web client wrapped in
   * an automatically generated web page
   */
  public static final int LAUNCH_TYPE_WEB_CLIENT = 0;

  /**
   * A constant indicating that the Dart application should be launched as a server application
   */
  public static final int LAUNCH_TYPE_SERVER_APP = 1;

  /**
   * The resource path launch configuration attribute key
   */
  public static final String ATTR_RESOURCE_PATH = "resourcePath";

  /**
   * A true/false indicating whether the web client should be displayed in an external web browser
   * (e.g. Chrome) or embedded in Eclipse.
   */
  public static final String ATTR_EXTERNAL_BROWSER = "externalBrowser";
}
