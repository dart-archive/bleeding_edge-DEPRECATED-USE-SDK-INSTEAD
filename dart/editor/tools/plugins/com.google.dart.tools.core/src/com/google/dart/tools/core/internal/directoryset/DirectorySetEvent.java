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
package com.google.dart.tools.core.internal.directoryset;

/**
 * Class represents some change in the {@link DirectorySetManager}.
 * <p>
 * Currently, whenever there is a change the entire refresh on the Files viewer, thus other than an
 * event being passed, we don't need any information.
 * <p>
 * TODO(jwren) Add context to this event object, so that the refresh can be faster in the Files
 * view.
 * 
 * @see DirectorySetManager
 * @see DirectorySetListener
 */
public class DirectorySetEvent {
}
