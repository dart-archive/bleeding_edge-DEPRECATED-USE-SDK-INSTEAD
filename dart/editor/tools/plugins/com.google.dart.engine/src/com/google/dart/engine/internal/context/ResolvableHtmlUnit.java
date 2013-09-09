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

import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.task.ResolveHtmlTask;

/**
 * Instances of the class {@code ResolvableHtmlUnit} represent an HTML unit that is not referenced
 * by any other objects and for which we have modification stamp information. It is used by the
 * {@link ResolveHtmlTask} to resolve an HTML source.
 */
public class ResolvableHtmlUnit extends TimestampedData<HtmlUnit> {
  /**
   * Initialize a newly created holder to hold the given values.
   * 
   * @param modificationTime the modification time of the source from which the AST was created
   * @param unit the AST that was created from the source
   */
  public ResolvableHtmlUnit(long modificationTime, HtmlUnit unit) {
    super(modificationTime, unit);
  }

  /**
   * Return the AST that was created from the source.
   * 
   * @return the AST that was created from the source
   */
  public HtmlUnit getCompilationUnit() {
    return getData();
  }
}
