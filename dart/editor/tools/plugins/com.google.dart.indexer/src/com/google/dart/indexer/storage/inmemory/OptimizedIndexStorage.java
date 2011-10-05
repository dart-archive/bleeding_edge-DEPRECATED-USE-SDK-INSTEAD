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
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;
import com.google.dart.indexer.index.entries.DependentEntity;
import com.google.dart.indexer.index.entries.DependentFileInfo;
import com.google.dart.indexer.index.entries.DependentLocation;
import com.google.dart.indexer.index.entries.FileInfo;
import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.entries.PathAndModStamp;
import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.locations.LocationPersitence;
import com.google.dart.indexer.storage.AbstractIntegratedStorage;
import com.google.dart.indexer.storage.inmemory.api.ILocationEncoder;
import com.google.dart.indexer.storage.inmemory.api.SimpleLocationInfoEncoder;
import com.google.dart.indexer.utils.PathUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Here all data is stored
 */
public class OptimizedIndexStorage extends AbstractIntegratedStorage implements ILocationEncoder {
  private static final boolean DEBUG = true;

  private static String getPortableString(IFile file) {
    return file.getFullPath().toString();
  }

  private SimpleLocationInfoEncoder locationInfoEncoder = new SimpleLocationInfoEncoder();
  private LocationInfoManager locationInfoPool = new LocationInfoManager(10,
      new OptimizedStringEncoder2());
  private FileInfoManager fileInfoPool = new FileInfoManager(1000, new PlainStringEncoder());

  private SimpleStringPool layerIds = new SimpleStringPool(1000);

  private int maxLocationsSize;
  private HashMap<String, ByteByteArray> layerMap = new HashMap<String, ByteByteArray>();
  private ByteByteArray fileInfos = new ByteByteArray(1000);
  private BitSet deletedLocations = new BitSet();

  private BitSet deletedFiles = new BitSet();

  private HashSet<IStorageChangeListener> listeners = new HashSet<IStorageChangeListener>();

  // OptimizedIndexStorage(LocationInfoManager lip, FileInfoManager fip,
  // SimpleStringPool lids, HashMap layers, int maxLocsSize,
  // ByteByteArray fInfos, BitSet deletedFiles2, BitSet deletedLocations2) {
  // setAllDatas(fip, lids, layers, maxLocsSize, fInfos, deletedFiles2,
  // deletedLocations2, lip);
  // StorageManager.registerStorage(this);
  // }

  public OptimizedIndexStorage(IndexConfigurationInstance configuration) {
    super(configuration);
    StorageManager.registerStorage(this);
  }

  public void addStorageChangeListener(IStorageChangeListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  @Override
  public Location decode(int position) {
    String string = locationInfoPool.getString(position);
    Location byUniqueIdentifier = LocationPersitence.getInstance().byUniqueIdentifier(string);
    return byUniqueIdentifier;
  }

  @Override
  public synchronized void deleteFileInfo(IFile file) {
    String portableString = getPortableString(file);
    int add = fileInfoPool.add(portableString);
    deletedFiles.set(add);
    int fileInfo = fileInfoPool.getFileInfo(add);
    if (fileInfo != 0) {
      fileInfos.data[fileInfo - 1] = null;
    }
    fireChanged();
  }

  @Override
  public synchronized void deleteLocationInfo(Location location) {
    String uniqueIdentifier = LocationPersitence.getInstance().getUniqueIdentifier(location);
    int bitIndex = locationInfoPool.get(uniqueIdentifier);
    if (bitIndex >= 0) {
      deletedLocations.set(bitIndex);
      Iterator<ByteByteArray> i = layerMap.values().iterator();
      int locationInfo2 = locationInfoPool.getLocationInfo(bitIndex);
      if (locationInfo2 != 0) {
        while (i.hasNext()) {
          ByteByteArray ba = i.next();
          ba.data[locationInfo2 - 1] = null;
        }
      }
      fireChanged();
    }
  }

  @Override
  public int encode(Location location) {
    int write = doWriteLocation(location, null, null);
    return write;
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj)) {
      return true;
    }
    int where = 0;
    OptimizedIndexStorage other = (OptimizedIndexStorage) obj;
    boolean result = other.maxLocationsSize == maxLocationsSize
        && ((++where > 0) && other.fileInfoPool.equals(this.fileInfoPool))
        && ((++where > 0) && other.fileInfos.equals(fileInfos))
        && ((++where > 0) && other.layerIds.equals(layerIds))

