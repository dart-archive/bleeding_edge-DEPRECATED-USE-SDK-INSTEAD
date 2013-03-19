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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.source.SourceKind;

/**
 * The unique instance of the class {@code DartInfo} acts as a placeholder for Dart compilation
 * units that have not yet had their kind computed.
 * 
 * @coverage dart.engine
 */
public class DartInfo extends SourceInfo {
  /**
   * The unique instance of this class.
   */
  private static final DartInfo UniqueInstance = new DartInfo();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DartInfo getInstance() {
    return UniqueInstance;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private DartInfo() {
    super();
  }

  @Override
  public DartInfo copy() {
    return this;
  }

  @Override
  public SourceKind getKind() {
    return SourceKind.UNKNOWN;
  }
}
