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
package com.google.dart.engine.metrics;

import com.google.dart.engine.metrics.Tracer.EventType;

/**
 * Dart events for SpeedTracer.
 */
public enum DartEventType implements EventType {
  ADD_OUTOFDATE("MistyRose"),
  BUILD_LIB_SCOPES("violet"),
  ANALYZE("green"), //
  ANALYZE_TOP_LEVEL_LIBRARY("gray"),
  ANALYZE_IMPORTS("brown"),
  EXEC_PHASE("blue"), //
  IMPORT_EMBEDDED_LIBRARIES("purple"),
  IS_SOURCE_OUTOFDATE("Chartreuse"), //
  SCANNER("GoldenRod"),
  PARSE("red"),
  PARSE_OUTOFDATE("LightCoral"), //
  RESOLVE_LIBRARIES("black"),
  TIMESTAMP_OUTOFDATE("LightSteelBlue"), //
  UPDATE_LIBRARIES("yellow"),
  UPDATE_RESOLVE("orange"),
  WRITE_METRICS("LightChiffon");

  final String cssColor;
  final String name;

  DartEventType(String cssColor) {
    this(null, cssColor);
  }

  DartEventType(String name, String cssColor) {
    this.name = name;
    this.cssColor = cssColor;
  }

  @Override
  public String getColor() {
    return cssColor;
  }

  @Override
  public String getName() {
    return name == null ? toString() : name;
  }
}
