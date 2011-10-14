" SCSS Values."

from colorsys import rgb_to_hls, hls_to_rgb
from pyparsing import ParseResults

from scss import OPRT, CONV_FACTOR, COLORS
from scss.base import Node


hex2rgba = {
    8: lambda c: (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), int(c[6:8], 16)),
    6: lambda c: (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 1.0),
    4: lambda c: (int(c[0]*2, 16), int(c[1]*2, 16), int(c[2]*2, 16), int(c[3]*2, 16)),
    3: lambda c: (int(c[0]*2, 16), int(c[1]*2, 16), int(c[2]*2, 16), 1.0),
}


def hsl_op(op, color, h, s, l):
    color = ColorValue(color)
    h, s, l = map(NumberValue, (h, s, l))
    h.units = 'deg'
    s.units = l.units = '%'
    other_hls = map(float, (h, l, s))
    self_hls = rgb_to_hls(*map(lambda x: x / 255.0, color.value[:3]))
    res_hls = map(lambda x, y: op(x, y) if op else y if y else x, self_hls, other_hls)
    res_hls = map(lambda x: 1 if x > 1 else 0 if x < 0 else x, res_hls)
    res = hls_to_rgb(*res_hls)
    return ColorValue((res[0] * 255.0, res[1] * 255.0, res[2] * 255.0, color.value[3]))


def rgba_op(op, color, r, g, b, a):
    other = (float(r), float(g), float(b), float(a))
    res = map(op or ( lambda x, y: x or y ), color.value, other)
    res[3] = 1 if float(a) == color.value[3] == 1 else res[3]
    return ColorValue(res)


class Value(Node):
    """ Abstract value.
    """
    @classmethod
    def _do_op(cls, self, other, op):
        first, second = cls(self), cls(other)
        return op(first.value, second.value)

    @classmethod
    def _do_cmps(cls, first, second, op):
        return op(first.value, second.value)

    # Math operation
    def __add__(self, other):
        return self._do_op(self, other, OPRT['+'])

    __radd__ = __add__

    def __div__(self, other):
        return self._do_op(self, other, OPRT['/'])

    def __rdiv__(self, other):
        return self._do_op(other, self, OPRT['/'])

    def __sub__(self, other):
        return self._do_op(self, other, OPRT['-'])

    def __rsub__(self, other):
        return self._do_op(other, self, OPRT['-'])

    def __mul__(self, other):
        return self._do_op(self, other, OPRT['*'])

    def __lt__(self, other):
        return self._do_cmps(self, other, OPRT['<'])

    __rmul__ = __mul__

    # Compare operation
    def __le__(self, other):
        return self._do_cmps(self, other, OPRT['<='])

    def __gt__(self, other):
        return self._do_cmps(self, other, OPRT['>='])

    def __ge__(self, other):
        return self._do_cmps(self, other, OPRT['>'])

    def __eq__(self, other):
        return self._do_cmps(self, other, OPRT['=='])

    def __ne__(self, other):
        return self._do_cmps(self, other, OPRT['!='])

    # Boolean
    def __nonzero__(self):
        return getattr(self, 'value') and True or False

    def __bool__(self):
        return bool(self.value) if self.value != 'false' else False

    def __str__(self):
        return str(self.value)

    def __float__(self):
        return float(self.value)


class StringValueMeta(type):

    def __call__(mcs, *args, **kwargs):
        test = mcs.__new__(mcs)
        test.__init__(*args, **kwargs)

        if test.value in ('true', 'false'):
            return BooleanValue(test.value)

        elif COLORS.has_key(test.value):
            return ColorValue(COLORS.get(test.value))

        return test


class StringValue(Value):

    __metaclass__ = StringValueMeta

    def_value = ''

    def __init__(self, t):
        super(StringValue, self).__init__(None, None, t)

        self.value = self.def_value

        if isinstance(t, ParseResults):
            self.value = ''.join(str(s) for s in t)

        elif isinstance(t, StringValue):
            self.value = t.value

        elif isinstance(t, Node):
            self.value = str(t.value).strip('"\'')

        elif isinstance(t, (str, int, float)):
            self.value = str(t)

    def __div__(self, other):
        self.value = '/'.join((str(self), str(other)))
        return self

    def __str__(self):
        return self.value


