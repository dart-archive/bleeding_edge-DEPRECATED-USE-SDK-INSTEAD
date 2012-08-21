/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.utilities.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Helper for hex-decimal encoding and decoding.
 */
public class Base16 {
  /**
   * @return the {@link Object} from the {@link String} created by {@link #encodeObject(Object)}.
   */
  public static <T> T decodeToObject(String str) throws Exception {
    byte[] bytes = hexToBytes(str);
    return objectFromBytes(bytes);
  }

  /**
   * @return the hex encoding of the given {@link Object}.
   */
  public static String encodeObject(Object o) throws Exception {
    byte[] byteArray = objectToBytes(o);
    return bytesToHex(byteArray);
  }

  private static void appendByteHex(StringBuilder sb, byte b) {
    String s = Integer.toHexString(b);
    int len = s.length();
    if (len < 2) {
      sb.append('0');
      sb.append(s);
    } else if (len == 2) {
      sb.append(s);
    } else {
      sb.append(s.substring(len - 2, len));
    }
  }

  private static String bytesToHex(byte[] byteArray) {
    StringBuilder sb = new StringBuilder(byteArray.length * 2);
    for (byte b : byteArray) {
      appendByteHex(sb, b);
    }
    return sb.toString();
  }

  private static byte hexToByte(String s) {
    int v = Integer.parseInt(s, 16);
    return (byte) v;
  }

  private static byte[] hexToBytes(String str) {
    int len = str.length();
    byte[] bytes = new byte[len / 2];
    for (int i = 0; i < len / 2; i++) {
      String s = str.substring(i * 2, i * 2 + 2);
      bytes[i] = hexToByte(s);
    }
    return bytes;
  }

  @SuppressWarnings("unchecked")
  private static <T> T objectFromBytes(byte[] bytes) throws Exception {
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    ObjectInputStream ois = new ObjectInputStream(bais);
    try {
      return (T) ois.readObject();
    } finally {
      ois.close();
    }
  }

  private static byte[] objectToBytes(Object o) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    return baos.toByteArray();
  }
}
