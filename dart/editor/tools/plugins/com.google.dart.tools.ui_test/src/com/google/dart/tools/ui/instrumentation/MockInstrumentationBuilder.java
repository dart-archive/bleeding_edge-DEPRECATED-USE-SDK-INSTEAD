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
package com.google.dart.tools.ui.instrumentation;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.instrumentation.InstrumentationLevel;

import java.util.HashMap;
import java.util.TreeSet;

public class MockInstrumentationBuilder implements InstrumentationBuilder {
  private final HashMap<String, String> data = new HashMap<String, String>();
  private final HashMap<String, String> metric = new HashMap<String, String>();

  @Override
  public InstrumentationBuilder data(String name, boolean value) {
    data.put(name, Boolean.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder data(String name, long value) {
    data.put(name, Long.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder data(String name, String value) {
    data.put(name, value);
    return this;
  }

  @Override
  public InstrumentationBuilder data(String name, String[] value) {
    for (int i = 0; i < value.length; i++) {
      data.put(name + "-" + String.valueOf(i), value[i]);
    }
    return this;
  }

  public void echoToStdOut(String name) {
    System.out.println("Instrumentation for " + name);
    System.out.println("  Metrics:");
    for (String key : new TreeSet<String>(metric.keySet())) {
      System.out.println("    " + key + " = " + metric.get(key));
    }
    System.out.println("  Data:");
    for (String key : new TreeSet<String>(data.keySet())) {
      System.out.println("    " + key + " = " + data.get(key));
    }
  }

  /**
   * Answer the "data" value associated with the specified key
   * 
   * @param key the key (not {@code null})
   * @return the value or {@code null} if none
   */
  public String getData(String key) {
    return data.get(key);
  }

  @Override
  public InstrumentationLevel getInstrumentationLevel() {
    return InstrumentationLevel.EVERYTHING;
  }

  /**
   * Answer the "metrics" value associated with the specified key
   * 
   * @param key the key (not {@code null})
   * @return the value or {@code null} if none
   */
  public String getMetric(String key) {
    return metric.get(key);
  }

  @Override
  public void log() {
    // ignored
  }

  @Override
  public InstrumentationBuilder metric(String name, boolean value) {
    metric.put(name, Boolean.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder metric(String name, long value) {
    metric.put(name, Long.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder metric(String name, String value) {
    metric.put(name, value);
    return this;
  }

  @Override
  public InstrumentationBuilder metric(String name, String[] value) {
    for (int i = 0; i < value.length; i++) {
      metric.put(name + "-" + String.valueOf(i), value[i]);
    }
    return this;
  }

  @Override
  public InstrumentationBuilder record(Throwable exception) {
    metric.put("Exception", exception.getClass().getName());
    return this;
  }
}
