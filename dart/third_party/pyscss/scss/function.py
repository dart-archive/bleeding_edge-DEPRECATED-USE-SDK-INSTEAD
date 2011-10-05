import base64
import colorsys
import math
import mimetypes
import os.path
import sys
from itertools import product

from scss import OPRT, CONV_TYPE, ELEMENTS_OF_TYPE
from scss.value import NumberValue, StringValue, QuotedStringValue, ColorValue, BooleanValue, hsl_op, rgba_op


try:
    from PIL import Image
except ImportError:
    Image = None

IMAGES = dict()


def warn(warning):
    """ Write warning messages in stderr.
    """
    print >> sys.stderr, "\nWarning: %s" % str( warning )


def unknown(*args, **kwargs):
    """ Unknow scss function handler.
        Simple return 'funcname(args)'
    """
    name = kwargs.get('name', '')
    return "%s(%s)" % ( name, ', '.join(str(a) for a in args) )


def check_pil(func):
    """ PIL module checking decorator.
    """
    def __wrapper(*args, **kwargs):
        root = kwargs.get('root')
        if not Image:
            if root and root.get_opt('warn'):
                warn("Images manipulation require PIL")
            return 'none'
        return func(*args, **kwargs)
    return __wrapper


# RGB functions
# =============

def _rgb(r, g, b, **kwargs):
    """ Converts an rgb(red, green, blue) triplet into a color.
    """
    return _rgba(r, g, b, 1.0)

def _rgba(r, g, b, a, **kwargs):
    """ Converts an rgba(red, green, blue, alpha) quadruplet into a color.
    """
    return ColorValue((float(r), float(g), float(b), float(a)))

def _red(color, **kwargs):
    """ Gets the red component of a color.
    """
    return NumberValue(color.value[0])

def _green(color, **kwargs):
    """ Gets the green component of a color.
    """
    return NumberValue(color.value[1])

def _blue(color, **kwargs):
    """ Gets the blue component of a color.
    """
    return NumberValue(color.value[2])

def _mix(color1, color2, weight=0.5, **kwargs):
    """ Mixes two colors together.
    """
    weight = float(weight)
    c1 = color1.value
    c2 = color2.value
    p = 0.0 if weight < 0 else 1.0 if weight > 1 else weight
    w = p * 2 - 1
    a = c1[3] - c2[3]

    w1 = ((w if (w * a == -1) else (w + a) / (1 + w * a)) + 1) / 2.0
    w2 = 1 - w1
    q = [ w1, w1, w1, p ]
    r = [ w2, w2, w2, 1 - p ]
    return ColorValue([c1[i] * q[i] + c2[i] * r[i] for i in range(4) ])


# HSL functions
# =============

def _hsl(h, s, l, **kwargs):
    """ HSL color value.
    """
    return _hsla(h, s, l, 1.0)

def _hsla(h, s, l, a, **kwargs):
    """ HSL with alpha channel color value.
    """
    res = colorsys.hls_to_rgb(float(h), float(l), float(s))
    return ColorValue(map(lambda x: x * 255.0, res) + [float(a)])

def _hue(color, **kwargs):
    """ Get hue value of HSL color.
    """
    h = colorsys.rgb_to_hls(*map(lambda x: x / 255.0, color.value[:3]))[0]
    return NumberValue(h * 360.0)

def _lightness(color, **kwargs):
    """ Get lightness value of HSL color.
    """
    l = colorsys.rgb_to_hls( *map(lambda x: x / 255.0, color.value[:3]) )[1]
    return NumberValue(( l * 100, '%' ))

def _saturation(color, **kwargs):
    """ Get saturation value of HSL color.
    """
    s = colorsys.rgb_to_hls( *map(lambda x: x / 255.0, color.value[:3]) )[2]
    return NumberValue(( s * 100, '%' ))

def _adjust_hue(color, degrees, **kwargs):
    return hsl_op(OPRT['+'], color, degrees, 0, 0)

