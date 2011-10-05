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
package com.google.dart.indexer.index.configuration;

import com.google.dart.indexer.index.configuration.internal.ConfigurationError;
import com.google.dart.indexer.index.configuration.internal.ContributorInfo;
import com.google.dart.indexer.index.configuration.internal.IndexConfigurationImpl;
import com.google.dart.indexer.index.configuration.internal.LayerInfo;
import com.google.dart.indexer.index.configuration.internal.ProcessorInfo;
import com.google.dart.indexer.index.configuration.internal.ProcessorRegistration;
import com.google.dart.indexer.index.layers.LayerId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Creates index configurations based on a set of processors and contributors.
 */
public class IndexConfigurationBuilder {
  private static class ProcessorBuilder {
    private final ProcessorInfo info;

    private final List<ContributorInfo> contributors = new ArrayList<ContributorInfo>();

    public ProcessorBuilder(ProcessorInfo info) {
      this.info = info;
    }

    public void add(ContributorInfo contributor) {
      contributors.add(contributor);
    }

    public ProcessorRegistration build() {
      ContributorInfo[] contributorsArr = contributors.toArray(new ContributorInfo[contributors.size()]);
      return new ProcessorRegistration(info, contributorsArr);
    }
  }

  private final Map<String, ProcessorBuilder> idsToProcessorBuilders = new HashMap<String, ProcessorBuilder>();

  private final Map<LayerId, LayerInfo> idsToLayers = new HashMap<LayerId, LayerInfo>();

  public void addContributor(String processorId, ContributorInfo contributor) {
    ProcessorBuilder builder = idsToProcessorBuilders.get(processorId);
    if (builder == null) {
      throw new IllegalArgumentException("Processor with ID {0} not found".replaceAll("\\{0}",
          processorId));
    }
    builder.add(contributor);
  }

  public void addLayer(LayerInfo info) {
    LayerInfo existingLayer = idsToLayers.get(info.getId());
    if (existingLayer != null) {
      throw new IllegalArgumentException("Layer with id " + info.getId()
          + " has already been registered: " + existingLayer);
    }
    idsToLayers.put(info.getId(), info);
  }

  public void addProcessor(ProcessorInfo info) {
    String id = info.getId();
    ProcessorBuilder builder = new ProcessorBuilder(info);
    ProcessorBuilder oldBuilder = idsToProcessorBuilders.put(id, builder);
    if (oldBuilder != null) {
      throw new IllegalArgumentException(
          "Processor with ID {0} has already been registered".replaceAll("\\{0}", id));
    }
  }

  public IndexConfiguration build() throws ConfigurationError {
    return new IndexConfigurationImpl(createRegistrations(), createLayers());
  }

  private LayerInfo[] createLayers() {
    IdentifierStableMappingBuilder builder = new IdentifierStableMappingBuilder();
    List<LayerInfo> layerInfos = new ArrayList<LayerInfo>(idsToLayers.values());
    for (Iterator<LayerInfo> iterator = layerInfos.iterator(); iterator.hasNext();) {
      builder.addUniqueIndentifier(iterator.next().getId().stringValue());
    }
    Map<String, Integer> idsToOrdinals = builder.build();

    for (Iterator<LayerInfo> iterator = layerInfos.iterator(); iterator.hasNext();) {
      LayerInfo info = iterator.next();
      Integer ord = idsToOrdinals.get(info.getId().stringValue());
      info.initializeOrdinal(ord.intValue());
    }
    Collections.sort(layerInfos);
    LayerInfo[] layers = layerInfos.toArray(new LayerInfo[layerInfos.size()]);
    return layers;
  }

  private ProcessorRegistration[] createRegistrations() {
    List<ProcessorRegistration> regs = new ArrayList<ProcessorRegistration>();
    for (Iterator<ProcessorBuilder> iterator = idsToProcessorBuilders.values().iterator(); iterator.hasNext();) {
      regs.add(iterator.next().build());
    }
    return regs.toArray(new ProcessorRegistration[regs.size()]);
  }
}
