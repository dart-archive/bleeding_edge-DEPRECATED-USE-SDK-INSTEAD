/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.tools.debug.core.source;

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

import java.io.File;
import java.net.URI;

/**
 * A source container that resolves "dart:" URIs.
 */
public class DartSdkSourceContainer extends AbstractSourceContainer {
  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.dartSdk"; //$NON-NLS-1$

  private static final Object[] EMPTY_COLLECTION = new Object[0];

  public DartSdkSourceContainer() {
  }

  @Override
  public Object[] findSourceElements(String path) throws CoreException {
    DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
    try {
      URI uri = URI.create(path);
      Source source = new DartUriResolver(sdk).resolveAbsolute(uri);
      if (source instanceof FileBasedSource) {
        String filePath = source.getFullName();
        File file = new File(filePath);
        if (file.isFile()) {
          return new Object[] {new LocalFileStorage(file)};
        }
      }
    } catch (Throwable e) {
    }
    return EMPTY_COLLECTION;
  }

  @Override
  public String getName() {
    return "Dart SDK";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }
}
