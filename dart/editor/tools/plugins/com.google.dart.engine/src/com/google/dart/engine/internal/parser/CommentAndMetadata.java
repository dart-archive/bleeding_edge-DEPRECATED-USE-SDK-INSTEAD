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
package com.google.dart.engine.internal.parser;

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.Comment;

import java.util.List;

/**
 * Instances of the class {@code CommentAndMetadata} implement a simple data-holder for a method
 * that needs to return multiple values.
 */
public class CommentAndMetadata {
  /**
   * The documentation comment that was parsed, or {@code null} if none was given.
   */
  private Comment comment;

  /**
   * The metadata that was parsed.
   */
  private List<Annotation> metadata;

  /**
   * Initialize a newly created holder with the given data.
   * 
   * @param comment the documentation comment that was parsed
   * @param metadata the metadata that was parsed
   */
  public CommentAndMetadata(Comment comment, List<Annotation> metadata) {
    this.comment = comment;
    this.metadata = metadata;
  }

  /**
   * Return the documentation comment that was parsed, or {@code null} if none was given.
   * 
   * @return the documentation comment that was parsed
   */
  public Comment getComment() {
    return comment;
  }

  /**
   * Return the metadata that was parsed. If there was no metadata, then the list will be empty.
   * 
   * @return the metadata that was parsed
   */
  public List<Annotation> getMetadata() {
    return metadata;
  }
}
