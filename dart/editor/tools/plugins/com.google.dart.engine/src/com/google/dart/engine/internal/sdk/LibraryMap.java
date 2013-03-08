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
package com.google.dart.engine.internal.sdk;

import com.google.dart.engine.sdk.SdkLibrary;

import java.util.HashMap;

/**
 * Instances of the class {@code LibraryMap} map Dart library URI's to the {@link SdkLibraryImpl
 * library}.
 * 
 * @coverage dart.engine.sdk
 */
public class LibraryMap {
  /**
   * A table mapping Dart library URI's to the library.
   */
  private HashMap<String, SdkLibraryImpl> libraryMap = new HashMap<String, SdkLibraryImpl>();

  /**
   * Initialize a newly created library map to be empty.
   */
  public LibraryMap() {
    super();
  }

  /**
   * Return the library with the given URI, or {@code null} if the URI does not map to a library.
   * 
   * @param dartUri the URI of the library to be returned
   * @return the library with the given URI
   */
  public SdkLibrary getLibrary(String dartUri) {
    return libraryMap.get(dartUri);
  }

  /**
   * Return an array containing all the sdk libraries {@link SdkLibraryImpl} in the mapping
   * 
   * @return the sdk libraries in the mapping
   */
  public SdkLibrary[] getSdkLibraries() {
    return libraryMap.values().toArray(new SdkLibraryImpl[libraryMap.size()]);
  }

  /**
   * Return an array containing the library URI's for which a mapping is available.
   * 
   * @return the library URI's for which a mapping is available
   */
  public String[] getUris() {
    return libraryMap.keySet().toArray(new String[libraryMap.size()]);
  }

  /**
   * Return the library with the given URI, or {@code null} if the URI does not map to a library.
   * 
   * @param dartUri the URI of the library to be returned
   * @param library the library with the given URI
   */
  public void setLibrary(String dartUri, SdkLibraryImpl library) {
    libraryMap.put(dartUri, library);
  }

  /**
   * Return the number of library URI's for which a mapping is available.
   * 
   * @return the number of library URI's for which a mapping is available
   */
  public int size() {
    return libraryMap.size();
  }
}
