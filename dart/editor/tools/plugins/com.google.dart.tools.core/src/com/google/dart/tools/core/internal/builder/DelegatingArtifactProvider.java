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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.Source;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 * This DartArtifactProvider implementation delegates to a provider DartArtifactProvider.
 */
public class DelegatingArtifactProvider extends DartArtifactProvider {
  private DartArtifactProvider provider;

  public DelegatingArtifactProvider(DartArtifactProvider provider) {
    this.provider = provider;
  }

  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    return provider.getArtifactReader(source, part, extension);
  }

  @Override
  public URI getArtifactUri(Source source, String part, String extension) {
    return provider.getArtifactUri(source, part, extension);
  }

  @Override
  public Writer getArtifactWriter(Source source, String part, String extension) throws IOException {
    return provider.getArtifactWriter(source, part, extension);
  }

  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    return provider.isOutOfDate(source, base, extension);
  }
}
