/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.storage.inmemory;

public class PlainStringEncoder extends AbstractStringEncoder {
  private static byte char0(char x) {
    return (byte) (x >> 0);
  }

  private static byte char1(char x) {
    return (byte) (x >> 8);
  }

  static private char makeChar(byte b1, byte b0) {
    return (char) ((b1 << 8) | (b0 & 0xff));
  }

  @Override
  public String decode(ByteArray array, int position) {
    StringBuilder builder = new StringBuilder();
    byte[] data = array.data;
    for (int a = position; a < data.length; a++) {
      byte b = data[a];
      if (b == 0) {
        break;
      }
      if (b == 1) {
        a++;
        byte b0 = data[a];
        a++;
        byte b1 = data[a];
        builder.append(makeChar(b1, b0));
      } else {
        builder.append((char) b);
      }
    }
    return builder.toString();
  }

  @Override
  public void encode(String value, ByteArray array) {
    for (int a = 0; a < value.length(); a++) {
      char charAt = value.charAt(a);
      if (charAt < Byte.MAX_VALUE) {
        array.add((byte) charAt);
      } else {
        array.add((byte) 1);
        array.add(char0(charAt));
        array.add(char1(charAt));
      }
    }
    array.add((byte) 0);
  }
}
