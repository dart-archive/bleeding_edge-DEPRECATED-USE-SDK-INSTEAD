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
package com.google.dart.tools.core.utilities.collections;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * A hierarchical set of files and directories. Adding or removing a directory from the set adds or
 * removes all of the files and directories contained in that directory recursively. The files and
 * directories contained in this set need not exist as the set represents the set of all possible
 * files and directories and not what currently exists on disk.
 */
public class FileSet {

  /**
   * Represents a path segment in the file set that is either included or excluded as specified by
   * {@link #included}. Any child segments not explicitly specified in {@link #children} are
   * included only if the receiver is included. </br> When a file or directory is added to the set,
   * the path for that file or directory is separated into segments with each successive segment
   * being a child of the previous segment. For example /user/home/boo is separated into 3 segments
   * and added such that
   * 
   * <pre>
   * <root-segment> ==> user ==> home ==> boo
   * </pre>
   * 
   * where <code>==></code> represents a parent / child relationship.
   */
  class Segment {
    /** The name of the segment or {@code null} if this is the root segment */
    final String name;
    /** {@code true} if this segment is included in the file set */
    final boolean included;
    /** An array of child segments or {@code null} if none */
    ArrayList<Segment> children;

    Segment(String name, boolean included) {
      this.name = name;
      this.included = included;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "[" + name + "," + included + "]";
    }

    /**
     * Add the segment names in path starting at the specified index to the receiver
     * 
     * @param path the path containing the segment names to be added (not {@code null})
     * @param nameStart the starting index of the segment name
     * @return {@code true} if segments were added or {@code false} if the receiver already
     *         contained the segment names
     */
    boolean add(String path, int nameStart) {
      final int nameEnd = path.indexOf(File.separatorChar, nameStart);
      final boolean isLeaf = nameEnd == -1;
      final String childName = path.substring(nameStart, isLeaf ? path.length() : nameEnd);

      // Search for a child representing the path segment
      if (children != null) {
        ListIterator<Segment> iter = children.listIterator();
        while (iter.hasNext()) {
          Segment child = iter.next();
          if (child.name.equals(childName)) {

            // If the child is included in the set and there are no grand children
            // then the set already contains the file
            if (child.included && child.children == null) {
              return false;
            }

            // Recurse until all path segments are matched
            if (!isLeaf) {
              return child.add(path, nameEnd + 1);
            }

            // Replace the current child to indicate that it and all contained files
            // and directories are included in the set
            iter.set(new Segment(childName, true));
            return true;
          }
        }
      } else {
        children = new ArrayList<Segment>(1);
      }

      // If no matching child, then add a new child representing the path segment
      Segment child = new Segment(childName, isLeaf);
      children.add(child);

      // Recursively add segments
      if (!isLeaf) {
        child.add(path, nameEnd + 1);
      }
      return true;
    }

    /**
     * Answer {@code true} if the receiver contains the segment names in path starting at the
     * specified index
     * 
     * @param path the path containing the segment names to be matched (not {@code null})
     * @param nameStart the starting index
     * @return {@code true} if the receiver contains the segment names
     */
    boolean contains(String path, int nameStart) {

      // Find a child matching the segment name
      if (children != null) {
        final int nameEnd = path.indexOf(File.separatorChar, nameStart);
        final boolean isLeaf = nameEnd == -1;
        final String childName = path.substring(nameStart, isLeaf ? path.length() : nameEnd);

        for (Segment child : children) {
          if (child.name.equals(childName)) {

            // Recurse until all path segments are matched
            if (!isLeaf) {
              return child.contains(path, nameEnd + 1);
            }

            // The child defines whether the file is in the set.
            return child.included;
          }
        }
      }

      // If no child is found, then the receiver defines
      // whether or not all child segments are included in the set.
      return included;
    }

    /**
     * Remove the segment names in path starting at the specified index from the receiver
     * 
     * @param path the path containing the segment names to be removed (not {@code null})
     * @param nameStart the starting index of the segment name
     * @return {@code true} if segments were removed or {@code false} if the receiver did not
     *         contain the segment names
     */
    boolean remove(String path, int nameStart) {
      final int nameEnd = path.indexOf(File.separatorChar, nameStart);
      final boolean isLeaf = nameEnd == -1;
      final String childName = path.substring(nameStart, isLeaf ? path.length() : nameEnd);

      // Search for a child representing the path segment
      if (children != null) {
        ListIterator<Segment> iter = children.listIterator();
        while (iter.hasNext()) {
          Segment child = iter.next();
          if (child.name.equals(childName)) {

            // Recurse until all path segments are matched
            if (!isLeaf) {
              return child.remove(path, nameEnd + 1);
            }

            // If the child is not included in the set and there are no grand children
            // then the set does not contain the file
            if (!child.included && child.children == null) {
              return false;
            }

            // Replace the current child to indicate that it and all contained files
            // and directories are not included in the set
            iter.set(new Segment(childName, false));
            return true;
          }
        }
      }

      // If the receiver represents a file that is excluded from the set
      // then the file in question is not part of the set
      if (!included) {
        return false;
      }

      if (children == null) {
        children = new ArrayList<Segment>(1);
      }

      // If no matching child, then add a new child representing the path segment
      Segment child = new Segment(childName, !isLeaf);
      children.add(child);

      // Recursively add segments
      if (!isLeaf) {
        child.remove(path, nameEnd + 1);
      }
      return true;
    }
  }

  /**
   * The root segment for the set.
   */
  private final Segment root = new Segment(null, false);

  /**
   * Add the specified file to the receiver. If the specified file is a directory then that
   * directory plus all of its contents recursively will be added to the set.
   * 
   * @param file the file (must be absolute and not {@code null})
   * @return {@code true} if the receiver changed as a result of this operation.
   */
  public boolean add(File file) {
    return root.add(file.getAbsolutePath(), 0);
  }

  /**
   * Answer {@code true} if the receiver contains the specified file.
   */
  public boolean contains(File file) {
    return root.contains(file.getAbsolutePath(), 0);
  }

  /**
   * Remove the specified file from the receiver. If the specified file is a directory then that
   * directory plus all of its contents recursively will be removed from the set.
   * 
   * @param file the file (must be absolute and not {@code null})
   * @return {@code true} if the receiver changed as a result of this operation.
   */
  public boolean remove(File file) {
    return root.remove(file.getAbsolutePath(), 0);
  }
}