def _lighten(color, amount, **kwargs):
    return hsl_op(OPRT['+'], color, 0, 0, amount)

def _darken(color, amount, **kwargs):
    return hsl_op(OPRT['-'], color, 0, 0, amount)

def _saturate(color, amount, **kwargs):
    return hsl_op(OPRT['+'], color, 0, amount, 0)

def _desaturate(color, amount, **kwargs):
    return hsl_op(OPRT['-'], color, 0, amount, 0)

def _grayscale(color, **kwargs):
    return hsl_op(OPRT['-'], color, 0, 100, 0)

def _complement(color, **kwargs):
    return hsl_op(OPRT['+'], color, 180.0, 0, 0)


# Opacity functions
# =================

def _alpha(color, **kwargs):
    c = ColorValue(color).value
    return NumberValue(c[3])

def _opacify(color, amount, **kwargs):
    return rgba_op(OPRT['+'], color, 0, 0, 0, amount)

def _transparentize(color, amount, **kwargs):
    return rgba_op(OPRT['-'], color, 0, 0, 0, amount)


# String functions
# =================

def _unquote(*args, **kwargs):
    return StringValue(' '.join(str(s).strip("\"'") for s in args))

def _quote(*args, **kwargs):
    return QuotedStringValue(' '.join(str(s) for s in args))


# Number functions
# =================

def _percentage(value, **kwargs):
    value = NumberValue(value)
    if not value.units == '%':
        value.value *= 100
        value.units = '%'
    return value

def _abs(value, **kwargs):
    return abs(float(value))

def _pi(**kwargs):
    return NumberValue(math.pi)

def _sin(value, **kwargs):
    return math.sin(value)

def _cos(value, **kwargs):
    return math.cos(value)

def _tan(value, **kwargs):
    return math.tan(value)

def _round(value, **kwargs):
    return round(value)

def _ceil(value, **kwargs):
    return math.ceil(value)

def _floor(value, **kwargs):
    return math.floor(value)


# Introspection functions
# =======================

def _type_of(obj, **kwargs):
    if isinstance(obj, BooleanValue):
        return StringValue('bool')
    if isinstance(obj, NumberValue):
        return StringValue('number')
    if isinstance(obj, QuotedStringValue):
        return StringValue('string')
    if isinstance(obj, ColorValue):
        return StringValue('color')
    if isinstance(obj, dict):
        return StringValue('list')
    return 'unknown'

def _unit(value, **kwargs):
    return NumberValue(value).units

def _unitless(value, **kwargs):
    if NumberValue(value).units:
        return BooleanValue(False)
    return BooleanValue(True)

def _comparable(n1, n2, **kwargs):
    n1, n2 = NumberValue(n1), NumberValue(n2)
    type1 = CONV_TYPE.get(n1.units)
    type2 = CONV_TYPE.get(n2.units)
    return BooleanValue(type1 == type2)


# Color functions
# ================

def _adjust_color(color, saturation=0.0, lightness=0.0, red=0.0, green=0.0, blue=0.0, alpha=0.0, **kwargs):
    return __asc_color(OPRT['+'], color, saturation, lightness, red, green, blue, alpha)

def _scale_color(color, saturation=1.0, lightness=1.0, red=1.0, green=1.0, blue=1.0, alpha=1.0, **kwargs):
    return __asc_color(OPRT['*'], color, saturation, lightness, red, green, blue, alpha)

def _change_color(color, saturation=None, lightness=None, red=None, green=None, blue=None, alpha=None, **kwargs):
    return __asc_color(None, color, saturation, lightness, red, green, blue, alpha)

def _invert(color, **kwargs):
    """ Returns the inverse (negative) of a color.
        The red, green, and blue values are inverted, while the opacity is left alone.
    """
    col = ColorValue(color)
    c = col.value
    c[0] = 255.0 - c[0]
    c[1] = 255.0 - c[1]
    c[2] = 255.0 - c[2]
    return col

