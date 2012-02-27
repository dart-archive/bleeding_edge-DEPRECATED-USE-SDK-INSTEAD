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
                                     timeZone.isUtc) {
    _asJs();
  }

  DateImplementation.now()
      : timeZone = new TimeZone.local(),
        value = _now() {
    _asJs();
  }

  factory DateImplementation.fromString(String formattedString) {
    // JavaScript's parse function is not specified and there are differences
    // between the different implementations. Make sure we can at least read in
    // Dart's output: try to read in (a subset of) ISO 8601 first. If that fails
    // fall back to JavaScript's implementation.
    final RegExp re =
        const RegExp(@'^([+-]?\d?\d\d\d\d)-?(\d\d)-?(\d\d) (\d\d):(\d\d):(\d\d)(?:.(\d{1,3}))? ?([zZ]?)$');
    Match match = re.firstMatch(formattedString);
    if (match !== null) {
      int years = Math.parseInt(match[1]);
      int month = Math.parseInt(match[2]);
      int day = Math.parseInt(match[3]);
      int hours = Math.parseInt(match[4]);
      int minutes = Math.parseInt(match[5]);
      int seconds = Math.parseInt(match[6]);
      int milliseconds = 0;
      if (match[7] !== null) {
        milliseconds = Math.parseInt(match[7]);
        if (match[7].length == 1) {
          milliseconds *= 100;
        } else if (match[7].length == 2) {
          milliseconds *= 10;
        } else {
          assert(match[7].length == 3);
        }
      }
      bool isUtc = match[8] !== null;
      TimeZone timezone = isUtc ? const TimeZone.utc() : new TimeZone.local();
      return new DateImplementation.withTimeZone(
          years, month, day, hours, minutes, seconds, milliseconds, timezone);
    } else {
      return new DateImplementation.fromEpoch(formattedString,
                                              new TimeZone.local());
    }
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
  '''return this.isUtc() ? this._asJs().getUTCFullYear() :
    this._asJs().getFullYear();''' {
    isUtc();
    _asJs();
  }

  int get month() native
  '''return this.isUtc() ? this._asJs().getUTCMonth() + 1 :
      this._asJs().getMonth() + 1;'''  {
    isUtc();
    _asJs();
  }

  int get day() native
  '''return this.isUtc() ? this._asJs().getUTCDate() :
      this._asJs().getDate();''' {
    isUtc();
    _asJs();
  }

  int get hours() native
  '''return this.isUtc() ? this._asJs().getUTCHours() :
      this._asJs().getHours();''' {
    isUtc();
    _asJs();
  }

  int get minutes() native
  '''return this.isUtc() ? this._asJs().getUTCMinutes() :
      this._asJs().getMinutes();''' {
    isUtc();
    _asJs();
  }

  int get seconds() native
  '''return this.isUtc() ? this._asJs().getUTCSeconds() :
      this._asJs().getSeconds();''' {
    isUtc();
    _asJs();
  }

  int get milliseconds() native
  '''return this.isUtc() ? this._asJs().getUTCMilliseconds() :
    this._asJs().getMilliseconds();''' {
    isUtc();
    _asJs();
  }

  // Adjust by one because JS weeks start on Sunday.
  int get weekday() native '''
    var day = this.isUtc() ? this._asJs().getUTCDay() : this._asJs().getDay();
    return (day + 6) % 7;''';

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
