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
package com.google.dart.engine.source;

import com.google.dart.engine.sdk.DartSdk;

import java.net.URI;

/**
 * Instances of the class {@code DartUriResolver} resolve {@code dart} URI's.
 * 
 * @coverage dart.engine.source
 */
public class DartUriResolver extends UriResolver {
  /**
   * Return {@code true} if the given URI is a {@code dart-ext:} URI.
   * 
   * @param uriContent the textual representation of the URI being tested
   * @return {@code true} if the given URI is a {@code dart-ext:} URI
   */
  public static boolean isDartExtUri(String uriContent) {
    return uriContent != null && uriContent.startsWith(DART_EXT_SCHEME);
  }

  /**
   * The Dart SDK against which URI's are to be resolved.
   */
  private final DartSdk sdk;

  /**
   * The name of the {@code dart} scheme.
   */
  public static final String DART_SCHEME = "dart";

  /**
   * The prefix of a URI using the dart-ext scheme to reference a native code library.
   */
  private static final String DART_EXT_SCHEME = "dart-ext:";

  /**
   * Return {@code true} if the given URI is a {@code dart:} URI.
   * 
   * @param uri the URI being tested
   * @return {@code true} if the given URI is a {@code dart:} URI
   */
  public static boolean isDartUri(URI uri) {
    return DART_SCHEME.equals(uri.getScheme());
  }

  /**
   * Initialize a newly created resolver to resolve Dart URI's against the given platform within the
   * given Dart SDK.
   * 
   * @param sdk the Dart SDK against which URI's are to be resolved
   */
  public DartUriResolver(DartSdk sdk) {
    this.sdk = sdk;
  }

  /**
   * Return the {@link DartSdk} against which URIs are to be resolved.
   * 
   * @return the {@link DartSdk} against which URIs are to be resolved.
   */
  public DartSdk getDartSdk() {
    return sdk;
  }

  @Override
  public Source resolveAbsolute(URI uri) {
    if (!isDartUri(uri)) {
      return null;
    }
    return sdk.mapDartUri(uri.toString());
  }
}
