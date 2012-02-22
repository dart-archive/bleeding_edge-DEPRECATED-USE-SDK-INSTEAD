// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Mocks of classes and interfaces that Leg cannot read directly.

// TODO(ahe): Remove this file.

class JSSyntaxRegExp implements RegExp {
  JSSyntaxRegExp(String pattern,
                 [bool multiLine = false,
                  bool ignoreCase = false]) {
    throw 'JSSyntaxRegExp is not implemented';
  }
}

class ReceivePortFactory {
  factory ReceivePort() {
    throw 'factory ReceivePort is not implemented';
  }

  factory ReceivePort.singleShot() {
    throw 'factory ReceivePort.singleShot is not implemented';
  }
}

class StringBase {
  static String createFromCharCodes(List<int> charCodes) {
    checkNull(charCodes);
    if (!isJSArray(charCodes)) {
      if (charCodes is !List) throw new IllegalArgumentException(charCodes);
      charCodes = new List.from(charCodes);
    }
    return Primitives.stringFromCharCodes(charCodes);
  }

  static String join(List<String> strings, String separator) {
    checkNull(strings);
    checkNull(separator);
    var result = "";
    var first = true;
    for (var string in strings) {
      checkNull(string);
      if (string is !String) throw new IllegalArgumentException(string);
      if (!first) result += separator; // TODO(ahe): Use string buffer.
      result += string; // TODO(ahe): Use string buffer.
      first = false;
    }
    return result;
  }

  static String concatAll(List<String> strings) {
    checkNull(strings);
    var result = "";
    for (var string in strings) {
      checkNull(string);
      if (string is !String) throw new IllegalArgumentException(string);
      result += string; // TODO(ahe): Use string buffer.
    }
    return result;
  }
}

class TimeZoneImplementation implements TimeZone {
  const TimeZoneImplementation.utc() : isUtc = true;
  TimeZoneImplementation.local() : isUtc = false;

  bool operator ==(Object other) {
    if (!(other is TimeZoneImplementation)) return false;
    return isUtc == other.isUtc;
  }

  final bool isUtc;
}

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
      : this.timeZone = checkNull(timeZone),
        value = Primitives.valueFromDecomposedDate(years, month, day,
                                                   hours, minutes, seconds,
                                                   milliseconds,
                                                   timeZone.isUtc) {
    _asJs();
  }

  DateImplementation.now()
      : timeZone = new TimeZone.local(),
        value = Primitives.dateNow() {
    _asJs();
  }

  DateImplementation.fromString(String formattedString)
      : timeZone = new TimeZone.local(),
        value = Primitives.valueFromDateString(formattedString) {
    _asJs();
  }

  const DateImplementation.fromEpoch(this.value, this.timeZone);

  bool operator ==(other) {
    if (!(other is DateImplementation)) return false;
    return (value == other.value) && (timeZone == other.timeZone);
  }

  int compareTo(Date other) {
    checkNull(other);
    return value.compareTo(other.value);
  }

  Date changeTimeZone(TimeZone targetTimeZone) {
    if (targetTimeZone == null) {
      targetTimeZone = new TimeZoneImplementation.local();
    }
    return new Date.fromEpoch(value, targetTimeZone);
  }

  int get year() => Primitives.getYear(this);

  int get month() => Primitives.getMonth(this);

  int get day() => Primitives.getDay(this);

  int get hours() => Primitives.getHours(this);

  int get minutes() => Primitives.getMinutes(this);

  int get seconds() => Primitives.getSeconds(this);

  int get milliseconds() => Primitives.getMilliseconds(this);

  int get weekday() {
    // Adjust by one because JS weeks start on Sunday.
    var day = Primitives.getWeekday(this);
    return (day + 6) % 7;
  }

  bool isLocalTime() {
    return !timeZone.isUtc;
  }

  bool isUtc() {
    return timeZone.isUtc;
  }

  String toString() {
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

  // Adds the [duration] to this Date instance.
  Date add(Duration duration) {
    checkNull(duration);
    return new DateImplementation.fromEpoch(value + duration.inMilliseconds,
                                            timeZone);
  }

  // Subtracts the [duration] from this Date instance.
  Date subtract(Duration duration) {
    checkNull(duration);
    return new DateImplementation.fromEpoch(value - duration.inMilliseconds,
                                            timeZone);
  }

  // Returns a [Duration] with the difference of [this] and [other].
  Duration difference(Date other) {
    checkNull(other);
    return new Duration(milliseconds: value - other.value);
  }

  // Lazily keep a JS Date stored in the dart object.
  var _asJs() => Primitives.lazyAsJsDate(this);
}

class ListFactory<E> implements List<E> {
  factory List([int length]) => Primitives.newList(length);
  factory List.from(Iterable<E> other) {
    List<E> result = new List<E>();
    // TODO(ahe): Use for-in when it is implemented correctly.
    Iterator<E> iterator = other.iterator();
    while (iterator.hasNext()) {
      result.add(iterator.next());
    }
    return result;
  }
}

class DurationImplementation implements Duration {
  final int inMilliseconds;

  const DurationImplementation([int days = 0,
                                int hours = 0,
                                int minutes = 0,
                                int seconds = 0,
                                int milliseconds = 0])
    : inMilliseconds = days * Duration.MILLISECONDS_PER_DAY +
                       hours * Duration.MILLISECONDS_PER_HOUR +
                       minutes * Duration.MILLISECONDS_PER_MINUTE +
                       seconds * Duration.MILLISECONDS_PER_SECOND +
                       milliseconds;

  int get inDays() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_DAY;
  }

  int get inHours() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_HOUR;
  }

  int get inMinutes() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_MINUTE;
  }

  int get inSeconds() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_SECOND;
  }

  bool operator ==(other) {
    if (other is !Duration) return false;
    return inMilliseconds == other.inMilliseconds;
  }

  int hashCode() {
    return inMilliseconds.hashCode();
  }

  int compareTo(Duration other) {
    return inMilliseconds.compareTo(other.inMilliseconds);
  }

  String toString() {
    if (inMilliseconds < 0) {
      Duration duration =
          new DurationImplementation(milliseconds: -inMilliseconds);
      return "-${duration}";
    }
    String twoDigitMinutes =
        twoDigits(inMinutes.remainder(Duration.MINUTES_PER_HOUR));
    String twoDigitSeconds =
        twoDigits(inSeconds.remainder(Duration.SECONDS_PER_MINUTE));
    String threeDigitMs =
        threeDigits(inMilliseconds.remainder(Duration.MILLISECONDS_PER_SECOND));
    return "${inHours}:${twoDigitMinutes}:${twoDigitSeconds}.${threeDigitMs}";
  }
}
