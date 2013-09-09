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
package com.google.dart.engine.internal.context;

/**
 * Instances of the class {@code TimestampedData} represent analysis data for which we have a
 * modification time.
 */
public class TimestampedData<E> {
  /**
   * The modification time of the source from which the data was created.
   */
  private long modificationTime;

  /**
   * The data that was created from the source.
   */
  private E data;

  /**
   * Initialize a newly created holder to hold the given values.
   * 
   * @param modificationTime the modification time of the source from which the data was created
   * @param unit the data that was created from the source
   */
  public TimestampedData(long modificationTime, E data) {
    this.modificationTime = modificationTime;
    this.data = data;
  }

  /**
   * Return the data that was created from the source.
   * 
   * @return the data that was created from the source
   */
  public E getData() {
    return data;
  }

  /**
   * Return the modification time of the source from which the data was created.
   * 
   * @return the modification time of the source from which the data was created
   */
  public long getModificationTime() {
    return modificationTime;
  }
}
