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

public class RootArtifactProviderTest extends TestCase {

  private static final String RANDOM_EXT = "jsx" + new Random().nextInt();
  private static final String RANDOM_CONTENT = "some-random-text-" + new Random().nextFloat();

  private static DartSource getSource() throws Exception {
    DartLibrary lib = getMoneyLibrary();
    return ((DartLibraryImpl) lib).getLibrarySourceFile().getSourceFor("currency.dart");
  }

  public void test_RootArtifactProvider_getArtifactUri() throws Exception {
    DartArtifactProvider provider = RootArtifactProvider.newInstanceForTesting();
    URI uri = provider.getArtifactUri(getSource(), "", RANDOM_EXT);
    assertEquals(RANDOM_EXT, uri.getPath().substring(uri.getPath().lastIndexOf(".") + 1));
  }

  public void test_RootArtifactProvider_readNonExistant() throws Exception {
    DartArtifactProvider provider = RootArtifactProvider.newInstanceForTesting();
    Reader reader = provider.getArtifactReader(getSource(), "", "doesnotexist");
    assertNull(reader);
  }

  public void test_RootArtifactProvider_writeThenRead() throws Exception {
    DartArtifactProvider provider = RootArtifactProvider.newInstanceForTesting();
    Writer writer = provider.getArtifactWriter(getSource(), "", RANDOM_EXT);
    writer.append(RANDOM_CONTENT);
    writer.close();

    Reader reader = provider.getArtifactReader(getSource(), "", RANDOM_EXT);
    char[] cbuf = new char[1000];
    int len = reader.read(cbuf);
    assertEquals(RANDOM_CONTENT, new String(cbuf, 0, len));
  }
}
