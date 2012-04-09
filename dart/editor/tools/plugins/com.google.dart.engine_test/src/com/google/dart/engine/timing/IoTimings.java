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
package com.google.dart.engine.timing;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class IoTimings extends TestCase {
  private Charset utf8Charset;

  private static final int REPETITION_COUNT = 500;

  public void test_file_read() throws IOException {
    // Replace the path below with a valid path.
    String fileName = ".../src/dart-public/dart/lib/html/html_dartium.dart";
    assertTrue(new File(fileName).exists());
    utf8Charset = Charset.forName("UTF-8");

    long[] readerTotal = new long[] {0, 0, 0};
    long[] bufferTotal = new long[] {0, 0, 0, 0};
    long[] bytesTotal = new long[] {0, 0, 0, 0};
    long count = 0;
    for (int i = 0; i < REPETITION_COUNT; i++) {
      long[] reader = readFromReader(fileName);
      long[] buffer = readFromBuffer(fileName);
      long[] bytes = readFromBytes(fileName);
      if (i == 0 || i > 100) {
//        System.out.println("file_read: reader = " + reader[0] + ", buffer = " + buffer[0]
//            + ", bytes = " + bytes[0]);
        if (i > 100) {
          addTo(readerTotal, reader);
          addTo(bufferTotal, buffer);
          addTo(bytesTotal, bytes);
          count++;
        }
      }
    }
    //
    // Print average number of milliseconds
    //
    long divisor = count * 1000;
    System.out.println("file_read (average): reader = " + (readerTotal[0] / divisor) + " ms ("
        + (readerTotal[1] / divisor) + ", " + (readerTotal[2] / divisor) + "), buffer = "
        + (bufferTotal[0] / divisor) + " ms (" + (bufferTotal[1] / divisor) + ", "
        + (bufferTotal[2] / divisor) + ", " + (bufferTotal[3] / divisor) + "), bytes = "
        + (bytesTotal[0] / divisor) + " ms (" + (bytesTotal[1] / divisor) + ", "
        + (bytesTotal[2] / divisor) + ", " + (bytesTotal[3] / divisor) + ")");
  }

  private void addTo(long[] total, long[] count) {
    for (int i = 0; i < total.length; i++) {
      total[i] += count[i];
    }
  }

  private long[] readFromBuffer(String fileName) throws IOException {
    long start1 = System.nanoTime();
    RandomAccessFile file = new RandomAccessFile(fileName, "r");
    FileChannel channel = null;
    ByteBuffer byteBuffer = null;
    try {
      channel = file.getChannel();
      long size = channel.size();
      if (size > Integer.MAX_VALUE) {
        throw new IllegalStateException("File is too long to be read");
      }
      int length = (int) size;
      byte[] bytes = new byte[length];
      byteBuffer = ByteBuffer.wrap(bytes);
      byteBuffer.position(0);
      byteBuffer.limit(length);
      channel.read(byteBuffer);
    } finally {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException exception) {
          // Ignored
        }
      }
    }
    long end1 = System.nanoTime();

    long start2 = System.nanoTime();
    CharBuffer charBuffer = utf8Charset.decode(byteBuffer);
    long end2 = System.nanoTime();

    long start3 = System.nanoTime();
    int charLength = charBuffer.length();
    @SuppressWarnings("unused")
    char currentChar;
    for (int i = 0; i < charLength; i++) {
      currentChar = charBuffer.charAt(i);
    }
    long end3 = System.nanoTime();

    return new long[] {end3 - start1, end1 - start1, end2 - start2, end3 - start3};
  }

  private long[] readFromBytes(String fileName) throws IOException {
    long start1 = System.nanoTime();
    RandomAccessFile file = new RandomAccessFile(fileName, "r");
    FileChannel channel = null;
    ByteBuffer byteBuffer = null;
    byte[] bytes;
    try {
      channel = file.getChannel();
      long size = channel.size();
      if (size > Integer.MAX_VALUE) {
        throw new IllegalStateException("File is too long to be read");
      }
      int length = (int) size;
      bytes = new byte[length];
      byteBuffer = ByteBuffer.wrap(bytes);
      byteBuffer.position(0);
      byteBuffer.limit(length);
      channel.read(byteBuffer);
    } finally {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException exception) {
          // Ignored
        }
      }
    }
    long end1 = System.nanoTime();

    long start2 = System.nanoTime();
    int charLength = bytes.length;
    char[] output = new char[charLength];
    for (int i = 0; i < charLength; i++) {
      output[i] = (char) bytes[i];
    }
    long end2 = System.nanoTime();

    long start3 = System.nanoTime();
    @SuppressWarnings("unused")
    char currentChar;
    for (int i = 0; i < charLength; i++) {
      currentChar = output[i];
    }
    long end3 = System.nanoTime();

    return new long[] {end3 - start1, end1 - start1, end2 - start2, end3 - start3};
  }

  private long[] readFromReader(String fileName) throws IOException {
    long start1 = System.nanoTime();
    File file = new File(fileName);
    long size = file.length();
    if (size > Integer.MAX_VALUE) {
      throw new IllegalStateException("File is too long to be read");
    }
    int length = (int) size;
    char[] chars = new char[length];
    BufferedReader reader = null;
    int readCount = 0;
    try {
      reader = new BufferedReader(new FileReader(file));
      readCount = reader.read(chars);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException exception) {
          // Ignored
        }
      }
    }
    long end1 = System.nanoTime();

    long start2 = System.nanoTime();
    @SuppressWarnings("unused")
    char currentChar;
    for (int i = 0; i < readCount; i++) {
      currentChar = chars[i];
    }
    long end2 = System.nanoTime();

    assertEquals(length, readCount);
    return new long[] {end2 - start1, end1 - start1, end2 - start2};
  }
}
