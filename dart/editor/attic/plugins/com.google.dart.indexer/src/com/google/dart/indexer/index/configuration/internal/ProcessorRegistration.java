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

import com.google.dart.indexer.index.configuration.ContributorWrapper;
import com.google.dart.indexer.index.configuration.Processor;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessorRegistration implements Comparable<ProcessorRegistration> {
  private static ContributorInfo[] sort(ContributorInfo[] contributors) {
    ContributorInfo[] res = new ContributorInfo[contributors.length];
    System.arraycopy(contributors, 0, res, 0, contributors.length);
    Arrays.sort(res);
    return res;
  }

  private final ProcessorInfo processorInfo;

  private final ContributorInfo[] contributors;

  public ProcessorRegistration(ProcessorInfo processorInfo, ContributorInfo[] contributors) {
    this.processorInfo = processorInfo;
    this.contributors = sort(contributors);
  }

  @Override
  public int compareTo(ProcessorRegistration o) {
    ProcessorRegistration peer = o;
    return getId().compareTo(peer.getId());
  }

  public Processor createAndInitializeProcessor(Map<LayerId, Layer> idsToLayers,
      ProcessorFactory factory) {
    Processor processor = processorInfo.create();
    String[] usedIds = processorInfo.getUsedProcessorIds();
    Map<String, Processor> idsToProcessors = new HashMap<String, Processor>();
    for (int i = 0; i < usedIds.length; i++) {
      String id = usedIds[i];
      idsToProcessors.put(id, factory.getInitializedProcessor(id));
    }
    processor.initialize(createContributors(idsToLayers), idsToProcessors);
    return processor;
  }

  public void describe(StringBuilder builder) {
    processorInfo.describe(builder);
    for (int i = 0; i < contributors.length; i++) {
      builder.append("\n- ");
      contributors[i].describe(builder);
    }
  }

  public String getId() {
    return processorInfo.getId();
  }

  public boolean idEquals(String id) {
    return getId().equals(id);
  }

  public boolean mightBeInterestedIn(String extension) {
    return processorInfo.mightBeInterestedIn(extension);
  }

  public void verifyRegistration(Set<String> allProcessorIds) throws ConfigurationError {
    processorInfo.verify(allProcessorIds);
  }

  private ContributorWrapper[] createContributors(Map<LayerId, Layer> idsToLayers) {
    List<ContributorWrapper> contrs = new ArrayList<ContributorWrapper>();
    for (int i = 0; i < contributors.length; i++) {
      LayerId layerId = contributors[i].getLayerId();
      Layer layer = idsToLayers.get(layerId);
      if (layer == null) {
        throw new IllegalArgumentException("Layer with id " + layerId + " not found.");
      }
      contrs.add(new ContributorWrapper(contributors[i].create(), layer));
    }
    return contrs.toArray(new ContributorWrapper[contrs.size()]);
  }
}