def _adjust_lightness(color, amount, **kwargs):
    return hsl_op(OPRT['+'], color, 0, 0, amount)

def _adjust_saturation(color, amount, **kwargs):
    return hsl_op(OPRT['+'], color, 0, amount, 0)

def _scale_lightness(color, amount, **kwargs):
    return hsl_op(OPRT['*'], color, 0, 0, amount)

def _scale_saturation(color, amount, **kwargs):
    return hsl_op(OPRT['*'], color, 0, amount, 0)


# Compass helpers
# ================

def _color_stops(*args, **kwargs):
    raise NotImplementedError

def _elements_of_type(display, **kwargs):
    return StringValue(ELEMENTS_OF_TYPE.get(StringValue(display).value, ''))

def _enumerate(s, b, e, **kwargs):
    return ', '.join(
        "%s%d" % (StringValue(s).value, x) for x in xrange(int(b.value), int(e.value+1))
    )

def _font_files(*args, **kwargs):
    raise NotImplementedError

def _headings(a=None, b=None, **kwargs):
    h = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6']
    if not a or StringValue(a).value == 'all':
        a, b = 1, 6
    elif b is None:
        b, a = a.value + 1, 1
    return ', '.join(h[int(float(a)-1):int(float(b))])

def _nest(*args, **kwargs):
    return ', '.join(
        ' '.join(s.strip() for s in p)
            if not '&' in p[1] else p[1].replace('&', p[0].strip())
                for p in product(
                    *(StringValue(sel).value.split(',') for sel in args)
                )
        )

@check_pil
def _image_width(image, **kwargs):
    root = kwargs.get('root')
    path = os.path.abspath(os.path.join(root.get_opt('path'), StringValue(image).value))
    size = __get_size(path, root=root)
    return NumberValue([size[0], 'px'])

@check_pil
def _image_height(image, **kwargs):
    root = kwargs.get('root')
    path = os.path.abspath(os.path.join(root.get_opt('path'), StringValue(image).value))
    size = __get_size(path, root=root)
    return NumberValue([size[1], 'px'])

def _image_url(image, **kwargs):
    return QuotedStringValue(image).value

def _url(image, **kwargs):
    return 'url({0})'.format(QuotedStringValue(image).value)

def _inline_image(image, mimetype=None, **kwargs):
    root = kwargs.get('root')
    path = os.path.abspath(os.path.join(root.get_opt('path'), StringValue(image).value))
    if os.path.exists(path):
        mimetype = StringValue(mimetype).value or mimetypes.guess_type(path)[0]
        f = open(path, 'rb')
        url = 'data:' + mimetype + ';base64,' + base64.b64encode(f.read())
    else:
        if root and root.get_opt('warn'):
            warn("Not found image: %s" % path)
        url = '%s?_=NA' % QuotedStringValue(image).value
    inline = 'url("%s")' % url
    return StringValue(inline)


# Misc
# ====

def _if(cond, body, els, **kwargs):
    if BooleanValue( cond ).value:
        return body
    return els


def _sprite_position(*args):
    pass

def _sprite_file(*args):
    pass

def _sprite(*args):
    pass

def _sprite_map(*args):
    pass

def _sprite_map_name(*args):
    pass

def _sprite_url(*args):
    pass

def _opposite_position(*args):
    pass

def _grad_point(*args):
    pass

def _grad_color_stops(*args):
    pass

def _nth(*args):
    pass

def _join(*args):
    pass

def _append(*args):
    pass

# Layout minmax
def _minmax(n1, n2, **kwargs):
    return StringValue('minmax({0},{1})'.format(n1, n2))


