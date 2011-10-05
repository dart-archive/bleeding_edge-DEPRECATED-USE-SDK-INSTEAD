/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.compiler;

import com.google.dart.tools.core.internal.util.CharOperation;

/**
 * Instances of the class <code>Util</code>
 */
public class Util {
  public static final String EMPTY_STRING = new String(CharOperation.NO_CHAR);
  public static final int[] EMPTY_INT_ARRAY = new int[0];
  public static final String LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$
}
