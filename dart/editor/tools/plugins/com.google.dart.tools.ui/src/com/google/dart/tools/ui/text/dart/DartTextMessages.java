/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text.dart;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages. Provisional API: This class/interface is part of an interim
 * API that is still under development and expected to change significantly before reaching
 * stability. It is being made available at this early stage to solicit feedback from pioneering
 * adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
final class DartTextMessages extends NLS {

  private static final String BUNDLE_NAME = DartTextMessages.class.getName();

  public static String ResultCollector_anonymous_type;

  public static String ResultCollector_overridingmethod;
  static {
    NLS.initializeMessages(BUNDLE_NAME, DartTextMessages.class);
  }

  private DartTextMessages() {
    // Do not instantiate
  }
}
