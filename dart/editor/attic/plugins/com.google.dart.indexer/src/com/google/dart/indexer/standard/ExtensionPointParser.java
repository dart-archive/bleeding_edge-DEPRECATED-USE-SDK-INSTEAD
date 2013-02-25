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
package com.google.dart.indexer.standard;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.index.configuration.Contributor;
import com.google.dart.indexer.index.configuration.IndexConfiguration;
import com.google.dart.indexer.index.configuration.IndexConfigurationBuilder;
import com.google.dart.indexer.index.configuration.Processor;
import com.google.dart.indexer.index.configuration.internal.ConfigurationError;
import com.google.dart.indexer.index.configuration.internal.ContributorInfo;
import com.google.dart.indexer.index.configuration.internal.LayerInfo;
import com.google.dart.indexer.index.configuration.internal.ProcessorInfo;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.Collection;

public class ExtensionPointParser {
  private static String getPluginId(IExtension extension) {
    return extension.getContributor().getName();
  }

  private static String nonrequired(IConfigurationElement element, String attr) {
    String value = element.getAttribute(attr);
    return value == null ? "" : value.trim();
  }

  private static String required(IConfigurationElement element, String attr)
      throws ConfigurationError {
    String value = element.getAttribute(attr);
    if (value == null || (value = value.trim()).length() == 0) {
      throw new ConfigurationError("Element {0} must have a non-empty attribute {1}".replaceAll(
          "\\{0}", element.getName()).replaceAll("\\{1}", attr));
    }
    return value;
  }

  private final IndexConfigurationBuilder builder;

  public ExtensionPointParser() {
    this.builder = new IndexConfigurationBuilder();
  }

  public IndexConfiguration parse() {
    IExtensionRegistry er = Platform.getExtensionRegistry();
    IExtensionPoint ep = er.getExtensionPoint(IndexerPlugin.PLUGIN_ID + ".indexerExtensions");
    IExtension[] extensions = ep.getExtensions();
    parseProcessorsAndIdentifiers(extensions);
    parseContributors(extensions);
    try {
      return builder.build();
    } catch (ConfigurationError e) {
      String message = "Indexer contribution is invalid: {0}".replaceAll("\\{0}", e.getMessage());
      IndexerPlugin.getLogger().logError(e, message);
      throw new AssertionError(message);
    }
  }

  private void parseContributorElement(final IConfigurationElement element)
      throws InvalidRegistryObjectException, ConfigurationError {
    String id = required(element, "id");
    String version = required(element, "version");
    String processorId = required(element, "processorId");
    String layerId = required(element, "layerId");
    required(element, "class");
    ContributorInfo info = new ContributorInfo(id, version, new LayerId(layerId)) {

      @Override
      public Contributor create() {
        try {
          return (Contributor) element.createExecutableExtension("class");
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
      }

    };
    builder.addContributor(processorId, info);
  }

  private void parseContributors(IConfigurationElement[] elements) throws ConfigurationError {
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if ("contributor".equals(element.getName())) {
        parseContributorElement(element);
      }
    }
  }

  private void parseContributors(IExtension[] extensions) throws AssertionError {
    for (int i = 0; i < extensions.length; i++) {
      IExtension extension = extensions[i];
      try {
        parseContributors(extension.getConfigurationElements());
      } catch (InvalidRegistryObjectException e) {
      } catch (ConfigurationError e) {
        String pluginId = getPluginId(extension);
        String message = "Indexer contribution by plugin {0} is invalid: {1}".replaceAll("\\{0}",
            pluginId).replaceAll("\\{1}", e.getMessage());
        IndexerPlugin.getLogger().logError(e, message);
        throw new AssertionError(message);
      }
    }
  }

  private void parseLayerElement(final IConfigurationElement element)
      throws InvalidRegistryObjectException, ConfigurationError {
    String id = required(element, "id");
    required(element, "class");
    builder.addLayer(new LayerInfo(new LayerId(id)) {

      @Override
      public Layer create() {
        try {
          return (Layer) element.createExecutableExtension("class");
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
      }

    });
  }

  private void parseProcessorChildren(IConfigurationElement[] elements,
      Collection<String> usedProcessorIds) throws ConfigurationError {
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if ("uses".equals(element.getName())) {
        parseUsedProcessor(element, usedProcessorIds);
      }
    }
  }

  private void parseProcessorElement(final IConfigurationElement element)
      throws InvalidRegistryObjectException, ConfigurationError {
    String id = required(element, "id");
    String version = required(element, "version");
    String extensions = nonrequired(element, "extensions");
    required(element, "class");
    String contributorType = nonrequired(element, "contributorType");
    Collection<String> usedProcessorIds = new ArrayList<String>();
    parseProcessorChildren(element.getChildren(), usedProcessorIds);
    String[] usedProcessorIdsArr = usedProcessorIds.toArray(new String[usedProcessorIds.size()]);
    String[] extentionsArr = extensions.split("\\s*,\\s*");
    ProcessorInfo info = new ProcessorInfo(id, version, contributorType, extentionsArr,
        usedProcessorIdsArr) {

      @Override
      public Processor create() {
        try {
          return (Processor) element.createExecutableExtension("class");
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
      }

    };
    builder.addProcessor(info);
  }

  private void parseProcessorsAndIdentifiers(IConfigurationElement[] elements)
      throws ConfigurationError {
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if ("processor".equals(element.getName())) {
        parseProcessorElement(element);
      } else if ("layer".equals(element.getName())) {
        parseLayerElement(element);
      }
    }
  }

  private void parseProcessorsAndIdentifiers(IExtension[] extensions) throws AssertionError {
    for (int i = 0; i < extensions.length; i++) {
      IExtension extension = extensions[i];
      try {
        parseProcessorsAndIdentifiers(extension.getConfigurationElements());
      } catch (InvalidRegistryObjectException e) {
      } catch (ConfigurationError e) {
        String pluginId = getPluginId(extension);
        String message = "Indexer contribution by plugin {0} is invalid: {1}".replaceAll("\\{0}",
            pluginId).replaceAll("\\{1}", e.getMessage());
        IndexerPlugin.getLogger().logError(e, message);
        throw new AssertionError(message);
      }
    }
  }

  private void parseUsedProcessor(IConfigurationElement element, Collection<String> usedProcessorIds)
      throws InvalidRegistryObjectException, ConfigurationError {
    String id = required(element, "processor-id");
    usedProcessorIds.add(id);
  }
}
