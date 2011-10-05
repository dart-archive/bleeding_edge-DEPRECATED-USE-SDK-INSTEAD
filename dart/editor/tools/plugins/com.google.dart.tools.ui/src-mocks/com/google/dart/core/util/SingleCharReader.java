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
package com.google.dart.core.util;

import java.io.IOException;
import java.io.Reader;

public abstract class SingleCharReader extends Reader {

  /**
   * Returns the readable content as string.
   * 
   * @return the readable content as string
   * @exception IOException in case reading fails
   */
  public String getString() throws IOException {
    StringBuffer buf = new StringBuffer();
    int ch;
    while ((ch = read()) != -1) {
      buf.append((char) ch);
    }
    return buf.toString();
  }

  /**
   * @see Reader#read()
   */
  @Override
  public abstract int read() throws IOException;

  /**
   * @see Reader#read(char[],int,int)
   */
  @Override
  public int read(char cbuf[], int off, int len) throws IOException {
    int end = off + len;
    for (int i = off; i < end; i++) {
      int ch = read();
      if (ch == -1) {
        if (i == off) {
          return -1;
        }
        return i - off;
      }
      cbuf[i] = (char) ch;
    }
    return len;
  }

  /**
   * @see Reader#ready()
   */
  @Override
  public boolean ready() throws IOException {
    return true;
  }
}
