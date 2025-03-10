/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.kll;

import static org.apache.datasketches.kll.KllSketch.SketchType.FLOATS_SKETCH;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.apache.datasketches.common.SketchesArgumentException;
import org.apache.datasketches.kll.KllDirectFloatsSketch.KllDirectCompactFloatsSketch;
import org.apache.datasketches.memory.DefaultMemoryRequestServer;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.memory.MemoryRequestServer;
import org.apache.datasketches.memory.WritableMemory;
import org.apache.datasketches.quantilescommon.FloatsSortedView;
import org.apache.datasketches.quantilescommon.FloatsSortedViewIterator;
import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class KllMiscFloatsTest {
  static final String LS = System.getProperty("line.separator");
  private final MemoryRequestServer memReqSvr = new DefaultMemoryRequestServer();

  @Test
  public void checkSortedViewConstruction() {
    final KllFloatsSketch kll = KllFloatsSketch.newHeapInstance(20);
    for (int i = 1; i <= 20; i++) { kll.update(i); }
    FloatsSortedView fsv = kll.getSortedView();
    long[] cumWeights = fsv.getCumulativeWeights();
    float[] values = fsv.getQuantiles();
    assertEquals(cumWeights.length, 20);
    assertEquals(values.length, 20);
    for (int i = 0; i < 20; i++) {
      assertEquals(cumWeights[i], i + 1);
      assertEquals(values[i], i + 1);
    }
  }

  @Test
  public void checkBounds() {
    final KllFloatsSketch kll = KllFloatsSketch.newHeapInstance(); //default k = 200
    for (int i = 0; i < 1000; i++) {
      kll.update(i);
    }
    final double eps = kll.getNormalizedRankError(false);
    final float est = kll.getQuantile(0.5);
    final float ub = kll.getQuantileUpperBound(0.5);
    final float lb = kll.getQuantileLowerBound(0.5);
    assertEquals(ub, kll.getQuantile(.5 + eps));
    assertEquals(lb, kll.getQuantile(0.5 - eps));
    println("Ext     : " + est);
    println("UB      : " + ub);
    println("LB      : " + lb);
    final double rest = kll.getRank(est);
    final double restUB = kll.getRankUpperBound(rest);
    final double restLB = kll.getRankLowerBound(rest);
    assertTrue(restUB - rest < (2 * eps));
    assertTrue(rest - restLB < (2 * eps));
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkHeapifyExceptions1() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance();
    WritableMemory wmem = WritableMemory.writableWrap(sk.toByteArray());
    wmem.putByte(6, (byte) 3); //corrupt with odd M
    KllFloatsSketch.heapify(wmem);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkHeapifyExceptions2() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance();
    WritableMemory wmem = WritableMemory.writableWrap(sk.toByteArray());
    wmem.putByte(0, (byte) 1); //corrupt preamble ints, should be 2
    KllFloatsSketch.heapify(wmem);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkHeapifyExceptions3() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance();
    sk.update(1.0f);
    sk.update(2.0f);
    WritableMemory wmem = WritableMemory.writableWrap(sk.toByteArray());
    wmem.putByte(0, (byte) 1); //corrupt preamble ints, should be 5
    KllFloatsSketch.heapify(wmem);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkHeapifyExceptions4() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance();
    WritableMemory wmem = WritableMemory.writableWrap(sk.toByteArray());
    wmem.putByte(1, (byte) 0); //corrupt SerVer, should be 1 or 2
    KllFloatsSketch.heapify(wmem);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkHeapifyExceptions5() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance();
    WritableMemory wmem = WritableMemory.writableWrap(sk.toByteArray());
    wmem.putByte(2, (byte) 0); //corrupt FamilyID, should be 15
    KllFloatsSketch.heapify(wmem);
  }

  @Test //set static enablePrinting = true for visual checking
  public void checkMisc() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance(8);
    try { sk.getMaxItem(); fail(); } catch (SketchesArgumentException e) {} //empty
    println(sk.toString(true, true));
    for (int i = 0; i < 20; i++) { sk.update(i); }
    println(sk.toString(true, true));
    sk.toByteArray();
    final float[] items = sk.getFloatItemsArray();
    assertEquals(items.length, 16);
    final int[] levels = sk.getLevelsArray(sk.sketchStructure);
    assertEquals(levels.length, 3);
    assertEquals(sk.getNumLevels(), 2);
  }

  @Test //set static enablePrinting = true for visual checking
  public void visualCheckToString() {
    final KllFloatsSketch sk = KllFloatsSketch.newHeapInstance(20);
    int n = 21;
    for (int i = 1; i <= n; i++) { sk.update(i); }
    println(sk.toString(true, true));
    assertEquals(sk.getNumLevels(), 2);
    assertEquals(sk.getMinItem(), 1);
    assertEquals(sk.getMaxItem(), 21);
    assertEquals(sk.getNumRetained(), 11);

    final KllFloatsSketch sk2 = KllFloatsSketch.newHeapInstance(20);
    n = 400;
    for (int i = 101; i <= n + 100; i++) { sk2.update(i); }
    println("\n" + sk2.toString(true, true));
    assertEquals(sk2.getNumLevels(), 5);
    assertEquals(sk2.getMinItem(), 101);
    assertEquals(sk2.getMaxItem(), 500);
    assertEquals(sk2.getNumRetained(), 52);

    sk2.merge(sk);
    println(LS + sk2.toString(true, true));
    assertEquals(sk2.getNumLevels(), 5);
    assertEquals(sk2.getMinItem(), 1);
    assertEquals(sk2.getMaxItem(), 500);
    assertEquals(sk2.getNumRetained(), 56);
  }

  @Test //set static enablePrinting = true for visual checking
  public void viewHeapCompactions() {
    int k = 20;
    int n = 108;
    int compaction = 0;
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance(k);
    for (int i = 1; i <= n; i++) {
      sk.update(i);
      if (sk.levelsArr[0] == 0) {
        println(LS + "#<<< BEFORE COMPACTION # " + (++compaction) + " >>>");
        println(sk.toString(true, true));
        sk.update(++i);
        println(LS + "#<<< AFTER COMPACTION  # " + (compaction) + " >>>");
        println(sk.toString(true, true));
        assertEquals(sk.getFloatItemsArray()[sk.levelsArr[0]], i);
      }
    }
  }

  @Test //set static enablePrinting = true for visual checking
  public void viewDirectCompactions() {
    int k = 20;
    int n = 108;
    int sizeBytes = KllSketch.getMaxSerializedSizeBytes(k, n, FLOATS_SKETCH, true);
    WritableMemory wmem = WritableMemory.allocate(sizeBytes);
    KllFloatsSketch sk = KllFloatsSketch.newDirectInstance(k, wmem, memReqSvr);
    for (int i = 1; i <= n; i++) {
      sk.update(i);
      if (sk.levelsArr[0] == 0) {
        println(sk.toString(true, true));
        sk.update(++i);
        println(sk.toString(true, true));
        assertEquals(sk.getFloatItemsArray()[sk.levelsArr[0]], i);
      }
    }
  }

  @Test //set static enablePrinting = true for visual checking
  public void viewCompactionAndSortedView() {
    int n = 43;
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance(20);
    for (int i = 1; i <= n; i++) { sk.update(i); }
    println(sk.toString(true, true));
    FloatsSortedView sv = sk.getSortedView();
    FloatsSortedViewIterator itr = sv.iterator();
    println("### SORTED VIEW");
    printf("%12s%12s\n", "Value", "CumWeight");
    while (itr.next()) {
      float v = itr.getQuantile();
      long wt = itr.getWeight();
      printf("%12.1f%12d\n", v, wt);
    }
  }

  @Test
  public void checkGrowLevels() {
    KllFloatsSketch sk = KllFloatsSketch.newHeapInstance(20);
    for (int i = 1; i <= 21; i++) { sk.update(i); }
    assertEquals(sk.getNumLevels(), 2);
    assertEquals(sk.getFloatItemsArray().length, 33);
    assertEquals(sk.getLevelsArray(sk.sketchStructure)[2], 33);
  }

  @Test //set static enablePrinting = true for visual checking
  public void checkSketchInitializeFloatHeap() {
    int k = 20; //don't change this
    KllFloatsSketch sk;

    println("#### CASE: FLOAT FULL HEAP");
    sk = KllFloatsSketch.newHeapInstance(k);
    for (int i = 1; i <= k + 1; i++) { sk.update(i); }
    println(sk.toString(true, true));
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), k + 1);
    assertEquals(sk.getNumRetained(), 11);
    assertFalse(sk.isEmpty());
    assertTrue(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 33);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 3);
    assertEquals(sk.getMaxItem(), 21.0F);
    assertEquals(sk.getMinItem(), 1.0F);
    assertEquals(sk.getNumLevels(), 2);
    assertFalse(sk.isLevelZeroSorted());

    println("#### CASE: FLOAT HEAP EMPTY");
    sk = KllFloatsSketch.newHeapInstance(k);
    println(sk.toString(true, true));
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), 0);
    assertEquals(sk.getNumRetained(), 0);
    assertTrue(sk.isEmpty());
    assertFalse(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 20);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 2);
    try { sk.getMaxItem(); fail(); } catch (SketchesArgumentException e) { }
    try { sk.getMinItem(); fail(); } catch (SketchesArgumentException e) { }
    assertEquals(sk.getNumLevels(), 1);
    assertFalse(sk.isLevelZeroSorted());

    println("#### CASE: FLOAT HEAP SINGLE");
    sk = KllFloatsSketch.newHeapInstance(k);
    sk.update(1);
    println(sk.toString(true, true));
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), 1);
    assertEquals(sk.getNumRetained(), 1);
    assertFalse(sk.isEmpty());
    assertFalse(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 20);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 2);
    assertEquals(sk.getMaxItem(), 1.0F);
    assertEquals(sk.getMinItem(), 1.0F);
    assertEquals(sk.getNumLevels(), 1);
    assertFalse(sk.isLevelZeroSorted());
  }

  @Test //set static enablePrinting = true for visual checking
  public void checkSketchInitializeFloatHeapifyCompactMem() {
    int k = 20; //don't change this
    KllFloatsSketch sk;
    KllFloatsSketch sk2;
    byte[] compBytes;
    WritableMemory wmem;

    println("#### CASE: FLOAT FULL HEAPIFIED FROM COMPACT");
    sk2 = KllFloatsSketch.newHeapInstance(k);
    for (int i = 1; i <= k + 1; i++) { sk2.update(i); }
    //println(sk2.toString(true, true));
    compBytes = sk2.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes);
    println(KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true));
    sk = KllFloatsSketch.heapify(wmem);
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), k + 1);
    assertEquals(sk.getNumRetained(), 11);
    assertFalse(sk.isEmpty());
    assertTrue(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 33);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 3);
    assertEquals(sk.getMaxItem(), 21.0F);
    assertEquals(sk.getMinItem(), 1.0F);
    assertEquals(sk.getNumLevels(), 2);
    assertFalse(sk.isLevelZeroSorted());

    println("#### CASE: FLOAT EMPTY HEAPIFIED FROM COMPACT");
    sk2 = KllFloatsSketch.newHeapInstance(k);
    //println(sk.toString(true, true));
    compBytes = sk2.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes);
    //println(KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true));
    sk = KllFloatsSketch.heapify(wmem);
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), 0);
    assertEquals(sk.getNumRetained(), 0);
    assertTrue(sk.isEmpty());
    assertFalse(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 20);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 2);
    try { sk.getMaxItem(); fail(); } catch (SketchesArgumentException e) { }
    try { sk.getMinItem(); fail(); } catch (SketchesArgumentException e) { }
    assertEquals(sk.getNumLevels(), 1);
    assertFalse(sk.isLevelZeroSorted());

    println("#### CASE: FLOAT SINGLE HEAPIFIED FROM COMPACT");
    sk2 = KllFloatsSketch.newHeapInstance(k);
    sk2.update(1);
    //println(sk2.toString(true, true));
    compBytes = sk2.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes);
    //println(KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true));
    sk = KllFloatsSketch.heapify(wmem);
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), 1);
    assertEquals(sk.getNumRetained(), 1);
    assertFalse(sk.isEmpty());
    assertFalse(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 20);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 2);
    assertEquals(sk.getMaxItem(), 1.0F);
    assertEquals(sk.getMinItem(), 1.0F);
    assertEquals(sk.getNumLevels(), 1);
    assertFalse(sk.isLevelZeroSorted());
  }

  @Test //set static enablePrinting = true for visual checking
  public void checkSketchInitializeFloatHeapifyUpdatableMem() {
    int k = 20; //don't change this
    KllFloatsSketch sk;
    KllFloatsSketch sk2;
    byte[] compBytes;
    WritableMemory wmem;

    println("#### CASE: FLOAT FULL HEAPIFIED FROM UPDATABLE");
    sk2 = KllFloatsSketch.newHeapInstance(k);
    for (int i = 1; i <= k + 1; i++) { sk2.update(i); }
    //println(sk2.toString(true, true));
    compBytes = KllHelper.toByteArray(sk2, true);
    wmem = WritableMemory.writableWrap(compBytes);
    println(KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true));
    sk = KllHeapFloatsSketch.heapifyImpl(wmem);
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), k + 1);
    assertEquals(sk.getNumRetained(), 11);
    assertFalse(sk.isEmpty());
    assertTrue(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 33);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 3);
    assertEquals(sk.getMaxItem(), 21.0F);
    assertEquals(sk.getMinItem(), 1.0F);
    assertEquals(sk.getNumLevels(), 2);
    assertFalse(sk.isLevelZeroSorted());

    println("#### CASE: FLOAT EMPTY HEAPIFIED FROM UPDATABLE");
    sk2 = KllFloatsSketch.newHeapInstance(k);
    //println(sk.toString(true, true));
    compBytes = KllHelper.toByteArray(sk2, true);
    wmem = WritableMemory.writableWrap(compBytes);
    //println(KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true));
    sk = KllHeapFloatsSketch.heapifyImpl(wmem);
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), 0);
    assertEquals(sk.getNumRetained(), 0);
    assertTrue(sk.isEmpty());
    assertFalse(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 20);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 2);
    try { sk.getMaxItem(); fail(); } catch (SketchesArgumentException e) { }
    try { sk.getMinItem(); fail(); } catch (SketchesArgumentException e) { }
    assertEquals(sk.getNumLevels(), 1);
    assertFalse(sk.isLevelZeroSorted());

    println("#### CASE: FLOAT SINGLE HEAPIFIED FROM UPDATABLE");
    sk2 = KllFloatsSketch.newHeapInstance(k);
    sk2.update(1);
    //println(sk.toString(true, true));
    compBytes = KllHelper.toByteArray(sk2, true);
    wmem = WritableMemory.writableWrap(compBytes);
    //println(KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true));
    sk = KllHeapFloatsSketch.heapifyImpl(wmem);
    assertEquals(sk.getK(), k);
    assertEquals(sk.getN(), 1);
    assertEquals(sk.getNumRetained(), 1);
    assertFalse(sk.isEmpty());
    assertFalse(sk.isEstimationMode());
    assertEquals(sk.getMinK(), k);
    assertEquals(sk.getFloatItemsArray().length, 20);
    assertEquals(sk.getLevelsArray(sk.sketchStructure).length, 2);
    assertEquals(sk.getMaxItem(), 1.0F);
    assertEquals(sk.getMinItem(), 1.0F);
    assertEquals(sk.getNumLevels(), 1);
    assertFalse(sk.isLevelZeroSorted());
  }

  @Test //set static enablePrinting = true for visual checking
  public void checkMemoryToStringFloatCompact() {
    int k = 20; //don't change this
    KllFloatsSketch sk;
    KllFloatsSketch sk2;
    byte[] compBytes;
    byte[] compBytes2;
    WritableMemory wmem;
    String s;

    println("#### CASE: FLOAT FULL COMPACT");
    sk = KllFloatsSketch.newHeapInstance(k);
    for (int i = 1; i <= k + 1; i++) { sk.update(i); }
    compBytes = sk.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 1: sketch to byte[]/memory & analyze memory");
    println(s);
    sk2 = KllFloatsSketch.heapify(wmem);
    compBytes2 = sk2.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes2);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 2: memory to heap sketch, to byte[]/memory & analyze memory. Should match above");
    println(s);
    assertEquals(compBytes, compBytes2);

    println("#### CASE: FLOAT EMPTY COMPACT");
    sk = KllFloatsSketch.newHeapInstance(k);
    compBytes = sk.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 1: sketch to byte[]/memory & analyze memory");
    println(s);
    sk2 = KllFloatsSketch.heapify(wmem);
    compBytes2 = sk2.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes2);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 2: memory to heap sketch, to byte[]/memory & analyze memory. Should match above");
    println(s);
    assertEquals(compBytes, compBytes2);

    println("#### CASE: FLOAT SINGLE COMPACT");
    sk = KllFloatsSketch.newHeapInstance(k);
    sk.update(1);
    compBytes = sk.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 1: sketch to byte[]/memory & analyze memory");
    println(s);
    sk2 = KllFloatsSketch.heapify(wmem);
    compBytes2 = sk2.toByteArray();
    wmem = WritableMemory.writableWrap(compBytes2);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 2: memory to heap sketch, to byte[]/memory & analyze memory. Should match above");
    println(s);
    assertEquals(compBytes, compBytes2);
  }

  @Test //set static enablePrinting = true for visual checking
  public void checkMemoryToStringFloatUpdatable() {
    int k = 20; //don't change this
    KllFloatsSketch sk;
    KllFloatsSketch sk2;
    byte[] upBytes;
    byte[] upBytes2;
    WritableMemory wmem;
    String s;

    println("#### CASE: FLOAT FULL UPDATABLE");
    sk = KllFloatsSketch.newHeapInstance(20);
    for (int i = 1; i <= k + 1; i++) { sk.update(i); }
    upBytes = KllHelper.toByteArray(sk, true);
    wmem = WritableMemory.writableWrap(upBytes);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 1: sketch to byte[]/memory & analyze memory");
    println(s);
    sk2 = KllHeapFloatsSketch.heapifyImpl(wmem);
    upBytes2 = KllHelper.toByteArray(sk2, true);
    wmem = WritableMemory.writableWrap(upBytes2);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 2: memory to heap sketch, to byte[]/memory & analyze memory. Should match above");
    println(s); //note: heapify does not copy free space, while toUpdatableByteArray does
    assertEquals(sk.getN(), sk2.getN());
    assertEquals(sk.getMinItem(), sk2.getMinItem());
    assertEquals(sk.getMaxItem(), sk2.getMaxItem());
    assertEquals(sk.getNumRetained(), sk2.getNumRetained());

    println("#### CASE: FLOAT EMPTY UPDATABLE");
    sk = KllFloatsSketch.newHeapInstance(k);
    upBytes = KllHelper.toByteArray(sk, true);
    wmem = WritableMemory.writableWrap(upBytes);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 1: sketch to byte[]/memory & analyze memory");
    println(s);
    sk2 = KllHeapFloatsSketch.heapifyImpl(wmem);
    upBytes2 = KllHelper.toByteArray(sk2, true);
    wmem = WritableMemory.writableWrap(upBytes2);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 2: memory to heap sketch, to byte[]/memory & analyze memory. Should match above");
    println(s);
    assertEquals(upBytes, upBytes2);

    println("#### CASE: FLOAT SINGLE UPDATABLE");
    sk = KllFloatsSketch.newHeapInstance(k);
    sk.update(1);
    upBytes = KllHelper.toByteArray(sk, true);
    wmem = WritableMemory.writableWrap(upBytes);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 1: sketch to byte[]/memory & analyze memory");
    println(s);
    sk2 = KllHeapFloatsSketch.heapifyImpl(wmem);
    upBytes2 = KllHelper.toByteArray(sk2, true);
    wmem = WritableMemory.writableWrap(upBytes2);
    s = KllPreambleUtil.toString(wmem, FLOATS_SKETCH, true);
    println("step 2: memory to heap sketch, to byte[]/memory & analyze memory. Should match above");
    println(s);
    assertEquals(upBytes, upBytes2);
  }

  @Test
  public void checkSimpleMerge() {
    int k = 20;
    int m = 8;
    int n1 = 21;
    int n2 = 43;
    WritableMemory wmem = WritableMemory.allocate(3000);
    WritableMemory wmem2 = WritableMemory.allocate(3000);

    KllFloatsSketch sk1 = KllDirectFloatsSketch.newDirectUpdatableInstance(k, m, wmem, memReqSvr);
    KllFloatsSketch sk2 = KllDirectFloatsSketch.newDirectUpdatableInstance(k, m, wmem2, memReqSvr);
    for (int i = 1; i <= n1; i++) {
      sk1.update(i);
    }
    for (int i = 1; i <= n2; i++) {
      sk2.update(i + 100);
    }
    sk1.merge(sk2);
    assertEquals(sk1.getMinItem(), 1.0);
    assertEquals(sk1.getMaxItem(), 143.0);
  }

  @Test
  public void checkGetSingleItem() {
    int k = 20;
    KllFloatsSketch skHeap = KllFloatsSketch.newHeapInstance(k);
    skHeap.update(1);
    assertTrue(skHeap instanceof KllHeapFloatsSketch);
    assertEquals(skHeap.getFloatSingleItem(), 1.0F);

    WritableMemory srcMem = WritableMemory.writableWrap(KllHelper.toByteArray(skHeap, true));
    KllFloatsSketch skDirect = KllFloatsSketch.writableWrap(srcMem, memReqSvr);
    assertTrue(skDirect instanceof KllDirectFloatsSketch);
    assertEquals(skDirect.getFloatSingleItem(), 1.0F);

    Memory srcMem2 = Memory.wrap(skHeap.toByteArray());
    KllFloatsSketch skCompact = KllFloatsSketch.wrap(srcMem2);
    assertTrue(skCompact instanceof KllDirectCompactFloatsSketch);
    assertEquals(skCompact.getFloatSingleItem(), 1.0F);
  }

  @Test
  public void printlnTest() {
    String s = "PRINTING:  printf in " + this.getClass().getName();
    println(s);
    printf("%s\n", s);
  }

  private final static boolean enablePrinting = false;

  /**
   * @param format the format
   * @param args the args
   */
  private static final void printf(final String format, final Object ... args) {
    if (enablePrinting) { System.out.printf(format, args); }
  }

  /**
   * @param o the Object to println
   */
  private static final void println(final Object o) {
    if (enablePrinting) { System.out.println(o.toString()); }
  }

}
