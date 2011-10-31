/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;


/**
 * A singleton which caches artifacts for the session
 */
public class RootArtifactProvider extends CachingArtifactProvider {
  private static final RootArtifactProvider INSTANCE = new RootArtifactProvider();

  /**
   * Answer the root artifact provider shared by all throughout the session.
   */
  public static RootArtifactProvider getInstance() {
    return INSTANCE;
  }

  /**
   * Answer a new instance for testing purposes only
   */
  public static RootArtifactProvider newInstanceForTesting() {
    return new RootArtifactProvider();
  }

  private RootArtifactProvider() {
  }
}
