/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.PackageLibraryManager;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public abstract class PackageLibraryManagerProviderTest extends TestCase {

  public void test_PackageLibraryManagerProvider_expandDoesNotExist() throws Exception {
    URI shortUri = new URI("dart:doesnotexist.lib");
    URI fullUri = getLibraryManager().expandRelativeDartUri(shortUri);
    assertNull(fullUri);
  }

  public void test_PackageLibraryManagerProvider_getAllLibrarySpecs() throws Exception {
    PackageLibraryManager libraryManager = getLibraryManager();
    Collection<String> specs = libraryManager.getAllLibrarySpecs();
//    System.out.println(getClass().getName());
//    System.out.println("  " + specs.size() + " system libraries");
//    for (String eachSpec : new TreeSet<String>(specs)) {
//      URI uri;
//      try {
//        uri = libraryManager.resolveDartUri(new URI(eachSpec));
//      } catch (Exception e) {
//        System.out.println("Failed to resolve " + eachSpec);
//        e.printStackTrace();
//        continue;
//      }
//      StringBuilder builder = new StringBuilder();
//      builder.append("  ");
//      builder.append(eachSpec);
//      while (builder.length() < 20) {
//        builder.append(' ');
//      }
//      builder.append(" --> ");
//      builder.append(uri);
//      System.out.println(builder.toString());
//    }
    assertNotNull(specs);
    assertTrue(specs.contains("dart:core"));
    assertTrue(specs.contains("dart:coreimpl"));
    assertTrue(specs.contains("dart:html"));
    assertTrue(specs.contains("dart:uri"));
  }

  public void test_PackageLibraryManagerProvider_getAllLibrarySpecs_no_duplicates()
      throws Exception {
    PackageLibraryManager libraryManager = getLibraryManager();
    Collection<String> specs = libraryManager.getAllLibrarySpecs();
    Collection<String> visited = new HashSet<String>();
    String actual = "";
    for (String eachSpec : specs) {
      if (visited.contains(eachSpec)) {
        actual += eachSpec + ", ";
      } else {
        visited.add(eachSpec);
      }
    }
    assertEquals("", actual);
  }

  public void test_PackageLibraryManagerProvider_revertFileUri() throws Exception {
    assertNull(getLibraryManager().getRelativeUri(null));
    assertNull(getLibraryManager().getRelativeUri(new URI("boo://does/not/exist.dart")));
    assertNull(getLibraryManager().getRelativeUri(new File("doesNotExist.dart").toURI()));
  }

  protected abstract PackageLibraryManager getLibraryManager();

  protected void testLibrary(String shortLibName, String libFileName) throws URISyntaxException,
      AssertionError {
    final URI shortUri = new URI("dart:" + shortLibName);

    final URI fullUri1 = getLibraryManager().expandRelativeDartUri(shortUri);
    assertNotNull(fullUri1);
    assertEquals("dart", fullUri1.getScheme());
    assertEquals(shortLibName, fullUri1.getHost());
    assertTrue(fullUri1.getPath(), fullUri1.getPath().endsWith("/" + libFileName));
    URI fullUri2 = getLibraryManager().expandRelativeDartUri(fullUri1);
    assertEquals(fullUri1, fullUri2);
    URI shortUri2 = getLibraryManager().getShortUri(fullUri1);
    assertEquals(shortUri, shortUri2);

    URI translatedUri1 = getLibraryManager().resolveDartUri(fullUri1);
    assertNotNull(translatedUri1);
    String scheme = translatedUri1.getScheme();
    assertTrue(scheme.equals("file"));
    assertTrue(translatedUri1.getPath().endsWith("/" + libFileName));
    URI shortUri3 = getLibraryManager().getShortUri(translatedUri1);
    assertEquals(shortUri, shortUri3);

    URI fullUri3 = getLibraryManager().getRelativeUri(translatedUri1);
    assertEquals(fullUri1, fullUri3);

    File dir = new File(translatedUri1).getParentFile();
    URI translatedUri2 = new File(dir, "afile.dart").toURI();
    URI fullUri4 = getLibraryManager().getRelativeUri(translatedUri2);
    assertEquals(fullUri1.resolve("afile.dart"), fullUri4);

    URI translatedUri3 = new File(new File(dir, "somedir"), "somefile.dart").toURI();
    URI fullUri5 = getLibraryManager().getRelativeUri(translatedUri3);
    assertEquals(fullUri1.resolve("somedir/somefile.dart"), fullUri5);
  }

  protected void testPackage(String libFileName, String uriString) throws AssertionError,
      URISyntaxException {
    List<File> packageRoots = new ArrayList<File>();
    packageRoots.addAll(PackageLibraryManagerProvider.getAnyLibraryManager().getPackageRoots());
    List<File> roots = new ArrayList<File>();
    roots.add(new File(System.getProperty("user.home")));
    PackageLibraryManagerProvider.getAnyLibraryManager().setPackageRoots(roots);

    final URI fullUri1 = getLibraryManager().expandRelativeDartUri(
        new URI("package", null, "/" + libFileName, null));
    assertNotNull(fullUri1);
    assertEquals("package", fullUri1.getScheme());
    assertTrue(fullUri1.getPath(), (fullUri1.getHost() + fullUri1.getPath()).endsWith(libFileName));

    // TODO(keertip): fix test, this will fail, we now check for file exists
    URI translatedUri = getLibraryManager().resolveDartUri(fullUri1);
    assertNotNull(translatedUri);
    String scheme = translatedUri.getScheme();
    assertTrue(scheme.equals("file"));
    assertTrue(translatedUri.getPath().endsWith("/" + libFileName));
    URI shortUri3 = getLibraryManager().getShortUri(translatedUri);
    assertEquals(new URI(uriString), shortUri3);

    PackageLibraryManagerProvider.getAnyLibraryManager().setPackageRoots(packageRoots);

  }
}
