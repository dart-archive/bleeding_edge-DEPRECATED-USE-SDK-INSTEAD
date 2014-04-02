// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'package:scheduled_test/scheduled_test.dart';
import 'package:scheduled_test/src/mock_clock.dart' as mock_clock;

import '../metatest.dart';
import '../utils.dart';

void main(_, message) {
  initMetatest(message);

  setUpTimeout();

  expectTestsFail('an out-of-band failure in wrapFuture is handled', () {
    mock_clock.mock().run();
    test('test', () {
      schedule(() {
        wrapFuture(sleep(1).then((_) => expect('foo', equals('bar'))));
      });
      schedule(() => sleep(2));
    });
  });

  expectTestsFail('an out-of-band failure in wrapFuture that finishes after '
      'the schedule is handled', () {
    mock_clock.mock().run();
    test('test', () {
      schedule(() {
        wrapFuture(sleep(2).then((_) => expect('foo', equals('bar'))));
      });
      schedule(() => sleep(1));
    });
  });

  expectTestsPass("wrapFuture should return the value of the wrapped future",
      () {
    test('test', () {
      schedule(() {
        expect(wrapFuture(pumpEventQueue().then((_) => 'foo')),
            completion(equals('foo')));
      });
    });
  });

  expectTestsPass("wrapFuture should pass through the error of the wrapped "
      "future", () {
    var error;
    test('test 1', () {
      schedule(() {
        wrapFuture(pumpEventQueue().then((_) {
          throw 'error';
        })).catchError(wrapAsync((e) {
          error = e;
        }));
      });
    });

    test('test 2', () {
      expect(error, equals('error'));
    });
  }, passing: ['test 2']);

  expectTestsPass("scheduled blocks whose return values are passed to "
      "wrapFuture should report exceptions once", () {
    var errors;
    test('test 1', () {
      currentSchedule.onException.schedule(() {
        errors = currentSchedule.errors;
      });

      wrapFuture(schedule(() {
        throw 'error';
      }));
    });

    test('test 2', () {
      expect(errors, everyElement(new isInstanceOf<ScheduleError>()));
      expect(errors.map((e) => e.error), equals(['error']));
    });
  }, passing: ['test 2']);
}
