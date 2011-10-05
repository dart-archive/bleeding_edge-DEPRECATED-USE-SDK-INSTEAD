import sys, os
from os.path import abspath, dirname, join, normpath
from scss import OPRT
from scss.base import ParseNode, Empty, Node, IncludeNode
from scss.function import FUNCTION_LIST, unknown, warn
from scss.value import StringValue, Value, BooleanValue, NumberValue, QuotedStringValue

class Option(Empty):
    """ Set parser option.
    """
    def parse(self, target):
        opts = map(lambda (x, y): (x.value, BooleanValue(y).value),
                    zip(*[iter(self.data[1:])]*2))
        for v in opts:
            self.root.set_opt(*v)


class Variable(Value, ParseNode):

    def_value = StringValue('none')

    @classmethod
    def _do_op(cls, self, other, op):
        return self.value._do_op(self.value, other, op)

    @classmethod
    def _do_cmps(cls, self, other, op):
        return self.value._do_cmps(self.value, other, op)

    def __nonzero__(self):
        return True

    @property
    def value(self):
        name = self.data[0].strip('-$')
        minus = self.data[0][0] == '-'
        value = self.ctx.get(name) or self.root.ctx.get(name, self.def_value)
        return (0 - value) if minus else value


class Variables(ParseNode, Empty):
    pass


class Expression(Variable):

    @property
    def value(self):
        it = iter(self.data)
        try:
            first = next(it)
            while True:
                res = next(it)
                op = OPRT.get(res.strip(), None)
                if op:
                    second = next(it)
                    first = op(first, second)

                    if op == OPRT['and'] and not first:
                        raise StopIteration

                    elif op == OPRT['or'] and first:
                        raise StopIteration

        except StopIteration:
            while isinstance(first, Variable):
                first = first.value
            return first


class SepValString(Expression):

    @property
    def value(self):
        return ', '.join(str(e.value) for e in self.data)


class Function(Expression):

    @property
    def value(self):
        name = self.data[0]
        func_name_a = "%s:%d" % (name, len(self.data) - 1)
        func_name_n = "%s:n" % name
        func = FUNCTION_LIST.get(func_name_a, FUNCTION_LIST.get(func_name_n, unknown))

        params = map(lambda v: v.value, self.data[1:])
        return func(*params, root=self.root, name=name)


class FunctionDefinition(Empty):

    def parse(self, target):
        name = self.data[1].value
        params = self.data[2]
        func_name = '%s:%s' % (name, len(params))
        FUNCTION_LIST[func_name] = self.wrapper

    def wrapper(self, *args, **kwargs):
        self.ctx = Mixin.get_context(self.data[2], args)
        for node in self.data[2:]:
            if isinstance(node, FunctionReturn):
                return node.value
            elif isinstance(node, Node):
                node.parse(self)


class FunctionReturn(Variable):

    @property
    def value(self):
        return self.data[1]


class MixinParam(Empty):
    def __init__(self, s, n, t):
        super(MixinParam, self).__init__(s, n, t)
        self.name = self.data[0].data[0][1:]
        self.value = self.data[1] if len(self.data) > 1 else None


class Extend(Empty):

    def parse(self, target):
        for rule in self.root.cache['rset'][self.data[1]]:
            rule.name.extend(target.name)


class Mixin(Empty):

    def __init__(self, s, n, t):
        super(Mixin, self).__init__(s, n, t)
        self.name = str(self.data[1])
        self.default = self.data[2]

    def parse(self, target):
        self.root.cache['mix'][self.name] = self

    def include(self, target, params):
        ctx = self.get_context(self.default, params)
        if target.ctx:
            ctx.update(target.ctx)

        self.ctx = ctx
        for n in params:
            n.parse(self)

        for node in self.data[3:]:
            if isinstance(node, Node):
                copy = node.copy()
                copy.ctx = ctx
                copy.parse(target)

    @staticmethod
    def get_context(default, params=''):
        test = map(lambda x, y: (x, y), default, params)
        return dict(( mp.name, v or mp.value ) for mp, v in test if mp)


class Include(IncludeNode):

    def parse(self, target):
        from scss import parser

        includeValue = self.data[1]
        if isinstance(target, ParseNode) and not(isinstance(includeValue,
                                                            QuotedStringValue)):
            name = str(includeValue)
            params = self.data[2:]
            mixin = self.root.cache['mix'].get(name)
            if mixin:
                mixin.include(target, params)
            else:
                warn("Required mixin not found: %s:%d." % ( name, len(params)))
        elif isinstance(target, parser.Stylesheet) and isinstance(includeValue,
                QuotedStringValue):
            # @include "scss file to include"
            # Parse this file for more class names to emit in Dart class.
            options = target.currentOptions
            f = target.currentFile

            s = parser.Stylesheet(options=target.currentOptions)
            f = target.currentFile
            path = dirname(abspath(f.name))
            includePath = normpath('{0}/{1}'.format(path, self.data[1].value))
            f = open(includePath, 'r')
            nodes = s.loadReturnNodes(f)
            target.addInclude(includeValue.value, nodes)

class If(IncludeNode):

    def parse(self, target):
        self.data[0].parse(self)
        target.ctx.update(self.ctx)
        if isinstance(target, ParseNode):
            if self.data[0].value:
                self.data[1].parse(target)
            elif len(self.data) > 2:
                self.data[2].parse(target)


class For(IncludeNode):
    def parse(self, target):
        if isinstance(target, ParseNode):
            name = self.data[1].data[0][1:]
            for i in xrange(int(float(self.data[2])), int(float(self.data[3]))+1):
                body = self.data[4].copy()
                body.ctx.update({name: NumberValue(i)})
                body.parse(target)

class Stylet(Empty):
    def __init__(self, s, n, t):
      super(Stylet, self).__init__(s, n, t)
      self.name = str(self.data[1])
