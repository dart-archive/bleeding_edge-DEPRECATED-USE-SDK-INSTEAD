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
package com.google.dart.tools.ui;

import java.io.IOException;
import java.io.Reader;

/**
 * TODO(devoncarew): This is a temporary class, used to resolve compilation errors.
 * <p>
 * see org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader
 */
public class DartDoc2HTMLTextReader extends Reader {
  private Reader reader;

  public DartDoc2HTMLTextReader(Reader reader) {
    this.reader = reader;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @Override
  public int read(char[] buffer, int offset, int length) throws IOException {
    return reader.read(buffer, offset, length);
  }

}
