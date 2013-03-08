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
package com.google.dart.engine.source;

/**
 * The enumeration {@code SourceKind} defines the different kinds of sources that are known to the
 * analysis engine.
 * 
 * @coverage dart.engine.source
 */
public enum SourceKind {
  /**
   * A source containing HTML. The HTML might or might not contain Dart scripts.
   */
  HTML,

  /**
   * A Dart compilation unit that is not a part of another library. Libraries might or might not
   * contain any directives, including a library directive.
   */
  LIBRARY,

  /**
   * A Dart compilation unit that is part of another library. Parts contain a part-of directive.
   */
  PART,

  /**
   * An unknown kind of source. Used both when it is not possible to identify the kind of a source
   * and also when the kind of a source is not known without performing a computation and the client
   * does not want to spend the time to identify the kind.
   */
  UNKNOWN;
}
