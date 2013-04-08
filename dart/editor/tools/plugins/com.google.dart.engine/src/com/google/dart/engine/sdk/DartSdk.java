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
package com.google.dart.engine.sdk;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code DartSdk} represent a Dart SDK installed in a specified location.
 * 
 * @coverage dart.engine.sdk
 */
public interface DartSdk {
  /**
   * The short name of the dart SDK core library.
   */
  public static final String DART_CORE = "dart:core";

  /**
   * The short name of the dart SDK html library.
   */
  public static final String DART_HTML = "dart:html";

  /**
   * Return the {@link AnalysisContext} used for all of the sources in this {@link DartSdk}.
   * 
   * @return the {@link AnalysisContext} used for all of the sources in this {@link DartSdk}
   */
  public AnalysisContext getContext();

  /**
   * Return an array containing all of the libraries defined in this SDK.
   * 
   * @return the libraries defined in this SDK
   */
  public SdkLibrary[] getSdkLibraries();

  /**
   * Return the revision number of this SDK, or {@code "0"} if the revision number cannot be
   * discovered.
   * 
   * @return the revision number of this SDK
   */
  public String getSdkVersion();

  /**
   * Return an array containing the library URI's for the libraries defined in this SDK.
   * 
   * @return the library URI's for the libraries defined in this SDK
   */
  public String[] getUris();

  /**
   * Return the source representing the library with the given {@code dart:} URI, or {@code null} if
   * the given URI does not denote a library in this SDK.
   * 
   * @param contentCache the content cache used to access the contents of the mapped source
   * @param dartUri the URI of the library to be returned
   * @return the source representing the specified library
   */
  public Source mapDartUri(ContentCache contentCache, String dartUri);
}
