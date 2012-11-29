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

package com.google.dart.tools.core.html;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.LinkedList;

/**
 * Public for testing.
 */
public class Tokenizer {

//  public static void main(String[] args) {
//    // <html>
//    // <head>
//    // <meta charset="utf-8">
//    final String data = "<html>\n<head>\n  <meta charset=\"utf-8\">\n</head>\n<p>what is this</p></html>\n";
//
//    Tokenizer tokenizer = new Tokenizer(data);
//
//    System.out.println("parsing data...");
//
//    while (tokenizer.hasNext()) {
//      System.out.println(tokenizer.next());
//    }
//  }

  private char[] buffer;

  private int position = 0;
  private int line = 1;

  private LinkedList<Token> tokens;
  private String[] passThroughElements;

  public Tokenizer(CharBuffer buffer) {
    this(buffer.toString());
  }

  public Tokenizer(Reader reader) throws IOException {
    this(CharStreams.toString(reader));

    reader.close();
  }

  public Tokenizer(String data) {
    this.buffer = data.toCharArray();
  }

  public boolean hasNext() {
    if (tokens == null) {
      parse();
    }

    return !tokens.isEmpty();
  }

  public Token next() {
    if (tokens == null) {
      parse();
    }

    return tokens.poll();
  }

  public Token peek() {
    if (tokens == null) {
      parse();
    }

    return tokens.peek();
  }

  public void setPassThroughElements(String[] passThroughElements) {
    this.passThroughElements = passThroughElements;
  }

  private Token emit(int count) {
    if (position + count > buffer.length) {
      count = buffer.length - position;
    }

    Token token = new Token(new String(buffer, position, count), position, line);

    tokens.add(token);

    for (int i = 0; i < count; i++) {
      if (buffer[position + i] == '\n') {
        line++;
      }
    }

    position += count;

    return token;
  }

  private boolean matchesPassThrough() {
    for (String str : passThroughElements) {
      int count = 0;

      while (Character.isWhitespace(peek(count))) {
        count++;
      }

      for (int i = 0; i <= str.length(); i++) {
        if (i == str.length()) {
          return true;
        }

        if (peek(count + i) != str.charAt(i)) {
          break;
        }
      }

    }

    return false;
  }

  private void parse() {
    tokens = new LinkedList<Token>();

    boolean inBrackets = false;
    boolean passThrough = false;

    // <--, -->, <?, <, >, =, "***", '***', in brackets, normal

    while (position < buffer.length) {
      final char c = peek(0);

      if (c == '<') {
        if (peek(1) == '!' && peek(2) == '-' && peek(3) == '-') {
          // handle a comment
          int count = 3;

          while (!(peek(count - 2) == '-' && peek(count - 1) == '-' && peek(count) == '>')
              && peek(count) != 0) {
            count++;
          }

          emit(count + 1);
        } else if (peek(1) == '!') {
          // handle a directive
          int count = 2;

          while (peek(count) != '>' && peek(count) != 0) {
            count++;
          }

          emit(count + 1);
        } else if (peek(1) == '?') {
          // handle a directive
          int count = 2;

          while (!(peek(count - 1) == '?' && peek(count) == '>') && peek(count) != 0) {
            count++;
          }

          emit(count + 1);
        } else if (peek(1) == '/') {
          emit(2);
          inBrackets = true;
        } else {
          inBrackets = true;

          emit(1);

          if (passThroughElements != null && peek(0) == 's') {
            passThrough = matchesPassThrough();
          }
        }
      } else if (c == '>') {
        emit(1);
        inBrackets = false;

        // if passThrough != null, read until we match it
        if (passThrough) {
          int count = 0;
          int peek = peek(count);

          while (peek != 0) {
            if (peek == '<' && peek(count + 1) == '/') {
              if (count > 0) {
                emit(count);
              }
              break;
            }

            count++;
            peek = peek(count);
          }

          passThrough = false;
        }
      } else if (c == '/' && peek(1) == '>') {
        emit(2);
        inBrackets = false;
      } else if (!inBrackets) {
        int count = 1;

        int peek = peek(count);

        while (peek != '<' && peek != 0) {
          count++;
          peek = peek(count);
        }

        emit(count);
      } else if (c == '"') {
        // read a string
        int count = 1;

        int peek = peek(count);

        while (peek != '"' && peek != 0) {
          count++;
          peek = peek(count);
        }

        if (peek == '"') {
          emit(count + 1);
        } else {
          emit(count);
        }
      } else if (c == '\'') {
        // read a string
        int count = 1;

        int peek = peek(count);

        while (peek != '\'' && peek != 0) {
          count++;
          peek = peek(count);
        }

        if (peek == '\'') {
          emit(count + 1);
        } else {
          emit(count);
        }
      } else if (Character.isWhitespace(c)) {
        int count = 1;

        while (Character.isWhitespace(peek(count))) {
          count++;
        }

        if (inBrackets) {
          // ignore whitespace
          position += count;
        } else {
          emit(count).setWhitespace(true);
        }
      } else if (Character.isLetterOrDigit(c)) {
        int count = 1;

        char peek = peek(count);

        while (Character.isLetterOrDigit(peek) || peek == '-' || peek == '_') {
          count++;
          peek = peek(count);
        }

        emit(count);
      } else {
        // a non-char token (=, ...)
        emit(1);
      }
    }
  }

  private char peek(int lookAhead) {
    if (position + lookAhead >= buffer.length) {
      return 0;
    } else {
      return buffer[position + lookAhead];
    }
  }

}
