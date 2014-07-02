/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.server.error;

import java.util.regex.Pattern;

/**
 * The enumeration {@code TodoCode} defines the single TODO {@code ErrorCode}.
 * 
 * @coverage dart.server.error
 */
public enum TodoCode implements ErrorCode {

  /**
   * The single enum of TodoCode.
   */
  TODO;

  /**
   * This matches the two common Dart task styles
   * <p>
   * <li>TODO:
   * <li>TODO(username):
   * <p>
   * As well as
   * <li>TODO
   * <p>
   * But not
   * <li>todo
   * <li>TODOS
   */
  // I would strongly recommend not editing this regex.
  public static Pattern TODO_REGEX = Pattern.compile("([\\s/\\*])((TODO[^\\w\\d][^\\r\\n]*)|(TODO:?$))");

  private TodoCode() {

  }

  @Override
  public String getUniqueName() {
    return getClass().getSimpleName() + '.' + name();
  }
}
