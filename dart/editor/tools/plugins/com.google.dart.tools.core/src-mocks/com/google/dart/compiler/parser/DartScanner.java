/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.compiler.parser;

/**
 *
 */
public class DartScanner {

  public static class Location {

    public static final Location NONE = null;
    private int begin;
    private int end;

    public Location(int begin) {
      this.begin = this.end = begin;
    }

    public Location(int begin, int end) {
      this.begin = begin;
      this.end = end;
    }

    public int getBegin() {
      return begin;
    }

    public int getEnd() {
      return end;
    }

    @Override
    public String toString() {
      return begin + "::" + end;
    }
  }

  public DartScanner(String src) {
  }

  public Location getTokenLocation() {
    return null;
  }

  public Token next() {
    return null;
  }

}
