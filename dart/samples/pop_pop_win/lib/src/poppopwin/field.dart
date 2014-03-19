part of ppw;

class Field extends Array2d<bool> {
  final int bombCount;
  final Array2d<int> _adjacents;

  factory Field([bombCount = 40, cols = 16, rows = 16, int seed = null]) {
    final squares = new List<bool>.filled(rows * cols, false);
    assert(bombCount < squares.length);
    assert(bombCount > 0);

    final rnd = new math.Random(seed);

    // This is the most simple code, but it'll get slow as
    // bombCount approaches the square count.
    // But more efficient if bombCount << square count
    // which is expected.
    for(int i = 0; i < bombCount; i++) {
      int index;
      do {
        index = rnd.nextInt(squares.length);
      } while(squares[index]);
      squares[index] = true;
    }

    return new Field._internal(bombCount, cols,
        new ReadOnlyCollection<bool>(squares));
  }

  factory Field.fromSquares(int cols, int rows, List<bool> squares) {
    assert(cols > 0);
    assert(rows > 0);
    assert(squares.length == cols * rows);

    int count = 0;
    for(final m in squares) {
      if(m) {
        count++;
      }
    }
    assert(count > 0);
    assert(count < squares.length);

    return new Field._internal(count, cols,
        new ReadOnlyCollection<bool>(squares));
  }

  Field._internal(this.bombCount, int cols, ReadOnlyCollection<bool> source) :
    this._adjacents = new Array2d<int>(cols, source.length ~/ cols),
    super.wrap(cols, source.toList()) {
    assert(width > 0);
    assert(height > 0);
    assert(bombCount > 0);
    assert(bombCount < length);

    int count = 0;
    for(final m in this) {
      if(m) {
        count++;
      }
    }
    assert(count == bombCount);
  }

  int getAdjacentCount(int x, int y) {
    if(get(x,y)) {
      return null;
    }

    int val = _adjacents.get(x, y);

    if(val == null) {
      val = 0;
      for(final i in getAdjacentIndices(x,y)) {
        if(this[i]) {
          val++;
        }
      }
      _adjacents.set(x, y, val);
    }
    return val;
  }

  String toString() => 'w${width}h${height}m${bombCount}';
}
