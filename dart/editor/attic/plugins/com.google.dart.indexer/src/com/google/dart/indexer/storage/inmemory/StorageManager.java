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
package com.google.dart.indexer.storage.inmemory;

import com.google.dart.indexer.IndexerPlugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class StorageManager {
  private static final long MAGIC = 0xCAFEBABEL;

  protected static final long FLUSH_TIMEOUT = 180000;

  private static Thread storagePersistenceManager = new Thread("Indexer State Saver") {

    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(FLUSH_TIMEOUT);
        } catch (InterruptedException e) {
          IndexerPlugin.getLogger().logError(e);
        }
        saveIfNeeded();
      }
    }

  };

  static HashSet<OptimizedIndexStorage> changed = new HashSet<OptimizedIndexStorage>();

  private static File wholeIndexFileStorage;

  private static File tmpFile;

  static {
    storagePersistenceManager.setDaemon(true);
    storagePersistenceManager.start();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        saveIfNeeded();
      }
    });
  }

  public static OptimizedIndexStorage loadFromCache() throws FileNotFoundException, IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(
        wholeIndexFileStorage)));
    try {
      OptimizedIndexStorage storage = loadOptimizedIndexStorage(in);
      // registerStorage(storage);
      return storage;
    } finally {
      in.close();
    }
  }

  public static void setTargetFile(File file) {
    wholeIndexFileStorage = file;
  }

  static void registerStorage(OptimizedIndexStorage storage) {
    storage.addStorageChangeListener(new IStorageChangeListener() {

      @Override
      public void storageChanged(OptimizedIndexStorage storage) {
        synchronized (changed) {
          changed.add(storage);
        }
        // saveIfNeeded();
      }

    });
  }

  private static void checkMagic(DataInputStream in) throws IOException {
    if (MAGIC != in.readLong()) {
      throw new RuntimeException("invalid magic!");
    }

  }

  private static File getTargetIndexFile() {
    if (wholeIndexFileStorage == null) {
      wholeIndexFileStorage = new File("c:\\test.indx");
    }
    wholeIndexFileStorage.delete();
    return wholeIndexFileStorage;
  }

  private static File getTemporaryFile(OptimizedIndexStorage ip) throws IOException {
    if (tmpFile == null) {
      tmpFile = File.createTempFile("indexer", null);
      tmpFile.deleteOnExit();
    }
    return tmpFile;
  }

  private static BitSet loadBitSet(DataInputStream in) throws IOException {
    int cardinality = in.readInt();
    BitSet result = new BitSet();
    while (cardinality-- != 0) {
      result.set(in.readInt());
    }
    return result;
  }

  private static ByteArray loadByteArray(DataInputStream in) throws IOException {
    int size = in.readInt();
    byte[] data = new byte[size];
    in.readFully(data);
    AbstractStringEncoder e = null;
    int x = in.read();
    if (x == 1) {
      e = new PlainStringEncoder();
    } else if (x == 2) {
      e = new OptimizedStringEncoder2(loadSimpleStringPool(in));
    } else {
      throw new IllegalStateException();
    }
    return new ByteArray(size, data, e);
  }

  private static FileInfoManager loadFileInfoManager(DataInputStream in) throws IOException {
    int size = in.readInt();
    int[] ids = readInts(in);
    IntArray locInfos = loadIntArray(in);
    IntArray poss = loadIntArray(in);
    ByteArray array = loadByteArray(in);
    checkMagic(in);

    return new FileInfoManager(size, ids, locInfos, poss, array, new PlainStringEncoder());
  }

  private static IntArray loadIntArray(DataInputStream in) throws IOException {
    return new IntArray(readInts(in));
  }

  private static HashMap<String, ByteByteArray> loadLayersMap(DataInputStream in)
      throws IOException {
    int size = in.readInt();
    HashMap<String, ByteByteArray> result = new HashMap<String, ByteByteArray>();
    for (int i = 0; i < size; i++) {
      String key = in.readUTF();
      ByteByteArray value = new ByteByteArray(in);
      result.put(key, value);
    }
    checkMagic(in);
    return result;
  }

  private static LocationInfoManager loadLocationInfoManager(DataInputStream in) throws IOException {
    int size = in.readInt();
    int[] ids = readInts(in);
    IntArray locInfos = loadIntArray(in);
    IntArray poss = loadIntArray(in);
    ByteArray array = loadByteArray(in);
    checkMagic(in);

    return new LocationInfoManager(size, ids, locInfos, poss, array, new OptimizedStringEncoder2());
  }

  private static int loadMaxLocationsSize(DataInputStream in) throws IOException {
    return in.readInt();
  }

  private static OptimizedIndexStorage loadOptimizedIndexStorage(DataInputStream in)
      throws IOException {
    /* int maxLoc = */loadMaxLocationsSize(in);
    /* SimpleStringPool layerIds = */loadSimpleStringPool(in);

    /* LocationInfoManager locInfoMananger = */loadLocationInfoManager(in);
    /* FileInfoManager fileInfoManager = */loadFileInfoManager(in);
    /* ByteByteArray fileInfos = */new ByteByteArray(in);
    /* HashMap<String, ByteByteArray> layers = */loadLayersMap(in);

    /* BitSet deletedFiles = */loadBitSet(in);
    /* BitSet deletedLocations = */loadBitSet(in);
    checkMagic(in);

    throw new AssertionError("unused method, disabled");

    // return new OptimizedIndexStorage(locInfoMananger, fileInfoManager,
    // layerIds, layers, maxLoc, fileInfos, deletedFiles,
    // deletedLocations);
  }

  private static SimpleStringPool loadSimpleStringPool(DataInputStream in) throws IOException {
    int size = in.readInt();
    int[] ids = readInts(in);
    IntArray intArray = loadIntArray(in);
    ByteArray byteArray = loadByteArray(in);
    checkMagic(in);
    return new SimpleStringPool(size, ids, intArray, byteArray);
  }

  private static int[] readInts(DataInputStream in) throws IOException {
    int[] result = new int[in.readInt()];
    for (int i = 0; i < result.length; i++) {
      result[i] = in.readInt();
    }
    checkMagic(in);
    return result;
  }

  private static boolean saveAllDeletionMarks(BitSet deletedFileIds, BitSet deletedElementIds,
      DataOutputStream out) {
    try {
      saveBitSet(deletedFileIds, out);
      saveBitSet(deletedElementIds, out);
      writeMagic(out);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean saveBitSet(BitSet bs, DataOutputStream out) {
    try {
      int elements = bs.cardinality();
      out.writeInt(elements);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        out.writeInt(i);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static void saveByteArray(ByteArray array, DataOutputStream out) throws IOException {
    out.writeInt(array.elementsCount);
    out.write(array.data, 0, array.elementsCount);
    if (array.encoder instanceof PlainStringEncoder) {
      out.write(1);
    } else if (array.encoder instanceof OptimizedStringEncoder2) {
      out.write(2);
      OptimizedStringEncoder2 e = (OptimizedStringEncoder2) array.encoder;
      saveSimpleStringPool(e.packagePool, out);
    } else {
      throw new RuntimeException();
    }
  }

  private static boolean saveFileInfoManager(FileInfoManager fileInfoPool, DataOutputStream out) {
    try {
      out.writeInt(fileInfoPool.size);
      writeInts(fileInfoPool.ids, out);
      saveIntArray(fileInfoPool.locationInfos, out);
      saveIntArray(fileInfoPool.positions, out);
      saveByteArray(fileInfoPool.array, out);
      writeMagic(out);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean saveFileInfos(ByteByteArray bbarray, DataOutputStream out) {
    try {
      bbarray.store(out);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static void saveIfNeeded() {
    HashSet<OptimizedIndexStorage> toSave = new HashSet<OptimizedIndexStorage>();
    synchronized (changed) {
      toSave.addAll(changed);
      changed.clear();
    }
    Iterator<OptimizedIndexStorage> i = toSave.iterator();
    while (i.hasNext()) {
      OptimizedIndexStorage ip = i.next();

      synchronized (ip) {

        try {
          File fls = getTemporaryFile(ip);
          DataOutputStream out = null;
          try {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fls)));
            if (saveOptimizedStorage(ip, out)) {
              out.close();

              File targetFile = getTargetIndexFile();
              if (!fls.renameTo(targetFile)) {
                targetFile.delete();

                fls.renameTo(targetFile);

              } else {
              }
            }
          } finally {
            if (null != out) {
              out.close();
            }
          }

        } catch (IOException exception) {
          IndexerPlugin.getLogger().logError(exception);
        }
      }
    }
  }

  private static void saveIntArray(IntArray array, DataOutputStream out) throws IOException {
    writeInts(array.toArray(), out);

  }

  private static boolean saveLayersMap(HashMap<String, ByteByteArray> map, DataOutputStream out) {
    try {
      int elements = map.size();
      out.writeInt(elements);
      for (Iterator<Map.Entry<String, ByteByteArray>> i = map.entrySet().iterator(); i.hasNext();) {
        Entry<String, ByteByteArray> entry = i.next();
        out.writeUTF(entry.getKey());
        entry.getValue().store(out);
      }
      writeMagic(out);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean saveLocationInfoManager(LocationInfoManager manager, DataOutputStream out) {
    try {
      out.writeInt(manager.size);
      writeInts(manager.ids, out);
      saveIntArray(manager.locationInfos, out);
      saveIntArray(manager.positions, out);
      saveByteArray(manager.array, out);
      writeMagic(out);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean saveMaxLocationsSize(int maxLocationSize, DataOutputStream out) {
    try {
      out.writeInt(maxLocationSize);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean saveOptimizedStorage(OptimizedIndexStorage ip, DataOutputStream out) {

    return saveMaxLocationsSize(ip.getMaxLocationSize(), out)
        && saveSimpleStringPool(ip.getLayerIds(), out)
        && saveLocationInfoManager(ip.getLocationInfoPool(), out)
        && saveFileInfoManager(ip.getFileInfoPool(), out) && saveFileInfos(ip.getFileInfos(), out)
        && saveLayersMap(ip.getLayersMap(), out)
        && saveAllDeletionMarks(ip.getDeletedFileIds(), ip.getDeletedElementIds(), out);
  }

  private static boolean saveSimpleStringPool(SimpleStringPool stringsPool, DataOutputStream out) {
    try {
      out.writeInt(stringsPool.size);
      writeInts(stringsPool.ids, out);
      saveIntArray(stringsPool.positions, out);
      saveByteArray(stringsPool.array, out);
      writeMagic(out);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static void writeInts(int[] ints, DataOutputStream out) throws IOException {
    out.writeInt(ints.length);
    for (int i = 0; i < ints.length; i++) {
      out.writeInt(ints[i]);
    }
    writeMagic(out);
  }

  private static void writeMagic(DataOutputStream out) throws IOException {
    out.writeLong(MAGIC);
  }

  private StorageManager() {
  }
}
