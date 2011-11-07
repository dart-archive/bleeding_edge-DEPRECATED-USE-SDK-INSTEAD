// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(jmesserly): the native class should be the real JS Date.
// TODO(jimhug): Making the date value non-lazy might be a good path there.
class DateImplementation implements Date {
  final int value;
  final TimeZoneImplementation timeZone;

  factory DateImplementation(int years,
                             int month,
                             int day,
                             int hours,
                             int minutes,
                             int seconds,
                             int milliseconds) {
    return new DateImplementation.withTimeZone(
        years, month, day,
        hours, minutes, seconds, milliseconds,
        new TimeZoneImplementation.local());
  }

  DateImplementation.withTimeZone(int years,
                                  int month,
                                  int day,
                                  int hours,
                                  int minutes,
                                  int seconds,
                                  int milliseconds,
                                  TimeZoneImplementation timeZone)
      : this.timeZone = timeZone,
        value = _valueFromDecomposed(years, month, day,
                                     hours, minutes, seconds, milliseconds,
                                     timeZone.isUtc) { _asJs();
  }

  DateImplementation.now()
      : timeZone = new TimeZone.local(),
        value = _now() { _asJs();
  }

  DateImplementation.fromString(String formattedString)
      : timeZone = new TimeZone.local(),
        value = _valueFromString(formattedString) { _asJs();
  }

  const DateImplementation.fromEpoch(this.value, this.timeZone);

  bool operator ==(other) {
    if (!(other is DateImplementation)) return false;
    return (value == other.value) && (timeZone == other.timeZone);
  }

  int compareTo(Date other) {
    return value.compareTo(other.value);
  }

  Date changeTimeZone(TimeZone targetTimeZone) {
    if (targetTimeZone == null) {
      targetTimeZone = new TimeZoneImplementation.local();
    }
    return new Date.fromEpoch(value, targetTimeZone);
  }

  int get year() native
  '''return this.isUtc ? this._asJs().getUTCFullYear() :
    this._asJs().getFullYear();''';

  int get month() native
  '''return this.isUtc ? this._asJs().getMonth() + 1 :
      this._asJs().getMonth() + 1;''';

  int get day() native
    'return this.isUtc ? this._asJs().getUTCDate() : this._asJs().getDate()';

  int get hours() native
    'return this.isUtc ? this._asJs().getUTCHours() : this._asJs().getHours()';

  int get minutes() native
    'return this.isUtc ? this._asJs().getUTCMinutes() : this._asJs().getMinutes()';

  int get seconds() native
    'return this.isUtc ? this._asJs().getUTCSeconds() : this._asJs().getSeconds()';

  int get milliseconds() native
  '''return this.isUtc ? this._asJs().getUTCMilliseconds() :
    this._asJs().getMilliseconds();''';

  int get weekday() {
    final Date unixTimeStart =
        new Date.withTimeZone(1970, 1, 1, 0, 0, 0, 0, timeZone);
    int msSince1970 = this.difference(unixTimeStart).inMilliseconds;
    // Adjust the milliseconds to avoid problems with summer-time.
    if (hours < 2) {
      msSince1970 += 2 * Duration.MILLISECONDS_PER_HOUR;
    }
    int daysSince1970 =
        (msSince1970 / Duration.MILLISECONDS_PER_DAY).floor().toInt();
    // 1970-1-1 was a Thursday.
    return ((daysSince1970 + Date.THU) % Date.DAYS_IN_WEEK);
  }

  // TODO(jimhug): Could this please be getters?
  bool isLocalTime() {
    return !timeZone.isUtc;
  }

  bool isUtc() {
    return timeZone.isUtc;
  }

  String toString() {
    String threeDigits(int n) {
      if (n >= 100) return "${n}";
      if (n > 10) return "0${n}";
      return "00${n}";
    }
    String twoDigits(int n) {
      if (n >= 10) return "${n}";
      return "0${n}";
    }

    String m = twoDigits(month);
    String d = twoDigits(day);
    String h = twoDigits(hours);
    String min = twoDigits(minutes);
    String sec = twoDigits(seconds);
    String ms = threeDigits(milliseconds);
    if (timeZone.isUtc) {
      return "$year-$m-$d $h:$min:$sec.${ms}Z";
    } else {
      return "$year-$m-$d $h:$min:$sec.$ms";
    }
  }

  // TODO(jimhug): Why not use operators here?
    // Adds the [duration] to this Date instance.
  Date add(Duration duration) {
    return new DateImplementation.fromEpoch(value + duration.inMilliseconds,
                                            timeZone);
  }

  // Subtracts the [duration] from this Date instance.
  Date subtract(Duration duration) {
    return new DateImplementation.fromEpoch(value - duration.inMilliseconds,
                                            timeZone);
  }

  // Returns a [Duration] with the difference of [this] and [other].
  Duration difference(Date other) {
    return new Duration(milliseconds: value - other.value);
  }

  // TODO(floitsch): Use real exception object.
  static int _valueFromDecomposed(int years, int month, int day,
                                  int hours, int minutes, int seconds,
                                  int milliseconds, bool isUtc) native
  '''var jsMonth = month - 1;
  var value = isUtc ?
    Date.UTC(years, jsMonth, day,
             hours, minutes, seconds, milliseconds) :
    new Date(years, jsMonth, day,
             hours, minutes, seconds, milliseconds).valueOf();
  if (isNaN(value)) throw Error("Invalid Date");
  return value;''';

  static int _valueFromString(String str) native
  '''var value = Date.parse(str);
  if (isNaN(value)) throw Error("Invalid Date");
  return value;''';

  static int _now() native "return new Date().valueOf();";

  // Lazily keep a JS Date stored in the dart object.
  var _asJs() native '''
  if (!this.date) {
    this.date = new Date(this.value);
  }
  return this.date;''';
}

// Trivial implementation of TimeZone
class TimeZoneImplementation implements TimeZone {
  const TimeZoneImplementation.utc() : this.isUtc = true;
  const TimeZoneImplementation.local() : this.isUtc = false;

  bool operator ==(other) {
    if (!(other is TimeZoneImplementation)) return false;
    return isUtc == other.isUtc;
  }

  String toString() {
    if (isUtc) return "TimeZone (UTC)";
    return "TimeZone (Local)";
  }

  final bool isUtc;
}