FUNCTION_LIST = {

    # RGB functions
    'rgb:3': _rgb,
    'rgba:4': _rgba,
    'red:1': _red,
    'green:1': _green,
    'blue:1': _blue,
    'mix:2': _mix,
    'mix:3': _mix,

    # HSL functions
    'hsl:3': _hsl,
    'hsla:4': _hsla,
    'hue:1': _hue,
    'saturation:1': _saturation,
    'lightness:1': _lightness,
    'adjust-hue:2': _adjust_hue,
    'spin:2': _adjust_hue,
    'lighten:2': _lighten,
    'darken:2': _darken,
    'saturate:2': _saturate,
    'desaturate:2': _desaturate,
    'grayscale:1': _grayscale,
    'complement:1': _complement,

    # Opacity functions
    'alpha:1': _alpha,
    'opacity:1': _alpha,
    'opacify:2': _opacify,
    'fadein:2': _opacify,
    'fade-in:2': _opacify,
    'transparentize:2': _transparentize,
    'fadeout:2': _transparentize,
    'fade-out:2': _transparentize,

    # String functions
    'quote:n': _quote,
    'unquote:n': _unquote,

    # Number functions
    'percentage:1': _percentage,
    'sin:1': _sin,
    'cos:1': _cos,
    'tan:1': _tan,
    'abs:1': _abs,
    'round:1': _round,
    'ceil:1': _ceil,
    'floor:1': _floor,
    'pi:0': _pi,

    # Introspection functions
    'type-of:1': _type_of,
    'unit:1': _unit,
    'unitless:1': _unitless,
    'comparable:2': _comparable,

    # Color functions
    'adjust-color:n': _adjust_color,
    'scale-color:n': _scale_color,
    'change-color:n': _change_color,
    'adjust-lightness:2': _adjust_lightness,
    'adjust-saturation:2': _adjust_saturation,
    'scale-lightness:2': _scale_lightness,
    'scale-saturation:2': _scale_saturation,
    'invert:1': _invert,

    # Compass helpers
    'append-selector:2': _nest,
    'color-stops:n': _color_stops,
    'enumerate:3': _enumerate,
    'elements-of-type:1': _elements_of_type,
    'font-files:n': _font_files,
    'headings:n': _headings,
    'nest:n': _nest,

    # Images functions
    'url:1': _url,
    'image-url:1': _image_url,
    'image-width:1': _image_width,
    'image-height:1': _image_height,
    'inline-image:1': _inline_image,
    'inline-image:2': _inline_image,

    # Not implemented
    'sprite-map:1': _sprite_map,
    'sprite:2': _sprite,
    'sprite:3': _sprite,
    'sprite:4': _sprite,
    'sprite-map-name:1': _sprite_map_name,
    'sprite-file:2': _sprite_file,
    'sprite-url:1': _sprite_url,
    'sprite-position:2': _sprite_position,
    'sprite-position:3': _sprite_position,
    'sprite-position:4': _sprite_position,

    'opposite-position:n': _opposite_position,
    'grad-point:n': _grad_point,
    'grad-color-stops:n': _grad_color_stops,

    'nth:2': _nth,
    'first-value-of:1': _nth,
    'join:2': _join,
    'join:3': _join,
    'append:2': _append,
    'append:3': _append,

    'if:3': _if,
    'escape:1': _unquote,
    'e:1': _unquote,

    #layout metrics functions
    'minmax:2': _minmax,
}

def __asc_color(op, color, saturation, lightness, red, green, blue, alpha):
    if lightness or saturation:
        color = hsl_op(op, color, 0, saturation, lightness)
    if red or green or blue or alpha:
        color = rgba_op(op, color, red, green, blue, alpha)
    return color

def __get_size(path, **kwargs):
    root = kwargs.get('root')
    if not IMAGES.has_key(path):

        if not os.path.exists(path):

            if root and root.get_opt('warn'):
                warn("Not found image: %s" % path)

            return 0, 0

        image = Image.open(path)
        IMAGES[path] = image.size
    return IMAGES[path]