class PointValue(Value):

    @property
    def value(self):
        return ' '.join(map(str, self.data))

    def __str__(self):
        return self.value

class RepeatValue(Value):
    @property
    def value(self):
        return ''.join(map(str, self.data))

    def __str__(self):
        return '[%s]' % self.value

class QuotedStringValue(StringValue):

    def __init__(self, t):
        super(QuotedStringValue, self).__init__(t)
        self.value = self.value.strip('"\'')

    def __str__(self):
        return "'%s'" % self.value


class ColorValue(Value):

    def_value = (255.0, 255.0, 255.0, 1)

    def __init__(self, t):
        super(ColorValue, self).__init__(None, None, t)
        self.value = self.def_value

        if isinstance(t, ParseResults):
            val = t[0][1:]
            self.value = hex2rgba[len(val)](val)

        elif isinstance(t, (list, tuple)):
            r = self.value
            c = map(lambda x, y: x if not x is None else y, t, r)
            c = tuple(0.0 if c[i] < 0 else r[i] if c[i] > r[i] else c[i] for i in range(4))
            self.value = c

        elif isinstance(t, str):
            val = t[1:]
            self.value = hex2rgba[len(val)](val)

        elif isinstance(t, ColorValue):
            self.value = t.value

    def __float__(self):
        return float( sum(self.value[:3], 0.0) / 3 * self.value[3] )

    def __str__(self):
        if self.value[3] == 1:
            v = '%02x%02x%02x' % self.value[:3]
            if v[0] == v[1] and v[2] == v[3] and v[4] == v[5]:
                v = v[0] + v[2] + v[4]
            return '#%s' % v
        return 'rgba(%d,%d,%d,%.2f)' % self.value

    __repr__ = __str__

    @classmethod
    def _do_op(cls, self, other, op):
        if isinstance(other, ColorValue):
            return rgba_op(op, self, *other.value)

        elif isinstance(other, ( NumberValue, int )):
            if op in (OPRT['*'], OPRT['/']):
                return ColorValue(map(lambda x: op(x, float(other)), self.value[:3]))
            return hsl_op(op, self, 0, other, 0)

        else:
            return self


class BooleanValue(Value):

    def_value = True

    def __init__(self, t):
        super(BooleanValue, self).__init__(None, None, t)
        self.value = self.def_value

        if isinstance(t, (str, bool)):
            self.value = bool(t) if t != 'false' else False

        elif isinstance(t, Node):
            self.value = bool(t)

    def __str__(self):
        return 'true' if self.value else 'false'

    __repr__ = __str__

    def __float__(self):
        return 1.0 if self.value else 0.0


class NumberValue(Value):

    def_value = 0.0

    def __init__(self, t):
        super(NumberValue, self).__init__(None, None, t)
        self.value = self.def_value
        self.units = ''

        if isinstance(t, (ParseResults, list, tuple)):
            if len(t) > 1:
                self.units = t[1]
            self.value = float(t[0])

        elif isinstance(t, NumberValue):
            self.value, self.units = t.value, t.units

        elif isinstance(t, Node):
            self.value = float(t.value)
            self.units = getattr(t.value, 'units', '')

        elif isinstance(t, (int, float, str)):
            self.value = float(t)

    def __float__(self):
        return self.value * CONV_FACTOR.get(self.units, 1.0)

    def __str__(self):
        value = ("%0.03f" % self.value).strip('0').rstrip('.') or 0
        # Don't emit unit suffix for 0 unless it's a percentage.  Necessary in
        # animation keyframe selectors.
        theUnits = self.units if (self.value or self.units == '%')  else ''
        return "%s%s" % (value, theUnits)

    @classmethod
    def _do_op(cls, self, other, op):
        first, second = cls(self), cls(other)
        units = second.units or first.units
        value = op(float(first), float(second))
        value /= CONV_FACTOR.get(units, 1.0)
        return cls((value, units))
