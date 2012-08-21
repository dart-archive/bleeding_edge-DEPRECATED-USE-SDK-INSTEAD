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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.cleanup.CleanUpContext;
import com.google.dart.tools.ui.cleanup.ICleanUp;

import org.eclipse.jface.text.IRegion;

/**
 * A clean up capable of fixing only a subset of lines in a compilation unit
 * 
 * @since 3.4
 */
public interface IMultiLineCleanUp extends ICleanUp {

  public static class MultiLineCleanUpContext extends CleanUpContext {

    private final IRegion[] fRegions;

    public MultiLineCleanUpContext(CompilationUnit unit, DartUnit ast, IRegion[] regions) {
      super(unit, ast);
      fRegions = regions;
    }

    /**
     * The regions of the lines which should be cleaned up. A region spans at least one line but can
     * span multiple line if the lines are successive.
     * 
     * @return the regions or <b>null</b> if none available
     */
    public IRegion[] getRegions() {
      return fRegions;
    }
  }
}
