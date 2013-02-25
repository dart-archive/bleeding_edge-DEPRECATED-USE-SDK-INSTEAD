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

import com.google.dart.indexer.index.configuration.Processor;

import java.util.Set;

public abstract class ProcessorInfo {
  private final String id;
  private final String version;
  private final String[] extensions;
  private final String[] usedProcessorIds;

  public ProcessorInfo(String id, String version, String contributorClass, String[] extensions,
      String[] usedProcessorIds) throws ConfigurationError {
    if (id == null) {
      throw new NullPointerException("id is null");
    }
    if (version == null) {
      throw new NullPointerException("version is null");
    }
    if (extensions == null) {
      throw new NullPointerException("extensions is null");
    }
    if (usedProcessorIds == null) {
      throw new NullPointerException("usedProcessorIds is null");
    }
    this.id = id;
    this.version = version;
    this.extensions = extensions;
    this.usedProcessorIds = usedProcessorIds;
    for (int i = 0; i < usedProcessorIds.length; i++) {
      if (usedProcessorIds[i].equals(id)) {
        throw new ConfigurationError(
            "Processor "
                + id
                + " tries to require itself, creating an endless entrophy loop and thus killing the Universe");
      }
    }
  }

  public abstract Processor create();

  public void describe(StringBuilder builder) {
    builder.append(id).append(' ').append(version);
    for (int i = 0; i < extensions.length; i++) {
      builder.append(" .").append(extensions[i]);
    }
  }

  public String[] getExtensions() {
    return extensions;
  }

  public String getId() {
    return id;
  }

  public String[] getUsedProcessorIds() {
    return usedProcessorIds;
  }

  public String getVersion() {
    return version;
  }

  public boolean mightBeInterestedIn(String extension) {
    for (int i = 0; i < extensions.length; i++) {
      if (extensions[i].equalsIgnoreCase(extension)) {
        return true;
      }
    }
    return false;
  }

  public void verify(Set<String> allProcessorIds) throws ConfigurationError {
    for (int i = 0; i < usedProcessorIds.length; i++) {
      if (!allProcessorIds.contains(usedProcessorIds[i])) {
        throw new ConfigurationError("Processor " + id + " require processor "
            + usedProcessorIds[i] + " which has not been registered.");
      }
    }
  }
}
