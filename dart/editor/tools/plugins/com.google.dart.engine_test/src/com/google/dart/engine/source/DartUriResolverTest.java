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
import com.google.dart.engine.sdk.Platform;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;

public class DartUriResolverTest extends TestCase {
  public void test_DartUriResolver() {
    File sdkDirectory = DartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    DartSdk sdk = new DartSdk(sdkDirectory);
    Platform platform = Platform.getPlatform("dartium");
    assertNotNull(new DartUriResolver(sdk, platform));
  }

  public void test_DartUriResolver_resolve_dart() throws Exception {
    SourceFactory factory = new SourceFactory();
    File sdkDirectory = DartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    DartSdk sdk = new DartSdk(sdkDirectory);
    Platform platform = Platform.getPlatform("dartium");
    UriResolver resolver = new DartUriResolver(sdk, platform);
    Source result = resolver.resolve(factory, null, new URI("dart:core"));
    assertNotNull(result);
  }

  public void test_DartUriResolver_resolve_nonDart() throws Exception {
    SourceFactory factory = new SourceFactory();
    File sdkDirectory = DartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    DartSdk sdk = new DartSdk(sdkDirectory);
    Platform platform = Platform.getPlatform("dartium");
    UriResolver resolver = new DartUriResolver(sdk, platform);
    Source result = resolver.resolve(factory, null, new URI("package:some/file.dart"));
    assertNull(result);
  }
}
