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
import com.google.dart.indexer.index.configuration.Processor;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;
import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class IndexConfigurationInstanceImpl implements IndexConfigurationInstance, ProcessorFactory {
  private static Map<LayerId, Layer> indexLayersById(Layer[] layers) {
    Map<LayerId, Layer> idsToLayers = new HashMap<LayerId, Layer>();
    for (int i = 0; i < layers.length; i++) {
      idsToLayers.put(layers[i].getId(), layers[i]);
    }
    return idsToLayers;
  }

  private final ProcessorRegistration[] registrations;

  private final Map<String, Processor[]> extensionsToRegistrations = new HashMap<String, Processor[]>();

  private final Map<ProcessorRegistration, Processor> registrationsToProcessors = new HashMap<ProcessorRegistration, Processor>();

  private final Layer[] layers;

  private final IndexConfiguration configuration;

  private Map<LayerId, Layer> idsToLayers;

  public IndexConfigurationInstanceImpl(IndexConfiguration configuration,
      ProcessorRegistration registrations[], Layer[] layers) {
    if (configuration == null) {
      throw new NullPointerException("configuration is null");
    }
    if (registrations == null) {
      throw new NullPointerException("registrations is null");
    }
    if (layers == null) {
      throw new NullPointerException("layers is null");
    }
    this.configuration = configuration;
    this.registrations = registrations;
    this.layers = layers;
    this.idsToLayers = indexLayersById(layers);
  }

  @Override
  public String describe() {
    return configuration.describe();
  }

  @Override
  @Deprecated
  public Processor[] findProcessors(IFile file) {
    String extension = file.getFileExtension();
    Processor[] processors = extensionsToRegistrations.get(extension);
    if (processors == null) {
      processors = calculateProcessors(extension);
      extensionsToRegistrations.put(extension, processors);
    }
    return processors;
  }

  @Override
  public Processor[] findProcessors(IndexableSource source) {
    String extension = source.getFileExtension();
    Processor[] processors = extensionsToRegistrations.get(extension);
    if (processors == null) {
      processors = calculateProcessors(extension);
      extensionsToRegistrations.put(extension, processors);
    }
    return processors;
  }

  @Override
  public long gatherTimeSpentParsing() {
    long result = 0;
    for (Iterator<Processor> iterator = registrationsToProcessors.values().iterator(); iterator.hasNext();) {
      Processor processor = iterator.next();
      result += processor.getAndResetTimeSpentParsing();
    }
    return result;
  }

  @Override
  public Processor getInitializedProcessor(String id) {
    return getInitializedProcessor(findRegistration(id));
  }

  @Override
  public Processor[] getKnownProcessors() {
    HashSet<Processor> knownProcessors = new HashSet<Processor>();
    for (Processor[] processors : extensionsToRegistrations.values()) {
      for (Processor processor : processors) {
        knownProcessors.add(processor);
      }
    }
    return knownProcessors.toArray(new Processor[knownProcessors.size()]);
  }

  @Override
  public Layer getLayer(int ordinal) {
    return layers[ordinal];
  }

  @Override
  public Layer getLayer(LayerId layerId) {
    return idsToLayers.get(layerId);
  }

  @Override
  public Layer[] getLayers() {
    return layers;
  }

  @Override
  @Deprecated
  public boolean isIndexedFile(IFile file) {
    return findProcessors(file).length > 0;
  }

  @Override
  public boolean isIndexedFile(IndexableSource source) {
    return findProcessors(source).length > 0;
  }

  private Processor[] calculateProcessors(String extension) {
    Processor[] processors;
    ProcessorRegistration[] regs = findRegistrations(extension);
    processors = new Processor[regs.length];
    for (int i = 0; i < regs.length; i++) {
      processors[i] = getInitializedProcessor(regs[i]);
    }
    return processors;
  }

  private ProcessorRegistration findRegistration(String id) {
    for (int i = 0; i < registrations.length; i++) {
      if (registrations[i].idEquals(id)) {
        return registrations[i];
      }
    }
    throw new AssertionError("Processor with id " + id + " not found");
  }

  private ProcessorRegistration[] findRegistrations(String extension) {
    List<ProcessorRegistration> rr = new ArrayList<ProcessorRegistration>();
    for (int i = 0; i < registrations.length; i++) {
      if (registrations[i].mightBeInterestedIn(extension)) {
        rr.add(registrations[i]);
      }
    }
    return rr.toArray(new ProcessorRegistration[rr.size()]);
  }

  private Processor getInitializedProcessor(ProcessorRegistration registration) {
    Processor processor = registrationsToProcessors.get(registration);
    if (processor == null) {
      processor = registration.createAndInitializeProcessor(idsToLayers, this);
      registrationsToProcessors.put(registration, processor);
    }
    return processor;
  }
}
