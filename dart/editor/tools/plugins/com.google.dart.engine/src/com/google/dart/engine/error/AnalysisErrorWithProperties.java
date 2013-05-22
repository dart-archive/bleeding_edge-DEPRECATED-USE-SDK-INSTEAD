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
package com.google.dart.engine.error;

import com.google.dart.engine.source.Source;

import java.util.HashMap;

/**
 * Instances of the class {@code AnalysisErrorWithProperties}
 */
public class AnalysisErrorWithProperties extends AnalysisError {
  /**
   * The properties associated with this error.
   */
  private HashMap<ErrorProperty, Object> propertyMap = new HashMap<ErrorProperty, Object>();

  /**
   * Initialize a newly created analysis error for the specified source. The error has no location
   * information.
   * 
   * @param source the source for which the exception occurred
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  public AnalysisErrorWithProperties(Source source, ErrorCode errorCode, Object... arguments) {
    super(source, errorCode, arguments);
  }

  /**
   * Initialize a newly created analysis error for the specified source at the given location.
   * 
   * @param source the source for which the exception occurred
   * @param offset the offset of the location of the error
   * @param length the length of the location of the error
   * @param errorCode the error code to be associated with this error
   * @param arguments the arguments used to build the error message
   */
  public AnalysisErrorWithProperties(Source source, int offset, int length, ErrorCode errorCode,
      Object... arguments) {
    super(source, offset, length, errorCode, arguments);
  }

  @Override
  public Object getProperty(ErrorProperty property) {
    return propertyMap.get(property);
  }

  /**
   * Set the value of the given property to the given value. Using a value of {@code null} will
   * effectively remove the property from this error.
   * 
   * @param property the property whose value is to be returned
   * @param value the new value of the given property
   */
  public void setProperty(ErrorProperty property, Object value) {
    propertyMap.put(property, value);
  }
}
