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
package com.google.dart.indexer.index.configuration.internal;

import com.google.dart.indexer.index.configuration.IndexConfiguration;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.layers.Layer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IndexConfigurationImpl implements IndexConfiguration {
  private static ProcessorRegistration[] sort(ProcessorRegistration[] registrations) {
    ProcessorRegistration[] res = new ProcessorRegistration[registrations.length];
    System.arraycopy(registrations, 0, res, 0, registrations.length);
    Arrays.sort(res);
    return res;
  }

  private final ProcessorRegistration[] registrations;

  private final LayerInfo[] layers;

  private String description;

  public IndexConfigurationImpl(ProcessorRegistration registrations[], LayerInfo[] layers)
      throws ConfigurationError {
    this.layers = layers;
    this.registrations = sort(registrations);
    verifyAllRegistrations(calculateAllProcessorIds(registrations));
    description = calculateDescription();
  }

  public Layer[] createLayers() {
    Layer[] result = new Layer[layers.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = layers[i].create();
      result[i].initialize(layers[i]);
    }
    return result;
  }

  @Override
  public String describe() {
    return description;
  }

  @Override
  public IndexConfigurationInstance instantiate() {
    return new IndexConfigurationInstanceImpl(this, registrations, createLayers());
  }

  private Set<String> calculateAllProcessorIds(ProcessorRegistration[] registrations) {
    Set<String> allProcessorIds = new HashSet<String>();
    for (int i = 0; i < registrations.length; i++) {
      allProcessorIds.add(registrations[i].getId());
    }
    return allProcessorIds;
  }

  private String calculateDescription() {
    StringBuilder result = new StringBuilder();
    result.append("PROCESSORS:").append('\n');
    for (int i = 0; i < registrations.length; i++) {
      registrations[i].describe(result);
    }
    result.append("\nLAYERS:");
    for (int i = 0; i < layers.length; i++) {
      result.append("\n- ").append(layers[i].getId());
    }
    result.append('\n');
    return result.toString();
  }

  private void verifyAllRegistrations(Set<String> allProcessorIds) throws ConfigurationError {
    for (int i = 0; i < registrations.length; i++) {
      registrations[i].verifyRegistration(allProcessorIds);
    }
  }
}
