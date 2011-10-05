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
package com.google.dart.tools.core;

/**
 * A console to which messages can be printed. Unlike a system log, consoles are meant to be viewed
 * by end-users. Clients connect to the console via link {@link MessageStream}s.
 * 
 * @see #addStream(MessageStream)
 */
public interface MessageConsole {

  /**
   * A stream of console events. Streams are the means of connecting things like views to the
   * console.
   */
  interface MessageStream {

    /**
     * @see MessageConsole#clear()
     */
    void clear();

    /**
     * @see MessageConsole#print(String)
     */
    void print(String s);

    /**
     * @see MessageConsole#println()
     */
    void println();

    /**
     * @see MessageConsole#println(String)
     */
    void println(String s);

  }

  /**
   * Add this stream.
   * 
   * @param stream the stream to add
   */
  void addStream(MessageStream stream);

  /**
   * Clears the message console.
   */
  void clear();

  /**
   * Prints a string to the message console. If the argument is null then the string "null" is
   * printed. Otherwise, the string's characters are converted into bytes according to the
   * platform's default character encoding.
   * 
   * @param s the string to print
   */
  void print(String s);

  /**
   * Terminates the current line by writing the line separator string. The line separator string is
   * defined by the system property line.separator, and is not necessarily a single newline
   * character ('\n').
   */
  void println();

  /**
   * Prints a String to the message console and then terminates the line. This method behaves as
   * though it invokes print(String) and then println().
   * 
   * @param s the string to print
   */
  void println(String s);

  /**
   * Remove this stream.
   * 
   * @param stream
   */
  void removeStream(MessageStream stream);
}
