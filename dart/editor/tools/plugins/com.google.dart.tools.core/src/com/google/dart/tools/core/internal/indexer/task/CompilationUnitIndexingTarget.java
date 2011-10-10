/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.indexer.task;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.indexer.workspace.index.IndexingTarget;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 * Instances of the class <code>CompilationUnitIndexingTarget</code> implement an indexing target
 * representing a compilation unit.
 */
public class CompilationUnitIndexingTarget implements IndexingTarget {
  /**
   * The compilation unit to be indexed.
   */
  private CompilationUnit compilationUnit;

  /**
   * The AST structure associated with the compilation unit being indexed.
   */
  private DartUnit ast;

  /**
   * Initialize a newly created target representing the given compilation unit.
   * 
   * @param compilationUnit the compilation unit to be indexed
   * @param ast the AST structure representing the contents of the compilation unit
   */
  public CompilationUnitIndexingTarget(CompilationUnit compilationUnit, DartUnit ast) {
    this.compilationUnit = compilationUnit;
    this.ast = ast;
  }

  public DartUnit getAST() {
    return ast;
  }

  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  @Override
  public IFile getFile() {
    try {
      return (IFile) compilationUnit.getCorrespondingResource();
    } catch (DartModelException exception) {
      return null;
    }
  }

  @Override
  public IProject getProject() {
    return compilationUnit.getDartProject().getProject();
  }
}
