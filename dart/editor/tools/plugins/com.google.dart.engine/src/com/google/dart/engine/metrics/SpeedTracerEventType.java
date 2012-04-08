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
 * Represents a type of event whose performance is tracked while running.
 */
public enum SpeedTracerEventType implements EventType {
  GC("Garbage Collection", "Plum"),
  OVERHEAD("Speedtracer Overhead", "Black");

  final String cssColor;
  final String name;

  SpeedTracerEventType(String name, String cssColor) {
    this.name = name;
    this.cssColor = cssColor;
  }

  @Override
  public String getColor() {
    return cssColor;
  }

  @Override
  public String getName() {
    return name;
  }
}
