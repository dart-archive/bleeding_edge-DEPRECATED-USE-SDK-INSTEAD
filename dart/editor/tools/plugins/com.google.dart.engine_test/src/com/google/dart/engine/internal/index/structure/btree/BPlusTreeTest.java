/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.index.structure.btree;

import com.google.common.collect.Sets;

import junit.framework.TestCase;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Set;

public class BPlusTreeTest extends TestCase {
  private static final Comparator<Integer> INT_COMPARATOR = new Comparator<Integer>() {
    @Override
    public int compare(Integer o1, Integer o2) {
      return o1.compareTo(o2);
    }
  };

  private NodeManager<Integer, String, Integer> nodeManager;
  private BPlusTree<Integer, String, Integer> tree;

  public void test_find() throws Exception {
    insertValues(12);
    assertEquals(null, tree.find(-1));
    assertEquals(null, tree.find(1000));
    for (int key = 0; key < 12; key++) {
      assertEquals("V" + key, tree.find(key));
    }
  }

  public void test_insert_01() throws Exception {
    tree.insert(0, "V0");
    assertTreeDump("LNode {0: V0}");
  }

  public void test_insert_02() throws Exception {
    tree.insert(1, "V1");
    tree.insert(0, "V0");
    assertTreeDump("LNode {0: V0, 1: V1}");
  }

  public void test_insert_03() throws Exception {
    tree.insert(2, "V2");
    tree.insert(0, "V0");
    tree.insert(1, "V1");
    assertTreeDump("LNode {0: V0, 1: V1, 2: V2}");
  }

