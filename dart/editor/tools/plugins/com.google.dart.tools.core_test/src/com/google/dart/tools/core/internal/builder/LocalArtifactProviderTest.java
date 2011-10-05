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
import com.google.dart.compiler.DartSource;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.model.DartLibrary;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyLibrary;

import junit.framework.TestCase;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.Random;

public class LocalArtifactProviderTest extends TestCase {

  private static final String RANDOM_EXT = "jsx" + new Random().nextInt();
  private static final String RANDOM_EXT2 = "jsx2" + new Random().nextInt();
  private static final String RANDOM_CONTENT = "some-random-text-" + new Random().nextFloat();
  private static final String RANDOM_CONTENT2 = "some-random-text2-" + new Random().nextFloat();

  private static DartSource getSource() throws Exception {
    DartLibrary lib = getMoneyLibrary();
    return ((DartLibraryImpl) lib).getLibrarySourceFile().getSourceFor("currency.dart");
  }

  public void test_ArtifactProvider_getArtifactUri() throws Exception {
    DartArtifactProvider globalProvider = new ArtifactProvider();
    DartArtifactProvider localProvider = new LocalArtifactProvider(globalProvider);

    URI uri = localProvider.getArtifactUri(getSource(), "", RANDOM_EXT);
    assertEquals(RANDOM_EXT, uri.getPath().substring(uri.getPath().lastIndexOf(".") + 1));
  }

  public void test_ArtifactProvider_readNonExistant() throws Exception {
    DartArtifactProvider globalProvider = new ArtifactProvider();
    DartArtifactProvider localProvider = new LocalArtifactProvider(globalProvider);

    Reader reader = localProvider.getArtifactReader(getSource(), "", "doesnotexist");
    assertNull(reader);
  }

  public void test_ArtifactProvider_writeGlobalThenRead() throws Exception {
    DartArtifactProvider globalProvider = new ArtifactProvider();
    DartArtifactProvider localProvider = new LocalArtifactProvider(globalProvider);

    Writer writer = globalProvider.getArtifactWriter(getSource(), "", RANDOM_EXT);
    writer.append(RANDOM_CONTENT);
    writer.close();

    Reader reader = globalProvider.getArtifactReader(getSource(), "", RANDOM_EXT);
    char[] cbuf = new char[1000];
    int len = reader.read(cbuf);
    assertEquals(RANDOM_CONTENT, new String(cbuf, 0, len));

    reader = localProvider.getArtifactReader(getSource(), "", RANDOM_EXT);
    cbuf = new char[1000];
    len = reader.read(cbuf);
    assertEquals(RANDOM_CONTENT, new String(cbuf, 0, len));
  }

  public void test_ArtifactProvider_writeLocalThenRead() throws Exception {
    DartArtifactProvider globalProvider = new ArtifactProvider();
    DartArtifactProvider localProvider = new LocalArtifactProvider(globalProvider);

    Writer writer = localProvider.getArtifactWriter(getSource(), "", RANDOM_EXT2);
    writer.append(RANDOM_CONTENT2);
    writer.close();

    Reader reader = globalProvider.getArtifactReader(getSource(), "", RANDOM_EXT2);
    assertNull(reader);

    reader = localProvider.getArtifactReader(getSource(), "", RANDOM_EXT2);
    char[] cbuf = new char[1000];
    int len = reader.read(cbuf);
    assertEquals(RANDOM_CONTENT2, new String(cbuf, 0, len));
  }
}
