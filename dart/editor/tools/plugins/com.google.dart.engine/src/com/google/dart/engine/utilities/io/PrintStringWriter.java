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
package com.google.dart.engine.utilities.io;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Instances of the class {@code PrintStringWriter} are a {@link PrintWriter} that stores the text
 * written to it in such a way that it can be returned as a {@link String}.
 * 
 * @coverage dart.engine.utilities
 */
public class PrintStringWriter extends PrintWriter {
  /**
   * The constant used to indicate that data should be left aligned.
   */
  public static final int ALIGN_LEFT = 0;

  /**
   * The constant used to indicate that data should be right aligned.
   */
  public static final int ALIGN_RIGHT = 2;

  /**
   * Initialize a newly created log entry writer.
   */
  public PrintStringWriter() {
    super(new StringWriter());
  }

  /**
   * Return the number of characters that have been written to this writer.
   * 
   * @return the number of characters that have been written to this writer
   */
  public int getLength() {
    return ((StringWriter) out).getBuffer().length();
  }

  /**
   * Print the given value in a field of the given width. If the length required by the value is
   * greater than the field width, then the field width and alignment will be ignored. Otherwise,
   * the alignment will be used to determine whether the spaces used to pad the value to the given
   * field width will be placed on the left or the right.
   * 
   * @param value the value to be printed
   * @param fieldWidth the width of the field in which it is to be printed
   * @param alignment the alignment of the value in the field
   */
  public void print(int value, int fieldWidth, int alignment) {
    print(Integer.toString(value), fieldWidth, alignment);
  }

  /**
   * Print the given value in a field of the given width. If the length required by the value is
   * greater than the field width, then the field width and alignment will be ignored. Otherwise,
   * the alignment will be used to determine whether the spaces used to pad the value to the given
   * field width will be placed on the left or the right.
   * 
   * @param value the value to be printed
   * @param fieldWidth the width of the field in which it is to be printed
   * @param alignment the alignment of the value in the field
   */
  public void print(String value, int fieldWidth, int alignment) {
    int padding;

    padding = fieldWidth - value.length();
    if (padding > 0) {
      if (alignment == ALIGN_LEFT) {
        print(value);
        for (int i = 0; i < padding; i++) {
          print(' ');
        }
      } else {
        for (int i = 0; i < padding; i++) {
          print(' ');
        }
        print(value);
      }
    } else {
      print(value);
    }
  }

  /**
   * Print the given value the given number of times.
   * 
   * @param count the number of times the value is to be printed
   * @param value the value to be written
   */
  public void printMultiple(int count, String value) {
    for (int i = 0; i < count; i++) {
      print(value);
    }
  }

  /**
   * Print the data in the given table to this writer. The table is an array of rows, where each row
   * is an array of cell values. The size of each row must be the same. The array of column
   * alignments indicates how the cell values are to be aligned within the columns. The values must
   * be one of the following constants: {@link #ALIGN_LEFT} or {@link #ALIGN_RIGHT}.
   * 
   * @return the text that has been written to this writer
   */
  public void printTable(String[][] data, int[] columnAlignments) {
    int rowCount, columnCount, padCount;
    int[] columnWidths;

    rowCount = data.length;
    columnCount = data[0].length;
    columnWidths = new int[columnCount];
    for (int i = 0; i < rowCount; i++) {
      for (int j = 0; j < columnCount; j++) {
        columnWidths[j] = Math.max(columnWidths[j], data[i][j].length());
      }
    }
    for (int i = 0; i < rowCount; i++) {
      for (int j = 0; j < columnCount; j++) {
        if (j > 0) {
          print(' ');
        }
        padCount = columnWidths[j] - data[i][j].length();
        if (columnAlignments[j] == ALIGN_RIGHT) {
          for (int k = 0; k < padCount; k++) {
            print(' ');
          }
        }
        print(data[i][j]);
        if (columnAlignments[j] == ALIGN_LEFT) {
          for (int k = 0; k < padCount; k++) {
            print(' ');
          }
        }
      }
      println();
    }
  }

  /**
   * Set the length of the character sequence to the given length.
   * 
   * @see AbstractStringBuilder#setLength(int)
   */
  public void setLength(int newLength) {
    ((StringWriter) out).getBuffer().setLength(newLength);
  }

  /**
   * Return the text that has been written to this writer.
   * 
   * @return the text that has been written to this writer
   */
  @Override
  public String toString() {
    return out.toString();
  }
}
