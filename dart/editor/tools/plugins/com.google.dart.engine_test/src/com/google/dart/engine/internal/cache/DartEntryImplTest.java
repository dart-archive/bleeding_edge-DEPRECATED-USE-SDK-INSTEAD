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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.CacheState;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class DartEntryImplTest extends EngineTestCase {

  public void test_creation() throws Exception {
    DartEntryImpl info = new DartEntryImpl();
    assertSame(CacheState.INVALID, info.getState(DartEntry.ELEMENT));
    assertSame(CacheState.INVALID, info.getState(DartEntry.INCLUDED_PARTS));
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_CLIENT));
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.INVALID, info.getState(DartEntry.LINE_INFO));
    assertSame(CacheState.INVALID, info.getState(DartEntry.PARSE_ERRORS));
    assertSame(CacheState.INVALID, info.getState(DartEntry.PARSED_UNIT));
    assertSame(CacheState.INVALID, info.getState(DartEntry.PUBLIC_NAMESPACE));
    assertSame(CacheState.INVALID, info.getState(DartEntry.REFERENCED_LIBRARIES));
  }

  public void test_isClient() throws Exception {
    DartEntryImpl info = new DartEntryImpl();
    // true
    info.setValue(DartEntry.IS_CLIENT, true);
    assertTrue(info.getValue(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_CLIENT));
    // invalidate
    info.setState(DartEntry.IS_CLIENT, CacheState.INVALID);
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_CLIENT));
    // false
    info.setValue(DartEntry.IS_CLIENT, false);
    assertFalse(info.getValue(DartEntry.IS_CLIENT));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_CLIENT));
  }

  public void test_isLaunchable() throws Exception {
    DartEntryImpl info = new DartEntryImpl();
    // true
    info.setValue(DartEntry.IS_LAUNCHABLE, true);
    assertTrue(info.getValue(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_LAUNCHABLE));
    // invalidate
    info.setState(DartEntry.IS_LAUNCHABLE, CacheState.INVALID);
    assertSame(CacheState.INVALID, info.getState(DartEntry.IS_LAUNCHABLE));
    // false
    info.setValue(DartEntry.IS_LAUNCHABLE, false);
    assertFalse(info.getValue(DartEntry.IS_LAUNCHABLE));
    assertSame(CacheState.VALID, info.getState(DartEntry.IS_LAUNCHABLE));
  }

  public void test_resolutionState() throws Exception {
    DartEntryImpl info1 = new DartEntryImpl();

    Source libSrc1 = new TestSource(null, createFile("/test1.dart"), "");
    Source libSrc2 = new TestSource(null, createFile("/test2.dart"), "");

    ParserErrorCode errCode = ParserErrorCode.DIRECTIVE_AFTER_DECLARATION;
    AnalysisError[] errors1 = new AnalysisError[] {new AnalysisError(libSrc1, 0, 10, errCode)};
    AnalysisError[] errors2 = new AnalysisError[] {new AnalysisError(libSrc2, 0, 20, errCode)};

    info1.setValue(DartEntry.RESOLUTION_ERRORS, libSrc1, errors1);
    info1.setValue(DartEntry.RESOLUTION_ERRORS, libSrc2, errors2);

    DartEntryImpl info2 = new DartEntryImpl();
    info2.copyFrom(info1);
    assertExactElements(info2.getAllErrors(), errors1[0], errors2[0]);

    info1.removeResolution(libSrc2);
    assertExactElements(info1.getAllErrors(), errors1[0]);

    info2.removeResolution(libSrc1);
    assertExactElements(info2.getAllErrors(), errors2[0]);

    info2.removeResolution(libSrc2);
    assertExactElements(info2.getAllErrors());
  }
}
