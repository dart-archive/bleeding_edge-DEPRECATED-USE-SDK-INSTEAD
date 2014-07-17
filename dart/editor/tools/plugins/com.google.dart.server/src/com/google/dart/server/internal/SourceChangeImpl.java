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
package com.google.dart.server.internal;

import com.google.dart.server.SourceChange;
import com.google.dart.server.SourceFileEdit;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link SourceChange}.
 * 
 * @coverage dart.server
 */
public class SourceChangeImpl implements SourceChange {

  private final String message;
  private final SourceFileEdit[] edits;

  public SourceChangeImpl(String message, SourceFileEdit[] edits) {
    this.message = message;
    this.edits = edits;
  }

  @Override
  public SourceFileEdit[] getEdits() {
    return edits;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[message=");
    builder.append(message);
    builder.append(", edits=");
    builder.append(StringUtils.join(edits, ", "));
    builder.append("]");
    return builder.toString();
  }

}
