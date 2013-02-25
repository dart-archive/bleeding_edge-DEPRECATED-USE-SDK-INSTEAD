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
package com.google.dart.indexer.storage.inmemory.api;

import com.google.dart.indexer.index.entries.LocationInfo;
import com.google.dart.indexer.index.layers.bidirectional_edges.BidirectionalEdgesLocationInfo;
import com.google.dart.indexer.index.layers.reverse_edges.ReverseEdgesLocationInfo;
import com.google.dart.indexer.locations.Location;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SimpleLocationInfoEncoder implements ILocationInfoCoder {
  private static boolean isBitSet(byte bt, int num) {
    return (bt & ((byte) 1 << num)) != 0;
  }

  private static byte setBit(byte bt, int num) {
    return (byte) (bt | ((byte) 1 << num));
  }

  byte flags;

  @Override
  public LocationInfo decode(byte[] data, ILocationEncoder encoder) {
    ByteBuffer wrap = ByteBuffer.wrap(data);
    flags = wrap.get(0);
    boolean biderectionalLocations = isBitSet(flags, 0);
    if (biderectionalLocations) {
      Collection<Location> source = new ArrayList<Location>();
      Collection<Location> destination = new ArrayList<Location>();
      int decodeCollection = decodeCollection(wrap, encoder, 1, 1, source);
      decodeCollection(wrap, encoder, decodeCollection, 4, destination);
      if (source.isEmpty()) {
        source = Collections.emptyList();
      }
      if (destination.isEmpty()) {
        destination = Collections.emptyList();
      }
      BidirectionalEdgesLocationInfo bidirectionalEdgesLocationInfo = new BidirectionalEdgesLocationInfo(
          source, destination);
      return bidirectionalEdgesLocationInfo;
    } else {
      Collection<Location> source = new ArrayList<Location>();
      decodeCollection(wrap, encoder, 1, 1, source);
      if (source.isEmpty()) {
        source = Collections.emptyList();
      }
      ReverseEdgesLocationInfo re = new ReverseEdgesLocationInfo(source);
      return re;
    }
  }

  @Override
  public byte[] encode(LocationInfo info, ILocationEncoder locationCoder) {
    flags = 0;
    if (info instanceof BidirectionalEdgesLocationInfo) {
      flags |= 1;
      BidirectionalEdgesLocationInfo inf = (BidirectionalEdgesLocationInfo) info;
      Location[] sourceLocations = inf.getSourceLocations();
      Location[] deLocations = inf.getDestinationLocations();
      int sourceLocationsSize = calcLocationSize(sourceLocations, locationCoder, 1);
      int deLocationsSize = calcLocationSize(deLocations, locationCoder, 4);
      int mem = 1 + sourceLocationsSize + deLocationsSize;
      byte[] result = new byte[mem];
      ByteBuffer wrap = ByteBuffer.wrap(result);
      wrap.put(0, flags);
      int pos = 1;
      int index = 1;
      pos = encodeCollection(locationCoder, sourceLocations, wrap, 1, index);
      pos = encodeCollection(locationCoder, deLocations, wrap, pos, 4);
      return result;
    }
    if (info instanceof ReverseEdgesLocationInfo) {
      ReverseEdgesLocationInfo inf = (ReverseEdgesLocationInfo) info;
      Location[] sourceLocations = inf.getSourceLocations();
      int mem = 1 + calcLocationSize(sourceLocations, locationCoder, 1);
      byte[] result = new byte[mem];
      ByteBuffer wrap = ByteBuffer.wrap(result);

      wrap.put(0, flags);
      int pos = 1;
      int index = 1;
      pos = encodeCollection(locationCoder, sourceLocations, wrap, pos, index);
      pos++;
      return result;
    }
    return null;
  }

  /*
   * private static class IncrementalBufferWriter { private final ByteBuffer buffer; private int
   * pos;
   * 
   * public IncrementalBufferWriter(ByteBuffer buff, int pos) { this.buffer = buff; this.pos = pos;
   * }
   * 
   * public void write(char ch) { buffer.putChar(pos, ch); pos += 2; }
   * 
   * public void write(int i) { buffer.putInt(pos, i); pos += 4; }
   * 
   * public int getPos() { return pos; } }
   * 
   * private int encodeCollection(ILocationEncoder locationCoder, Location[] sourceLocations,
   * ByteBuffer wrap, int pos, int index) { boolean shortS = isBitSet(flags, index); boolean zero =
   * isBitSet(flags, index + 1); boolean smallLength = isBitSet(flags, index + 2); boolean one =
   * zero & smallLength;
   * 
   * IncrementalBufferWriter writer = new IncrementalBufferWriter(wrap, pos);
   * 
   * if (one) { int encode = locationCoder.encode(sourceLocations[0]); writer.write( shortS ? (char)
   * encode : encode); } else if (!zero) { int length = sourceLocations.length; writer.write(
   * smallLength ? (char) length : length); for (int a = 0; a < sourceLocations.length; a++) { int
   * encode = locationCoder.encode(sourceLocations[a]); writer.write(shortS ?(char) encode : encode
   * ); } } return writer.getPos(); }
   */

  int decodeCollection(ByteBuffer buffer, ILocationEncoder encoder, int pos, int index,
      Collection<Location> result) {
    boolean shortS = isBitSet(flags, index);
    boolean zero = isBitSet(flags, index + 1);
    boolean smallLength = isBitSet(flags, index + 2);
    boolean one = zero & smallLength;

    if (one) {
      if (shortS) {
        Location decode = encoder.decode(buffer.getChar(pos));
        result.add(decode);
        pos += 2;
      } else {
        Location decode = encoder.decode(buffer.getInt(pos));
        result.add(decode);
        pos += 4;
      }
    } else if (!zero) {
      if (smallLength) {
        int length = buffer.getChar(pos);
        pos += 2;

        if (shortS) {
          for (int a = 0; a < length; a++) {
            int location = buffer.getChar(pos);
            result.add(encoder.decode(location));
            pos += 2;
          }
        } else {
          for (int a = 0; a < length; a++) {
            int location = buffer.getInt(pos);
            result.add(encoder.decode(location));
            pos += 4;
          }
        }
      } else {
        int length = buffer.getChar(pos);
        pos += 4;
        if (shortS) {
          for (int a = 0; a < length; a++) {
            int location = buffer.getChar(pos);
            result.add(encoder.decode(location));
            pos += 2;
          }
        } else {
          for (int a = 0; a < length; a++) {
            int location = buffer.getInt(pos);
            result.add(encoder.decode(location));
            pos += 4;
          }
        }
      }
    }
    return pos;
  }

  private int calcLocationSize(Location[] sourceLocations, ILocationEncoder locationCoder, int index) {
    int vla = 2;
    for (int a = 0; a < sourceLocations.length; a++) {
      int encode = locationCoder.encode(sourceLocations[a]);
      if (encode > 65535) {
        vla = 4;
        break;
      }
    }
    if (vla == 2) {
      flags = setBit(flags, index);
    }
    if (sourceLocations.length < 65535) {
      if (sourceLocations.length == 0) {
        flags = setBit(flags, index + 1);
        return 0;
      } else if (sourceLocations.length == 1) {
        flags = setBit(flags, index + 1);
        flags = setBit(flags, index + 2);
        return vla;
      } else {
        flags = setBit(flags, index + 2);
        return 2 + sourceLocations.length * vla;
      }
    }
    return 4 + sourceLocations.length * vla;
  }

  private int encodeCollection(ILocationEncoder locationCoder, Location[] sourceLocations,
      ByteBuffer wrap, int pos, int index) {
    boolean shortS = isBitSet(flags, index);
    boolean zero = isBitSet(flags, index + 1);
    boolean smallLength = isBitSet(flags, index + 2);
    boolean one = zero & smallLength;

    if (one) {
      if (shortS) {
        int encode = locationCoder.encode(sourceLocations[0]);
        wrap.putChar(pos, (char) encode);
        pos += 2;
      } else {
        int encode = locationCoder.encode(sourceLocations[0]);
        wrap.putInt(pos, encode);
        pos += 4;
      }
    } else if (!zero) {
      if (smallLength) {
        int length = sourceLocations.length;
        wrap.putChar(pos, (char) length);
        pos += 2;
        if (shortS) {
          for (int a = 0; a < sourceLocations.length; a++) {
            wrap.putChar(pos, (char) locationCoder.encode(sourceLocations[a]));
            pos += 2;
          }
        } else {
          for (int a = 0; a < sourceLocations.length; a++) {
            wrap.putInt(pos, locationCoder.encode(sourceLocations[a]));
            pos += 4;
          }
        }
      } else {
        int length = sourceLocations.length;
        wrap.putInt(pos, length);
        pos += 4;
        if (shortS) {
          for (int a = 0; a < sourceLocations.length; a++) {
            wrap.putChar(pos, (char) locationCoder.encode(sourceLocations[a]));
            pos += 2;
          }
        } else {
          for (int a = 0; a < sourceLocations.length; a++) {
            wrap.putInt(pos, locationCoder.encode(sourceLocations[a]));
            pos += 4;
          }
        }
      }
    }
    return pos;
  }
}
