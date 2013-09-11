/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.PubSuggestionCode;
import com.google.dart.engine.source.Source;

public class PubSuggestionCodeTest extends ResolverTestCase {
  public void test_import_package() throws Exception {
    Source source = addSource(createSource(//
    "import 'package:somepackage/other.dart';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.URI_DOES_NOT_EXIST);
  }

  public void test_import_packageWithDotDot() throws Exception {
    Source source = addSource(createSource(//
    "import 'package:somepackage/../other.dart';"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT);
  }

  public void test_import_packageWithLeadingDotDot() throws Exception {
    Source source = addSource(createSource(//
    "import 'package:../other.dart';"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.URI_DOES_NOT_EXIST,
        PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT);
  }

  public void test_import_referenceIntoLibDirectory() throws Exception {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/lib/other.dart", "");
    Source source = addSource("/myproj/web/test.dart", createSource(//
        "import '../lib/other.dart';"));
    resolve(source);
    assertErrors(source, PubSuggestionCode.FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE);
  }

  public void test_import_referenceIntoLibDirectory_no_pubspec() throws Exception {
    cacheSource("/myproj/lib/other.dart", "");
    Source source = addSource("/myproj/web/test.dart", createSource(//
        "import '../lib/other.dart';"));
    resolve(source);
    assertNoErrors(source);
  }

  public void test_import_referenceOutOfLibDirectory() throws Exception {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/web/other.dart", "");
    Source source = addSource("/myproj/lib/test.dart", createSource(//
        "import '../web/other.dart';"));
    resolve(source);
    assertErrors(source, PubSuggestionCode.FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE);
  }

  public void test_import_referenceOutOfLibDirectory_no_pubspec() throws Exception {
    cacheSource("/myproj/web/other.dart", "");
    Source source = addSource("/myproj/lib/test.dart", createSource(//
        "import '../web/other.dart';"));
    resolve(source);
    assertNoErrors(source);
  }

  public void test_import_valid_inside_lib1() throws Exception {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/lib/other.dart", "");
    Source source = addSource("/myproj/lib/test.dart", createSource(//
        "import 'other.dart';"));
    resolve(source);
    assertNoErrors(source);
  }

  public void test_import_valid_inside_lib2() throws Exception {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/lib/bar/other.dart", "");
    Source source = addSource("/myproj/lib/foo/test.dart", createSource(//
        "import '../bar/other.dart';"));
    resolve(source);
    assertNoErrors(source);
  }

  public void test_import_valid_outside_lib() throws Exception {
    cacheSource("/myproj/pubspec.yaml", "");
    cacheSource("/myproj/web/other.dart", "");
    Source source = addSource("/myproj/lib2/test.dart", createSource(//
        "import '../web/other.dart';"));
    resolve(source);
    assertNoErrors(source);
  }
}
