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
package com.google.dart.indexer.pagedstorage.stats;

import java.text.DecimalFormat;

public class NumericDistribution {
  static class Computed {
    final double avg;
    final double s;

    public Computed(double avg, double s) {
      this.avg = avg;
      this.s = s;
    }
  }

  private double min, max, sum1, sum2, sum3;
  private int count;
  private transient Computed computed;

  public void add(double value) {
    computed = null;
    if (value < min || count == 0) {
      min = value;
    }
    if (value > max || count == 0) {
      max = value;
    }
    sum1 += value;
    sum2 += value * value;
    sum3 += value * value * value;
    ++count;
  }

  public void addAll(NumericDistribution other) {
    computed = null;
    min = (count > 0 ? Math.min(min, other.min) : other.min);
    max = (count > 0 ? Math.max(max, other.max) : other.max);
    sum1 += other.sum1;
    sum2 += other.sum2;
    sum3 += other.sum3;
    count += other.count;
  }

  public double average() {
    return computed().avg;
  }

  public double deviation() {
    return computed().s;
  }

  public String toDetailedString() {
    computed();
    DecimalFormat f = new DecimalFormat("##0.0");
    return "<" + f.format(computed.avg) + " ±" + f.format(computed.s) + " (" + f.format(min) + ".."
        + f.format(max) + ")>";
  }

  @Override
  public String toString() {
    computed();
    DecimalFormat f = new DecimalFormat("##0.0");
    return f.format(computed.avg) + "±" + f.format(computed.s);
  }

  private Computed computed() {
    if (computed == null) {
      double avg = sum1 / count;
      double s2 = sum2 / count - avg * avg;
      double s = Math.sqrt(s2);
      computed = new Computed(avg, s);
    }
    return computed;
  }
}
