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
package com.google.dart.indexer.pagedstorage.util;

import com.google.dart.indexer.pagedstorage.exceptions.PagedStorageException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility class for object creation and serialization. Starting with Java 1.5, some objects are
 * re-used.
 */
public class ObjectUtils {
  /**
   * The maximum number of elements to copy using a Java loop. This value was found by running tests
   * using the Sun JDK 1.4 and JDK 1.6 on Windows XP. The biggest difference is for size smaller
   * than 40 (more than 50% saving).
   */
  private static final int MAX_JAVA_LOOP_COPY = 50;

  /**
   * Copy the elements of the source array to the target array. System.arraycopy is used for larger
   * arrays, but for very small arrays it is faster to use a regular loop.
   * 
   * @param source the source array
   * @param target the target array
   * @param size the number of elements to copy
   */
  public static void arrayCopy(Object[] source, Object[] target, int size) {
    if (size > MAX_JAVA_LOOP_COPY) {
      System.arraycopy(source, 0, target, 0, size);
    } else {
      for (int i = 0; i < size; i++) {
        target[i] = source[i];
      }
    }
  }

  /**
   * De-serialize the byte array to an object.
   * 
   * @param data the byte array
   * @return the object
   * @throws PagedStorageException
   */
  public static Object deserialize(byte[] data) throws PagedStorageException {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(data);
      ObjectInputStream is = new ObjectInputStream(in);
      Object obj = is.readObject();
      return obj;
    } catch (Throwable e) {
      throw new RuntimeException(e);
      // throw
      // Message.getPagedMemoryException(ErrorCode.DESERIALIZATION_FAILED_1, new
      // String[] { e.toString() }, e);
    }
  }

  /**
   * Calculate the hash code of the given object. The object may be null.
   * 
   * @param o the object
   * @return the hash code, or 0 if the object is null
   */
  public static int hashCode(Object o) {
    return o == null ? 0 : o.hashCode();
  }

  /**
   * Serialize the object to a byte array.
   * 
   * @param obj the object to serialize
   * @return the byte array
   */
  public static byte[] serialize(Object obj) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(out);
      os.writeObject(obj);
      return out.toByteArray();
    } catch (Throwable e) {
      throw new RuntimeException(e);
      // throw Message.getPagedMemoryException(ErrorCode.SERIALIZATION_FAILED_1,
      // new String[] { e.toString() }, e);
    }
  }

  private ObjectUtils() {
    // utility class
  }
}
