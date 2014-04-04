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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.source.Source;

/**
 * The interface {@code HtmlEntry} defines the behavior of objects that maintain the information
 * cached by an analysis context about an individual HTML file.
 * 
 * @coverage dart.engine
 */
public interface HtmlEntry extends SourceEntry {
  /**
   * The data descriptor representing the information about an Angular application this source is
   * used in.
   */
  public static final DataDescriptor<AngularApplication> ANGULAR_APPLICATION = new DataDescriptor<AngularApplication>(
      "HtmlEntry.ANGULAR_APPLICATION");

  /**
   * The data descriptor representing the information about an Angular component this source is used
   * as template for.
   */
  public static final DataDescriptor<AngularComponentElement> ANGULAR_COMPONENT = new DataDescriptor<AngularComponentElement>(
      "HtmlEntry.ANGULAR_COMPONENT");

  /**
   * The data descriptor representing the information about an Angular application this source is
   * entry point for.
   */
  public static final DataDescriptor<AngularApplication> ANGULAR_ENTRY = new DataDescriptor<AngularApplication>(
      "HtmlEntry.ANGULAR_ENTRY");

  /**
   * The data descriptor representing the errors reported during Angular resolution.
   */
  public static final DataDescriptor<AnalysisError[]> ANGULAR_ERRORS = new DataDescriptor<AnalysisError[]>(
      "HtmlEntry.ANGULAR_ERRORS");

  /**
   * The data descriptor representing the HTML element.
   */
  public static final DataDescriptor<HtmlElement> ELEMENT = new DataDescriptor<HtmlElement>(
      "HtmlEntry.ELEMENT");

  /**
   * The data descriptor representing the hints resulting from auditing the source.
   */
  public static final DataDescriptor<AnalysisError[]> HINTS = new DataDescriptor<AnalysisError[]>(
      "HtmlEntry.HINTS");

  /**
   * The data descriptor representing the errors resulting from parsing the source.
   */
  public static final DataDescriptor<AnalysisError[]> PARSE_ERRORS = new DataDescriptor<AnalysisError[]>(
      "HtmlEntry.PARSE_ERRORS");

  /**
   * The data descriptor representing the parsed AST structure.
   */
  public static final DataDescriptor<HtmlUnit> PARSED_UNIT = new DataDescriptor<HtmlUnit>(
      "HtmlEntry.PARSED_UNIT");

  /**
   * The data descriptor representing the resolved AST structure.
   */
  public static final DataDescriptor<HtmlUnit> RESOLVED_UNIT = new DataDescriptor<HtmlUnit>(
      "HtmlEntry.RESOLVED_UNIT");

  /**
   * The data descriptor representing the list of referenced libraries.
   */
  public static final DataDescriptor<Source[]> REFERENCED_LIBRARIES = new DataDescriptor<Source[]>(
      "HtmlEntry.REFERENCED_LIBRARIES");

  /**
   * The data descriptor representing the errors resulting from resolving the source.
   */
  public static final DataDescriptor<AnalysisError[]> RESOLUTION_ERRORS = new DataDescriptor<AnalysisError[]>(
      "HtmlEntry.RESOLUTION_ERRORS");

  /**
   * The data descriptor representing the status of Polymer elements in the source.
   */
  public static final DataDescriptor<AnalysisError[]> POLYMER_BUILD_ERRORS = new DataDescriptor<AnalysisError[]>(
      "HtmlEntry.POLYMER_BUILD_ERRORS");

  /**
   * The data descriptor representing the errors reported during Polymer resolution.
   */
  public static final DataDescriptor<AnalysisError[]> POLYMER_RESOLUTION_ERRORS = new DataDescriptor<AnalysisError[]>(
      "HtmlEntry.POLYMER_RESOLUTION_ERRORS");

  /**
   * Return all of the errors associated with the compilation unit that are currently cached.
   * 
   * @return all of the errors associated with the compilation unit
   */
  public AnalysisError[] getAllErrors();

  /**
   * Return a valid parsed unit, either an unresolved AST structure or the result of resolving the
   * AST structure, or {@code null} if there is no parsed unit available.
   * 
   * @return a valid parsed unit
   */
  public HtmlUnit getAnyParsedUnit();

  @Override
  public HtmlEntryImpl getWritableCopy();
}
