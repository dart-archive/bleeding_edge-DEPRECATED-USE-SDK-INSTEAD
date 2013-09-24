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
package com.google.dart.tools.core.pub;

import java.util.List;

/**
 * A listener that watches for updates to the pub packages list from pub.dartlang
 */
public interface IPubPackageListener {

  /**
   * Notifies that the pub packages list has been updated with latest information from pub.dartlang
   */
  void pubPackagesChanged(List<PubPackageObject> pubPackages);

}
