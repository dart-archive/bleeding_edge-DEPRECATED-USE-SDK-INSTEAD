library pop_pop_win.field_test;

import 'package:pop_pop_win/src/game.dart';
import 'package:unittest/unittest.dart';

import 'test_util.dart';

void main() {
  test('defaults', _testDefaults);
  test('bombCount', _testBombCount);
  test('fromSquares', _testFromSquares);
  test('adjacent', _testAdjacent);
}

void _testDefaults() {
  var f = new Field();

  expect(f.bombCount, equals(40));
  expect(f.height, equals(16));
  expect(f.width, equals(16));
}

void _testBombCount() {
  var f = new Field();

  int bombCount = 0;
  for (int x = 0; x < 16; x++) {
    for (int y = 0; y < 16; y++) {
      if (f.get(x, y)) {
        bombCount++;
      }
    }
  }
  expect(bombCount, equals(f.bombCount));
}

void _testFromSquares() {
  var f = new Field.fromSquares(2, 2, [true, true, true, false]);
  expect(f.height, equals(2));
  expect(f.width, equals(2));
  expect(f.bombCount, equals(3));
}

void _testAdjacent() {
  var f = getSampleField();

  expect(f.bombCount, equals(13));

  for (int x = 0; x < f.width; x++) {
    for (int y = 0; y < f.height; y++) {
      var i = x + y * f.width;
      var adj = f.getAdjacentCount(x, y);
      expect(adj, equals(SAMPLE_FIELD[i]));
    }
  }
}