  public void test_insert_05() throws Exception {
    insertValues(5);
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 3: V3, 4: V4}",
        "}");
  }

  public void test_insert_09() throws Exception {
    insertValues(9);
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7, 8: V8}",
        "}");
  }

  public void test_insert_innerSplitLeft() throws Exception {
    // Prepare a tree with '0' key missing.
    for (int i = 1; i < 12; i++) {
      tree.insert(i, "V" + i);
    }
    assertTreeDump(
        "INode {",
        "    LNode {1: V1, 2: V2}",
        "  3",
        "    LNode {3: V3, 4: V4}",
        "  5",
        "    LNode {5: V5, 6: V6}",
        "  7",
        "    LNode {7: V7, 8: V8}",
        "  9",
        "    LNode {9: V9, 10: V10, 11: V11}",
        "}");
    // Split and insert into the 'left' child.
    tree.insert(0, "V0");
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1, 2: V2}",
        "      3",
        "        LNode {3: V3, 4: V4}",
        "      5",
        "        LNode {5: V5, 6: V6}",
        "    }",
        "  7",
        "    INode {",
        "        LNode {7: V7, 8: V8}",
        "      9",
        "        LNode {9: V9, 10: V10, 11: V11}",
        "    }",
        "}");
  }

  public void test_insert_innerSplitRight() throws Exception {
    insertValues(12);
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1}",
        "      2",
        "        LNode {2: V2, 3: V3}",
        "      4",
        "        LNode {4: V4, 5: V5}",
        "    }",
        "  6",
        "    INode {",
        "        LNode {6: V6, 7: V7}",
        "      8",
        "        LNode {8: V8, 9: V9, 10: V10, 11: V11}",
        "    }",
        "}");
  }

  public void test_insert_replace() throws Exception {
    tree.insert(0, "A");
    tree.insert(1, "B");
    tree.insert(2, "C");
    assertTreeDump("LNode {0: A, 1: B, 2: C}");
    tree.insert(2, "C2");
    tree.insert(1, "B2");
    tree.insert(0, "A2");
    assertTreeDump("LNode {0: A2, 1: B2, 2: C2}");
  }

  public void test_remove_internal_borrowLeft() throws Exception {
    createTree(10, 4);
    for (int i = 100; i < 125; i++) {
      tree.insert(i, "V" + i);
    }
    for (int i = 0; i < 10; i++) {
      tree.insert(i, "V" + i);
    }
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1}",
        "      2",
        "        LNode {2: V2, 3: V3}",
        "      4",
        "        LNode {4: V4, 5: V5}",
        "      6",
        "        LNode {6: V6, 7: V7}",
        "      8",
        "        LNode {8: V8, 9: V9, 100: V100, 101: V101}",
        "      102",
        "        LNode {102: V102, 103: V103}",
        "      104",
        "        LNode {104: V104, 105: V105}",
        "      106",
        "        LNode {106: V106, 107: V107}",
        "      108",
        "        LNode {108: V108, 109: V109}",
        "      110",
        "        LNode {110: V110, 111: V111}",
        "    }",
        "  112",
        "    INode {",
        "        LNode {112: V112, 113: V113}",
        "      114",
        "        LNode {114: V114, 115: V115}",
        "      116",
        "        LNode {116: V116, 117: V117}",
        "      118",
        "        LNode {118: V118, 119: V119}",
        "      120",
        "        LNode {120: V120, 121: V121}",
        "      122",
        "        LNode {122: V122, 123: V123, 124: V124}",
        "    }",
        "}");
    assertEquals("V112", tree.remove(112));
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1}",
        "      2",
        "        LNode {2: V2, 3: V3}",
        "      4",
        "        LNode {4: V4, 5: V5}",
        "      6",
        "        LNode {6: V6, 7: V7}",
        "      8",
        "        LNode {8: V8, 9: V9, 100: V100, 101: V101}",
        "      102",
        "        LNode {102: V102, 103: V103}",
        "      104",
        "        LNode {104: V104, 105: V105}",
        "    }",
        "  106",
        "    INode {",
        "        LNode {106: V106, 107: V107}",
        "      108",
        "        LNode {108: V108, 109: V109}",
        "      110",
        "        LNode {110: V110, 111: V111}",
        "      112",
        "        LNode {113: V113, 114: V114, 115: V115}",
        "      116",
        "        LNode {116: V116, 117: V117}",
        "      118",
        "        LNode {118: V118, 119: V119}",
        "      120",
        "        LNode {120: V120, 121: V121}",
        "      122",
        "        LNode {122: V122, 123: V123, 124: V124}",
        "    }",
        "}");
  }

  public void test_remove_internal_borrowRight() throws Exception {
    createTree(10, 4);
    for (int i = 100; i < 135; i++) {
      tree.insert(i, "V" + i);
    }
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {100: V100, 101: V101}",
        "      102",
        "        LNode {102: V102, 103: V103}",
        "      104",
        "        LNode {104: V104, 105: V105}",
        "      106",
        "        LNode {106: V106, 107: V107}",
        "      108",
        "        LNode {108: V108, 109: V109}",
        "      110",
        "        LNode {110: V110, 111: V111}",
        "    }",
        "  112",
        "    INode {",
        "        LNode {112: V112, 113: V113}",
        "      114",
        "        LNode {114: V114, 115: V115}",
        "      116",
        "        LNode {116: V116, 117: V117}",
        "      118",
        "        LNode {118: V118, 119: V119}",
        "      120",
        "        LNode {120: V120, 121: V121}",
        "      122",
        "        LNode {122: V122, 123: V123}",
        "      124",
        "        LNode {124: V124, 125: V125}",
        "      126",
        "        LNode {126: V126, 127: V127}",
        "      128",
        "        LNode {128: V128, 129: V129}",
        "      130",
        "        LNode {130: V130, 131: V131}",
        "      132",
        "        LNode {132: V132, 133: V133, 134: V134}",
        "    }",
        "}");
    assertEquals("V100", tree.remove(100));
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {101: V101, 102: V102, 103: V103}",
        "      104",
        "        LNode {104: V104, 105: V105}",
        "      106",
        "        LNode {106: V106, 107: V107}",
        "      108",
        "        LNode {108: V108, 109: V109}",
        "      110",
        "        LNode {110: V110, 111: V111}",
        "      112",
        "        LNode {112: V112, 113: V113}",
        "      114",
        "        LNode {114: V114, 115: V115}",
        "      116",
        "        LNode {116: V116, 117: V117}",
        "    }",
        "  118",
        "    INode {",
        "        LNode {118: V118, 119: V119}",
        "      120",
        "        LNode {120: V120, 121: V121}",
        "      122",
        "        LNode {122: V122, 123: V123}",
        "      124",
        "        LNode {124: V124, 125: V125}",
        "      126",
        "        LNode {126: V126, 127: V127}",
        "      128",
        "        LNode {128: V128, 129: V129}",
        "      130",
        "        LNode {130: V130, 131: V131}",
        "      132",
        "        LNode {132: V132, 133: V133, 134: V134}",
        "    }",
        "}");
  }

  public void test_remove_internal_mergeLeft() throws Exception {
    insertValues(15);
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1}",
        "      2",
        "        LNode {2: V2, 3: V3}",
        "      4",
        "        LNode {4: V4, 5: V5}",
        "    }",
        "  6",
        "    INode {",
        "        LNode {6: V6, 7: V7}",
        "      8",
        "        LNode {8: V8, 9: V9}",
        "      10",
        "        LNode {10: V10, 11: V11}",
        "      12",
        "        LNode {12: V12, 13: V13, 14: V14}",
        "    }",
        "}");
    assertEquals("V12", tree.remove(12));
    assertEquals("V13", tree.remove(13));
    assertEquals("V14", tree.remove(14));
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1}",
        "      2",
        "        LNode {2: V2, 3: V3}",
        "      4",
        "        LNode {4: V4, 5: V5}",
        "    }",
        "  6",
        "    INode {",
        "        LNode {6: V6, 7: V7}",
        "      8",
        "        LNode {8: V8, 9: V9}",
        "      10",
        "        LNode {10: V10, 11: V11}",
        "    }",
        "}");
    assertEquals("V8", tree.remove(8));
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7, 9: V9}",
        "  10",
        "    LNode {10: V10, 11: V11}",
        "}");
  }

  public void test_remove_internal_mergeRight() throws Exception {
    insertValues(12);
    assertTreeDump(
        "INode {",
        "    INode {",
        "        LNode {0: V0, 1: V1}",
        "      2",
        "        LNode {2: V2, 3: V3}",
        "      4",
        "        LNode {4: V4, 5: V5}",
        "    }",
        "  6",
        "    INode {",
        "        LNode {6: V6, 7: V7}",
        "      8",
        "        LNode {8: V8, 9: V9, 10: V10, 11: V11}",
        "    }",
        "}");
    assertEquals("V0", tree.remove(0));
    assertTreeDump(
        "INode {",
        "    LNode {1: V1, 2: V2, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7}",
        "  8",
        "    LNode {8: V8, 9: V9, 10: V10, 11: V11}",
        "}");
  }

  public void test_remove_internal_notFound() throws Exception {
    insertValues(20);
    assertEquals(null, tree.remove(100));
  }

  public void test_remove_leaf_borrowLeft() throws Exception {
    createTree(10, 10);
    for (int i = 20; i < 40; i++) {
      tree.insert(i, "V" + i);
    }
    for (int i = 0; i < 5; i++) {
      tree.insert(i, "V" + i);
    }
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1, 2: V2, 3: V3, 4: V4, 20: V20, 21: V21, 22: V22, 23: V23, 24: V24}",
        "  25",
        "    LNode {25: V25, 26: V26, 27: V27, 28: V28, 29: V29}",
        "  30",
        "    LNode {30: V30, 31: V31, 32: V32, 33: V33, 34: V34, 35: V35, 36: V36, 37: V37, 38: V38, 39: V39}",
        "}");
    assertEquals("V25", tree.remove(25));
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1, 2: V2, 3: V3, 4: V4, 20: V20, 21: V21}",
        "  22",
        "    LNode {22: V22, 23: V23, 24: V24, 26: V26, 27: V27, 28: V28, 29: V29}",
        "  30",
        "    LNode {30: V30, 31: V31, 32: V32, 33: V33, 34: V34, 35: V35, 36: V36, 37: V37, 38: V38, 39: V39}",
        "}");
  }

  public void test_remove_leaf_borrowRight() throws Exception {
    createTree(10, 10);
    insertValues(15);
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1, 2: V2, 3: V3, 4: V4}",
        "  5",
        "    LNode {5: V5, 6: V6, 7: V7, 8: V8, 9: V9, 10: V10, 11: V11, 12: V12, 13: V13, 14: V14}",
        "}");
    assertEquals("V0", tree.remove(0));
    assertTreeDump(
        "INode {",
        "    LNode {1: V1, 2: V2, 3: V3, 4: V4, 5: V5, 6: V6, 7: V7}",
        "  8",
        "    LNode {8: V8, 9: V9, 10: V10, 11: V11, 12: V12, 13: V13, 14: V14}",
        "}");
  }

  public void test_remove_leaf_mergeLeft() throws Exception {
    insertValues(9);
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7, 8: V8}",
        "}");
    assertEquals("V2", tree.remove(2));
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7, 8: V8}",
        "}");
  }

  public void test_remove_leaf_mergeRight() throws Exception {
    insertValues(9);
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7, 8: V8}",
        "}");
    assertEquals("V1", tree.remove(1));
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 2: V2, 3: V3}",
        "  4",
        "    LNode {4: V4, 5: V5}",
        "  6",
        "    LNode {6: V6, 7: V7, 8: V8}",
        "}");
  }

  public void test_remove_leaf_noReorder() throws Exception {
    insertValues(5);
    assertTreeDump(
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 3: V3, 4: V4}",
        "}");
    assertEquals("V3", tree.remove(3));
    assertTreeDump(//
        "INode {",
        "    LNode {0: V0, 1: V1}",
        "  2",
        "    LNode {2: V2, 4: V4}",
        "}");
  }

  public void test_remove_leafRoot_becomesEmpty() throws Exception {
    insertValues(1);
    assertTreeDump("LNode {0: V0}");
    assertEquals("V0", tree.remove(0));
    assertTreeDump("LNode {}");
  }

  public void test_remove_leafRoot_first() throws Exception {
    insertValues(3);
    assertTreeDump("LNode {0: V0, 1: V1, 2: V2}");
    assertEquals("V0", tree.remove(0));
    assertTreeDump("LNode {1: V1, 2: V2}");
  }

  public void test_remove_leafRoot_last() throws Exception {
    insertValues(3);
    assertTreeDump("LNode {0: V0, 1: V1, 2: V2}");
    assertEquals("V2", tree.remove(2));
    assertTreeDump("LNode {0: V0, 1: V1}");
  }

  public void test_remove_leafRoot_middle() throws Exception {
    insertValues(3);
    assertTreeDump("LNode {0: V0, 1: V1, 2: V2}");
    assertEquals("V1", tree.remove(1));
    assertTreeDump("LNode {0: V0, 2: V2}");
  }

  public void test_stress_evenOdd() throws Exception {
    int count = 1000;
    // insert odd, forward
    for (int i = 1; i < count; i += 2) {
      tree.insert(i, "V" + i);
    }
    // insert even, backward
    for (int i = count - 2; i >= 0; i -= 2) {
      tree.insert(i, "V" + i);
    }
    // find every
    for (int i = 0; i < count; i++) {
      assertEquals("V" + i, tree.find(i));
    }
    // remove odd, backward
    for (int i = count - 1; i >= 1; i -= 2) {
      assertEquals("V" + i, tree.remove(i));
    }
    for (int i = 0; i < count; i++) {
      if ((i % 2) == 0) {
        assertEquals("V" + i, tree.find(i));
      } else {
        assertEquals(null, tree.find(i));
      }
    }
    // remove even, forward
    for (int i = 0; i < count; i += 2) {
      tree.remove(i);
    }
    for (int i = 0; i < count; i++) {
      assertEquals(null, tree.find(i));
    }
  }

  public void test_stress_random() throws Exception {
    createTree(10, 10);
    int maxKey = 1000000;
    int tryCount = 1000;
    Set<Integer> keys = Sets.newHashSet();
    for (int i = 0; i < tryCount; i++) {
      int key = (int) (Math.random() * maxKey);
      keys.add(key);
      tree.insert(key, "V" + key);
    }
    // find every
    for (int key : keys) {
      assertEquals("V" + key, tree.find(key));
    }
    // remove random keys
    for (int key : Sets.newHashSet(keys)) {
      if (Math.random() > 0.5) {
        keys.remove(key);
        assertEquals("V" + key, tree.remove(key));
      }
    }
    // find every remaining key
    for (int key : keys) {
      assertEquals("V" + key, tree.find(key));
    }
  }

  @Override
  protected void setUp() throws Exception {
    createTree(4, 4);
  }

  private void assertTreeDump(String... lines) {
    String dump = getDump();
    assertEquals(StringUtils.join(lines, "\n") + "\n", dump);
  }

  private void createTree(int maxIndexKeys, int maxLeafKeys) {
    nodeManager = new MemoryNodeManager<Integer, String>(maxIndexKeys, maxLeafKeys);
    tree = new BPlusTree<Integer, String, Integer>(INT_COMPARATOR, nodeManager);
  }

  private String getDump() {
    StringBuilder sb = new StringBuilder();
    tree.writeOn(sb);
    String dump = sb.toString();
    return dump;
  }

  private void insertValues(int count) {
    for (int i = 0; i < count; i++) {
      tree.insert(i, "V" + i);
    }
  }
}
