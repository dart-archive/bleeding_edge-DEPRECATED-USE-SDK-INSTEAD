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
package com.google.dart.engine.mirrors;

import com.google.dart.engine.mirrors.core.Symbol;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

/**
 * A {@code MirrorSystem} is the main interface used to reflect on a set of associated libraries. At
 * runtime each running isolate has a distinct {@code MirrorSystem}. It is also possible to have a
 * {@code MirrorSystem} which represents a set of libraries which are not running -- perhaps at
 * compile-time. In this case, all available reflective functionality would be supported, but
 * runtime functionality (such as invoking a function or inspecting the contents of a variable)
 * would fail dynamically.
 */
public abstract class MirrorSystem {

  public static String getName(Symbol symbol) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns an iterable of all libraries in the mirror system whose library name is [libraryName].
   */
  public Iterable<LibraryMirror> findLibrary(Symbol libraryName) {
    ArrayList<LibraryMirror> matchingLibraries = new ArrayList<LibraryMirror>();
    for (LibraryMirror libraryMirror : getLibraries().values()) {
      if (libraryMirror.getSimpleName().equals(libraryName)) {
        matchingLibraries.add(libraryMirror);
      }
    }
    return matchingLibraries;
  }

  /**
   * A mirror on the {@code dynamic) type.

   */
  public abstract TypeMirror getDynamicType();

  /**
   * A mirror on the isolate associated with this {@code MirrorSystem}. This may be null if this
   * mirror system is not running.
   */
  public abstract IsolateMirror getIsolate();

  /**
   * An immutable map from from library names to mirrors for all libraries known to this mirror
   * system.
   */
  public abstract Map<URI, LibraryMirror> getLibraries();

  /**
   * A mirror on the {@code void} type.
   */
  public abstract TypeMirror getVoidType();
}
