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
package com.google.dart.tools.core.internal.model.info;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.internal.problem.CategorizedProblem;

import java.util.HashMap;

/**
 * Instances of the class <code>ASTHolderCUInfo</code> are used during a reconcile operation to hold
 * additional information not normally cached for a compilation unit.
 */
public class ASTHolderCUInfo extends CompilationUnitInfo {
  public boolean resolveBindings;
  public boolean forceProblemDetection;
  public HashMap<String, CategorizedProblem[]> problems = null;
  public DartUnit ast;
}
