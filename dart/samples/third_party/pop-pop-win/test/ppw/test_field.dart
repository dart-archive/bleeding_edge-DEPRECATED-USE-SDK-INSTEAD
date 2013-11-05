part of ppw_test;

class TestField {
  // This grid
  // XXXXX2
  // X7X8X3
  // X5XXX2
  // X32321
  // 110000

  static const sample = const
      [null, null, null, null, null, 2,
       null,    7, null,    8, null, 3,
       null,    5, null, null, null, 2,
       null,    3,    2,    3,    2, 1,
          1,    1,    0,    0,    0, 0];


  static Field getSampleField() {
    final bools = new List<bool>.from(sample.map((x) => x == null));

    return new Field.fromSquares(6, 5, bools);
  }

  static void run() {
    group('Field', () {
      test('defaults', _testDefaults);
      test('bombCount', _testBombCount);
      test('fromSquares', _testFromSquares);
      test('adjacent', _testAdjacent);
    });
  }

  static void _testDefaults() {
    final f = new Field();

    expect(f.bombCount, equals(40));
    expect(f.height, equals(16));
    expect(f.width, equals(16));
  }

  static void _testBombCount() {
    final f = new Field();

    int bombCount = 0;
    for(int x = 0; x < 16; x++) {
      for(int y = 0; y < 16; y++) {
        if(f.get(x, y)) {
          bombCount++;
        }
      }
    }
    expect(bombCount, equals(f.bombCount));
  }

  static void _testFromSquares() {
    final f = new Field.fromSquares(2, 2, [true, true, true, false]);
    expect(f.height, equals(2));
    expect(f.width, equals(2));
    expect(f.bombCount, equals(3));
  }

  static void _testAdjacent() {
    final f = getSampleField();

    expect(f.bombCount, equals(13));

    for(int x = 0; x < f.width; x++) {
      for(int y = 0; y < f.height; y++) {
        final i = x + y * f.width;
        final adj = f.getAdjacentCount(x, y);
        expect(adj, equals(sample[i]));
      }
    }
  }
}
