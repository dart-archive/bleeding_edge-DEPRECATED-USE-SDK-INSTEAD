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

import com.google.dart.server.SourceEdit;
import com.google.dart.server.SourceFileEdit;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link SourceFileEdit}.
 * 
 * @coverage dart.server
 */
public class SourceFileEditImpl implements SourceFileEdit {

  private final String file;
  private final SourceEdit[] edits;

  public SourceFileEditImpl(String file, SourceEdit[] edits) {
    this.file = file;
    this.edits = edits;
  }

  @Override
  public SourceEdit[] getEdits() {
    return edits;
  }

  @Override
  public String getFile() {
    return file;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[file=");
    builder.append(file);
    builder.append(", edits=");
    builder.append(StringUtils.join(edits, ", "));
    builder.append("]");
    return builder.toString();
  }

}
