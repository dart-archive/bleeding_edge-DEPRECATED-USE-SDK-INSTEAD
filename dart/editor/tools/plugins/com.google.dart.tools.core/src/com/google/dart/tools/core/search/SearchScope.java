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
package com.google.dart.tools.core.search;


/**
 * The interface <code>SearchScope</code> defines the behavior common to objects that define where
 * search result should be found by a <code>SearchEngine</code>. Clients must pass an instance of
 * this interface to the <code>search(...)</code> methods. Such an instance can be created using the
 * following factory methods on <code>SearchEngine</code>: <code>createHierarchyScope(Type)</code>,
 * <code>createDartSearchScope(IResource[])</code>, <code>createWorkspaceScope()</code>, or clients
 * may choose to implement this interface.
 * 
 * @coverage dart.tools.core.search
 */
public interface SearchScope {
}
