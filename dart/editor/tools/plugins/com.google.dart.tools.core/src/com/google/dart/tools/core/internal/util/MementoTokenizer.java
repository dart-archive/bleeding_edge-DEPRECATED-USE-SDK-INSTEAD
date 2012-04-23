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
package com.google.dart.tools.core.internal.util;

import com.google.dart.tools.core.internal.model.DartElementImpl;

/**
 * Instances of the class <code>MementoTokenizer</code> tokenize a DartElementImpl memento.
 */
public class MementoTokenizer {
  public static final String COMPILATION_UNIT = Character.toString(DartElementImpl.MEMENTO_DELIMITER_COMPILATION_UNIT);
  public static final String COUNT = Character.toString(DartElementImpl.MEMENTO_DELIMITER_COUNT);
  public static final String FIELD = Character.toString(DartElementImpl.MEMENTO_DELIMITER_FIELD);
  public static final String FUNCTION = Character.toString(DartElementImpl.MEMENTO_DELIMITER_FUNCTION);
  public static final String FUNCTION_TYPE_ALIAS = Character.toString(DartElementImpl.MEMENTO_DELIMITER_FUNCTION_TYPE_ALIAS);
  public static final String HTML_FILE = Character.toString(DartElementImpl.MEMENTO_DELIMITER_HTML_FILE);
  public static final String IMPORT = Character.toString(DartElementImpl.MEMENTO_DELIMITER_IMPORT);
  public static final String IMPORT_CONTAINER = Character.toString(DartElementImpl.MEMENTO_DELIMITER_IMPORT_CONTAINER);
  public static final String LIBRARY = Character.toString(DartElementImpl.MEMENTO_DELIMITER_LIBRARY);
  public static final String LIBRARY_FOLDER = Character.toString(DartElementImpl.MEMENTO_DELIMITER_LIBRARY_FOLDER);
  public static final String METHOD = Character.toString(DartElementImpl.MEMENTO_DELIMITER_METHOD);
  public static final String PROJECT = Character.toString(DartElementImpl.MEMENTO_DELIMITER_PROJECT);
  public static final String TYPE = Character.toString(DartElementImpl.MEMENTO_DELIMITER_TYPE);
  public static final String TYPE_PARAMETER = Character.toString(DartElementImpl.MEMENTO_DELIMITER_TYPE_PARAMETER);
  public static final String VARIABLE = Character.toString(DartElementImpl.MEMENTO_DELIMITER_VARIABLE);

  private final char[] memento;
  private final int length;
  private int index = 0;

  public MementoTokenizer(String memento) {
    this.memento = memento.toCharArray();
    this.length = this.memento.length;
  }

  public boolean hasMoreTokens() {
    return this.index < this.length;
  }

  public String nextToken() {
    int start = this.index;
    StringBuffer buffer = null;
    switch (this.memento[this.index++]) {
      case DartElementImpl.MEMENTO_DELIMITER_ESCAPE:
        buffer = new StringBuffer();
        buffer.append(this.memento[this.index]);
        start = ++this.index;
        break;
      case DartElementImpl.MEMENTO_DELIMITER_COMPILATION_UNIT:
        return COMPILATION_UNIT;
      case DartElementImpl.MEMENTO_DELIMITER_COUNT:
        return COUNT;
      case DartElementImpl.MEMENTO_DELIMITER_FIELD:
        return FIELD;
      case DartElementImpl.MEMENTO_DELIMITER_TYPE_PARAMETER:
        return TYPE_PARAMETER;
      case DartElementImpl.MEMENTO_DELIMITER_FUNCTION:
        return FUNCTION;
      case DartElementImpl.MEMENTO_DELIMITER_FUNCTION_TYPE_ALIAS:
        return FUNCTION_TYPE_ALIAS;
      case DartElementImpl.MEMENTO_DELIMITER_HTML_FILE:
        return HTML_FILE;
      case DartElementImpl.MEMENTO_DELIMITER_IMPORT:
        return IMPORT;
      case DartElementImpl.MEMENTO_DELIMITER_IMPORT_CONTAINER:
        return IMPORT_CONTAINER;
      case DartElementImpl.MEMENTO_DELIMITER_LIBRARY:
        return LIBRARY;
      case DartElementImpl.MEMENTO_DELIMITER_LIBRARY_FOLDER:
        return LIBRARY_FOLDER;
      case DartElementImpl.MEMENTO_DELIMITER_METHOD:
        return METHOD;
      case DartElementImpl.MEMENTO_DELIMITER_PROJECT:
        return PROJECT;
      case DartElementImpl.MEMENTO_DELIMITER_TYPE:
        return TYPE;
      case DartElementImpl.MEMENTO_DELIMITER_VARIABLE:
        return VARIABLE;
    }
    loop : while (this.index < this.length) {
      switch (this.memento[this.index]) {
        case DartElementImpl.MEMENTO_DELIMITER_ESCAPE:
          if (buffer == null) {
            buffer = new StringBuffer();
          }
          buffer.append(this.memento, start, this.index - start);
          start = ++this.index;
          break;
        case DartElementImpl.MEMENTO_DELIMITER_COMPILATION_UNIT:
        case DartElementImpl.MEMENTO_DELIMITER_COUNT:
        case DartElementImpl.MEMENTO_DELIMITER_FIELD:
        case DartElementImpl.MEMENTO_DELIMITER_FUNCTION:
        case DartElementImpl.MEMENTO_DELIMITER_FUNCTION_TYPE_ALIAS:
        case DartElementImpl.MEMENTO_DELIMITER_HTML_FILE:
        case DartElementImpl.MEMENTO_DELIMITER_IMPORT:
        case DartElementImpl.MEMENTO_DELIMITER_IMPORT_CONTAINER:
        case DartElementImpl.MEMENTO_DELIMITER_LIBRARY:
        case DartElementImpl.MEMENTO_DELIMITER_LIBRARY_FOLDER:
        case DartElementImpl.MEMENTO_DELIMITER_METHOD:
        case DartElementImpl.MEMENTO_DELIMITER_PROJECT:
        case DartElementImpl.MEMENTO_DELIMITER_TYPE:
        case DartElementImpl.MEMENTO_DELIMITER_TYPE_PARAMETER:
        case DartElementImpl.MEMENTO_DELIMITER_VARIABLE:
          break loop;
      }
      this.index++;
    }
    if (buffer != null) {
      buffer.append(this.memento, start, this.index - start);
      return buffer.toString();
    } else {
      return new String(this.memento, start, this.index - start);
    }
  }
}
