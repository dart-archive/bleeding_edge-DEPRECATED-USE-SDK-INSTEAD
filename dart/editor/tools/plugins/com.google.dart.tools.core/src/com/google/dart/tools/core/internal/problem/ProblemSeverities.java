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
package com.google.dart.tools.core.internal.problem;

/**
 * The interface <code>ProblemSeverities</code> defines constants that can be combined to represent
 * the severity of a compilation problem.
 */
public interface ProblemSeverities {
  final int Ignore = 256; // during handling only
  final int Warning = 512; // during handling only
  final int Info = 1024; // during handling only

  final int Error = 1; // when bit is set: problem is error, if not it is a warning
  final int AbortCompilation = 2;
  final int AbortCompilationUnit = 4;
  final int AbortType = 8;
  final int AbortMethod = 16;
  final int Abort = 30; // 2r11110
  final int Optional = 32; // when bit is set: problem was configurable
  final int SecondaryError = 64;
  final int Fatal = 128; // when bit is set: problem was either a mandatory error, or an optional+treatOptionalErrorAsFatal
}