        /* && other.layerMap.equals(layerMap) */
        && ((++where > 0) && other.locationInfoPool.equals(locationInfoPool));

    if (!result && DEBUG) {
      // where may be used here
    }
    return result;
  }

  public BitSet getDeletedElementIds() {
    return deletedLocations;
  }

  public BitSet getDeletedFileIds() {
    return deletedFiles;
  }

  public FileInfoManager getFileInfoPool() {
    return this.fileInfoPool;
  }

  public ByteByteArray getFileInfos() {
    return fileInfos;
  }

  public SimpleStringPool getLayerIds() {
    return layerIds;
  }

  public HashMap<String, ByteByteArray> getLayersMap() {
    return layerMap;
  }

  public LocationInfoManager getLocationInfoPool() {
    return this.locationInfoPool;
  }

  public int getMaxLocationSize() {
    return maxLocationsSize;
  }

  @Override
  public synchronized Map<IFile, FileInfo> readAllFileInfos(IndexConfigurationInstance configuration) {
    HashMap<IFile, FileInfo> result = new HashMap<IFile, FileInfo>();
    for (int a = 0; a < fileInfoPool.ids.length; a++) {
      int id = fileInfoPool.ids[a];
      if (id != 0) {
        String str = fileInfoPool.getString(id);
        IPath path = PathUtils.fromPortableString(str);
        IFile dependentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        FileInfo fileInfo = readFileInfo(dependentFile);
        if (fileInfo != null) {
          result.put(dependentFile, fileInfo);
        }
      }
    }
    return result;
  }

  @Override
  public synchronized void readAllLayerLocationsInto(Map<Location, LocationInfo> locationInfos,
      Layer layer) {
    /* String layedId = */layer.getId().stringValue();
    boolean updated = true;
    for (int a = 0; a < locationInfoPool.ids.length; a++) {
      int id = locationInfoPool.ids[a];
      if (id != 0) {
        Location decode = decode(id);
        LocationInfo locationInfo = readLocationInfo(decode, layer);
        if (locationInfo != null) {
          Object object = locationInfos.get(decode);
          if (object != null) {
            if (!object.toString().equals(locationInfo.toString())) {
              readLocationInfo(decode, layer);
              throw new RuntimeException();
            }
          }
          locationInfos.put(decode, locationInfo);
          updated = true;
        }
      }
    }
    if (updated) {
      fireChanged();
    }
  }

  @Override
  public synchronized FileInfo readFileInfo(IFile file) {
    String portableString = getPortableString(file);
    int add = fileInfoPool.add(portableString);
    if (deletedFiles.get(add)) {
      return null;
    }
    int fileInfo = fileInfoPool.getFileInfo(add);
    if (fileInfo != 0) {
      byte[] bytes = fileInfos.data[fileInfo - 1];
      if (bytes != null) {
        return decodeInfo(bytes, configuration);
      }
    }
    return null;

  }

  @Override
  public synchronized PathAndModStamp[] readFileNamesAndStamps(
      HashSet<IFile> unprocessedExistingFiles) {
    ArrayList<PathAndModStamp> ls = new ArrayList<PathAndModStamp>();
    for (int a = 0; a < fileInfoPool.ids.length; a++) {
      int id = fileInfoPool.ids[a];
      if (id != 0) {
        String str = fileInfoPool.getString(id);
        int fileInfo = fileInfoPool.getFileInfo(str);
        if (fileInfo != 0) {
          byte[] bs = fileInfos.data[fileInfo - 1];
          if (bs != null) {
            long long1 = ByteBuffer.wrap(bs).getLong();
            ls.add(new PathAndModStamp(str, long1));
          }
        }
      }
    }
    return ls.toArray(new PathAndModStamp[ls.size()]);
  }

  @Override
  public synchronized LocationInfo readLocationInfo(Location location, Layer layer) {
    int locationInfo = locationInfoPool.getLocationInfo(LocationPersitence.getInstance().getUniqueIdentifier(
        location));
    if (locationInfo <= 0) {
      return null;
    }
    return readLocationInfo(locationInfo, layer.getId().stringValue());
  }

  public void removeStorageChangeListener(IStorageChangeListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  @Override
  public synchronized void writeFileInfo(IFile file, FileInfo info) {
    doWriteFile(file, info, configuration);
  }

  @Override
  public synchronized void writeLocationInfo(Location location, LocationInfo info, Layer layer) {
    try {
      doWriteLocation(location, info, layer.getId().stringValue());
    } catch (Exception e) {
      IndexerPlugin.getLogger().logError(e);
    }
  }

  synchronized FileInfo decodeInfo(byte[] array, IndexConfigurationInstance instance) {
    if (null == array) {
      throw new IllegalArgumentException("array is null");
    }
    ByteBuffer wrap = ByteBuffer.wrap(array);
    long long1 = wrap.getLong();
    ArrayList<Location> sourceLocations = new ArrayList<Location>();
    int sz = wrap.getInt();
    for (int a = 0; a < sz; a++) {
      int k = wrap.getInt();
      if (k != -1) {
        sourceLocations.add(decode(k));
      }
    }
    ArrayList<DependentEntity> internalDependencies = readCollection(wrap, instance);
    ArrayList<DependentEntity> eDependencies = readCollection(wrap, instance);
    FileInfo fileInfo = new FileInfo(sourceLocations, internalDependencies, eDependencies);
    fileInfo.setVersion(long1);
    return fileInfo;
  }

  private synchronized int doWriteFile(IFile file, FileInfo info, IndexConfigurationInstance config) {
    String portableString = getPortableString(file);
    int add = fileInfoPool.add(portableString);
    deletedFiles.clear(add);
    if (info == null) {
      info = new FileInfo();
    }
    ByteByteArray intBuf = fileInfos;
    int fileInfo = fileInfoPool.getFileInfo(add);
    if (fileInfo == 0) {
      fileInfo = fileInfos.elementsCount + 1;
      fileInfos.add(null);
    }
    long modificationStamp = file.getModificationStamp();
    byte[] encode = encodeInfo(modificationStamp, info);
    intBuf.data[fileInfo - 1] = encode;
    fileInfoPool.setFileInfo(add, fileInfo);

    fireChanged();

    return add;
  }

  private int doWriteLocation(Location location, LocationInfo info, String layerId) {
    String identifier = LocationPersitence.getInstance().getUniqueIdentifier(location);
    int toRet = locationInfoPool.add(identifier);
    deletedLocations.clear(toRet);
    if (info != null) {
      ByteByteArray intBuf = getLayerBuf(layerId);
      int locationInfo = locationInfoPool.getLocationInfo(toRet);
      if (locationInfo == 0) {
        locationInfo = expandLayers();
      }
      byte[] encode = locationInfoEncoder.encode(info, this);
      intBuf.data[locationInfo - 1] = encode;
      locationInfoPool.setLocationInfo(toRet, locationInfo);
    }
    fireChanged();
    return toRet;
  }

  private byte[] encodeInfo(long l, FileInfo info) {
    int size = 8;
    Collection<Location> sourceLocations = info.getSourceLocations();
    size += sourceLocations.size() * 4 + 4;
    Collection<DependentEntity> internalDependencies = info.getInternalDependencies();
    Collection<DependentEntity> eDependencies = info.getExternalDependencies();
    size += (internalDependencies.size() + eDependencies.size()) * 9;
    size += 8;
    byte[] result = new byte[size];
    ByteBuffer wrap = ByteBuffer.wrap(result);
    wrap.putLong(l);
    wrap.putInt(sourceLocations.size());
    Iterator<Location> i = sourceLocations.iterator();
    while (i.hasNext()) {
      Location la = i.next();
      int write = doWriteLocation(la, null, null);
      wrap.putInt(write);
    }
    writeCollection(wrap, internalDependencies);
    writeCollection(wrap, eDependencies);
    return result;
  }

  private int expandLayers() {
    int locationInfo;
    locationInfo = ++maxLocationsSize;
    Iterator<ByteByteArray> i = layerMap.values().iterator();
    while (i.hasNext()) {
      ByteByteArray ba = i.next();
      ba.add(null);
    }
    return locationInfo;
  }

  private void fireChanged() {
    synchronized (listeners) {
      Iterator<IStorageChangeListener> i = listeners.iterator();
      while (i.hasNext()) {
        IStorageChangeListener listener = i.next();
        listener.storageChanged(this);
      }
    }
  }

  private ByteByteArray getLayerBuf(String id) {
    ByteByteArray object = layerMap.get(id);
    if (object == null) {
      ByteByteArray ba = new ByteByteArray(maxLocationsSize * 2 + 1000);
      ba.elementsCount = maxLocationsSize;
      layerMap.put(id, ba);
      return ba;
    }
    return object;
  }

  private ArrayList<DependentEntity> readCollection(ByteBuffer wrap,
      IndexConfigurationInstance instance) {
    int sz = wrap.getInt();
    ArrayList<DependentEntity> result = new ArrayList<DependentEntity>();
    for (int a = 0; a < sz; a++) {
      int b = wrap.get();
      int layer = wrap.getInt();
      int wal = wrap.getInt();

      if (b == 0) {
        String string = layerIds.getString(layer);
        Layer layer2 = instance.getLayer(new LayerId(string));
        if (layer2 != null) {
          DependentLocation dependentLocation = new DependentLocation(decode(wal), layer2);
          result.add(dependentLocation);
        }
      } else {
        String string = fileInfoPool.getString(wal);
        IPath path = PathUtils.fromPortableString(string);
        IFile dependentFile = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        result.add(new DependentFileInfo(dependentFile));
      }
    }
    return result;
  }

  private LocationInfo readLocationInfo(int locationInfo, String id) {
    ByteByteArray intBuf = getLayerBuf(id);
    byte[] data = intBuf.data[locationInfo - 1];
    if (data == null) {
      return null;
    }
    LocationInfo decode = locationInfoEncoder.decode(data, this);
    return decode;
  }

  @SuppressWarnings("unused")
  private void setAllDatas(FileInfoManager fip, SimpleStringPool lids,
      HashMap<String, ByteByteArray> layers, int maxLocsSize, ByteByteArray fInfos,
      BitSet deletedFiles2, BitSet deletedLocations2, LocationInfoManager lip) {
    this.fileInfoPool = fip;
    this.fileInfos = fInfos;
    this.layerIds = lids;
    this.layerMap = layers;
    this.maxLocationsSize = maxLocsSize;
    this.deletedFiles = deletedFiles2;
    this.deletedLocations = deletedLocations2;
    this.locationInfoPool = lip;
  }

  private void writeCollection(ByteBuffer wrap, Collection<DependentEntity> internalDependencies) {
    Iterator<DependentEntity> i;
    wrap.putInt(internalDependencies.size());
    i = internalDependencies.iterator();
    while (i.hasNext()) {
      DependentEntity entity = i.next();
      if (entity instanceof DependentLocation) {
        wrap.put((byte) 0);
        DependentLocation l = (DependentLocation) entity;
        Layer dependentLayer = l.getDependentLayer();
        String string = dependentLayer.getId().toString();
        int add = layerIds.add(string);
        wrap.putInt(add);
        int write = doWriteLocation(l.getDependentLocation(), null, null);
        wrap.putInt(write);
      }
      if (entity instanceof DependentFileInfo) {
        wrap.put((byte) 1);
        wrap.putInt(0);
        DependentFileInfo l = (DependentFileInfo) entity;
        int write = doWriteFile(l.getFile(), null, null);
        wrap.putInt(write);
      }
    }
  }
}
