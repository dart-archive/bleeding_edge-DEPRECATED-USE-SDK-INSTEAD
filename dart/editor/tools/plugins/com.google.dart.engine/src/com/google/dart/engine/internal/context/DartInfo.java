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
 * Instances of the class {@code DartInfo} acts as a placeholder for Dart compilation units that
 * have not yet had their kind computed.
 * 
 * @coverage dart.engine
 */
public class DartInfo extends SourceInfo {
  /**
   * The instance of this class used to represent a compilation unit for which an attempt was made
   * to compute the kind but the kind could not be computed.
   */
  private static final DartInfo ErrorInstance = new DartInfo();

  /**
   * The instance of this class used to represent a compilation unit for which no attempt has been
   * made to compute the kind.
   */
  private static final DartInfo PendingInstance = new DartInfo();

  /**
   * Return an instance of this class representing a compilation unit for which an attempt was made
   * to compute the kind but the kind could not be computed.
   * 
   * @return an instance of this class used to indicate that the computation of the kind resulted in
   *         an error and there is no point trying again
   */
  public static DartInfo getErrorInstance() {
    return ErrorInstance;
  }

  /**
   * Return an instance of this class representing a compilation unit for which no attempt has been
   * made to compute the kind.
   * 
   * @return an instance of this class used to indicate that the computation of the kind is pending
   */
  public static DartInfo getPendingInstance() {
    return PendingInstance;
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
