/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.delta;

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.UrlDartSource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyLibrary;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class DeltaProcessorTest extends TestCase {

  private static final String FILE_URI_SCHEME = "file";

  public void test_DeltaProcessor_getCachedDirectives_fileVersion() throws Exception {
    // Get and assert that the cached directives for the Money library is correct when using the
    // file-version of the "getCachedDirectives" method in the DeltaProcessor.
    DartLibrary moneyLibrary = getMoneyLibrary();
    CachedDirectives moneyCachedDirectives = getCachedDirectives(
        computeDartSourceFromDartLibrary(moneyLibrary),
        (DartLibraryImpl) moneyLibrary);

    assertValidMoneyLibraryCachedDirectives(moneyCachedDirectives);
  }

  public void test_DeltaProcessor_getCachedDirectives_modelVersion() throws Exception {
    // Get and assert that the cached directives for the Money library is correct when using the
    // model-version of the "getCachedDirectives" method in the DeltaProcessor.
    DartLibrary moneyLibrary = getMoneyLibrary();
    CachedDirectives moneyCachedDirectives = getCachedDirectives(moneyLibrary);

    assertValidMoneyLibraryCachedDirectives(moneyCachedDirectives);
  }

  /**
   * When making assertions about some absolute file URI being in a {@link CachedDirectives} set,
   * the path to the file will have the user id in in (
   * <code>such as /Users/&lt;user&gt;/directory/file.txt</code>), thus we make assertions about the
   * suffix of the path, i.e. <code>/file.txt</code>, in the passed set of Strings.
   */
  private void assertSetContainsStringWithSuffix(Set<String> strSet, String suffix) {
    for (String string : strSet) {
      if (string.endsWith(suffix)) {
        return;
      }
    }
    fail("The suffix \"" + suffix + "\" was not found in the set: " + strSet.toString());
  }

  /**
   * This method validates that the passed {@link CachedDirectives} object was constructed from the
   * Money example.
   */
  private void assertValidMoneyLibraryCachedDirectives(CachedDirectives cachedDirectives) {
    assertNotNull(cachedDirectives);

    // Note: the sources directive is a set of strings of the form {"file:/User/..../money.dart",
    // "file:/User/..../complex_money.dart", "file:/User/..../currency_exchange.dart",
    // "file:/User/..../currency.dart", "file:/User/..../simple_money.dart"}

    // Also note, the name of the library should be "Money", and the imports and resources sets
    // should be empty.

    // Make assertions concerning the library name and the size of each of the directive sets.
    Set<CachedLibraryImport> imports = cachedDirectives.getImports();
    Set<String> resources = cachedDirectives.getResources();
    Set<String> sources = cachedDirectives.getSources();

    assertEquals("Money", cachedDirectives.getLibraryName());
    assertEquals(0, imports.size());
    assertEquals(0, resources.size());
    assertEquals(5, sources.size());

    // Assert that each of the source strings is an absolute file URI
    for (String string : sources) {
      try {
        URI uri = new URI(string);
        Assert.isTrue(uri.isAbsolute());
        assertEquals(uri.getScheme(), FILE_URI_SCHEME);
      } catch (URISyntaxException e) {
        fail(e.getMessage());
      }
    }

    // And finally assert the contents of the sources set
    assertSetContainsStringWithSuffix(sources, "/money.dart");
    assertSetContainsStringWithSuffix(sources, "/complex_money.dart");
    assertSetContainsStringWithSuffix(sources, "/currency_exchange.dart");
    assertSetContainsStringWithSuffix(sources, "/currency.dart");
    assertSetContainsStringWithSuffix(sources, "/simple_money.dart");
  }

  /**
   * This is a utility method which returns the DartSource from some DartLibary.
   */
  private DartSource computeDartSourceFromDartLibrary(DartLibrary dartLibrary) {
    try {
      IFile iFile = (IFile) dartLibrary.getDefiningCompilationUnit().getResource();
      LibrarySource librarySource = null;
      if (iFile != null && iFile.exists()) {
        File libFile = new File(iFile.getLocationURI());
        librarySource = new UrlLibrarySource(
            iFile.getLocationURI(),
            SystemLibraryManagerProvider.getSystemLibraryManager());
        return new UrlDartSource(libFile, librarySource);
      }
    } catch (DartModelException e) {
      fail("Not able to compute the DartSource for this DartLibrary: "
          + dartLibrary.getDisplayName()
          + " - "
          + e.getMessage());
    }
    fail("Not able to compute the DartSource for this DartLibrary: " + dartLibrary.getDisplayName());
    return null;
  }

  /**
   * Invoke the private method {@link DeltaProcessor#getgetCachedDirectives(DartLibrary)}.
   * 
   * @param unit the compilation unit to be passed in to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private CachedDirectives getCachedDirectives(DartLibrary library) throws Exception {
    DeltaProcessor deltaProcessor = DartModelManager.getInstance().getDeltaProcessor();
    Method method = DeltaProcessor.class.getDeclaredMethod("getCachedDirectives", DartLibrary.class);
    method.setAccessible(true);
    return (CachedDirectives) method.invoke(deltaProcessor, library);
  }

  /**
   * Invoke the private method
   * {@link DeltaProcessor#getgetCachedDirectives(DartSource, DartLibraryImpl)}.
   * 
   * @param unit the compilation unit to be passed in to the method
   * @return the result of executing the method
   * @throws Exception if the method could not be executed or itself throws an exception
   */
  private CachedDirectives getCachedDirectives(DartSource dartSrc, DartLibraryImpl library)
      throws Exception {
    DeltaProcessor deltaProcessor = DartModelManager.getInstance().getDeltaProcessor();
    Method method = DeltaProcessor.class.getDeclaredMethod(
        "getCachedDirectives",
        DartSource.class,
        DartLibraryImpl.class);
    method.setAccessible(true);
    return (CachedDirectives) method.invoke(deltaProcessor, dartSrc, library);
  }
}
