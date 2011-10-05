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
package com.google.dart.indexer.index.configuration;

/**
 * Contributes information to the index. Contributors are registered per processor, and are expected
 * to update the index using the data prepared by the processor.
 * <p>
 * Note that this is a marker interface. Specific processors will define (and require implementation
 * of) corresponding specific interfaces.
 */
public interface Contributor {
}
