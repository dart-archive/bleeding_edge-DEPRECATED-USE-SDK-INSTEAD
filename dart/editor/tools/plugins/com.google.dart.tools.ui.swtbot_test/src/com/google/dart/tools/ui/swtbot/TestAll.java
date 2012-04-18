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
package com.google.dart.tools.ui.swtbot;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The set of all SWTBot tests for the Dart Editor.
 */
@RunWith(Suite.class)
//@Suite.SuiteClasses({InitialEditorStateTest.class})
//@Suite.SuiteClasses({NewApplicationActionTest.class})
//@Suite.SuiteClasses({SamplesUITest.class})
//@Suite.SuiteClasses({EndToEndUITest.class})
@Suite.SuiteClasses({
    InitialEditorStateTest.class, NewApplicationActionTest.class, SamplesUITest.class,
    EndToEndUITest.class})
public final class TestAll {
}
