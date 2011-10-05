from scss import SORTING


class Node(object):
    """ Base class for parsed objects.
    """
    delim = ' '
    root = None

    def __init__(self, s, n, t):
        self.num, self.data = n, t
        self.parent = self._ctx = None

    def __str__(self):
        return self.delim.join(map(str, self.data))

    def __repr__(self):
        return '(%s%s)' % (
                self.__class__.__name__,
                ': %s' % ' '.join(map(repr, self.data)) if self.data else ''
        )

    def parse(self, target):
        self.parent = target

    def copy(self):
        return self

    @property
    def ctx(self):
        if self._ctx:
            return self._ctx

        if self.parent:
            return self.parent.ctx

        self._ctx = dict()
        return self._ctx

    @ctx.setter
    def ctx(self, value):
        self._ctx = value


class Empty(Node):

    def __str__(self):
        return ''


class ParseNode(Node):

    def parse(self, target):
        super(ParseNode, self).parse(target)
        for n in self.data:
            if isinstance(n, Node):
                n.parse(self)

    def copy(self):
        t = [n.copy() if isinstance(n, Node) else n for n in self.data]
        return self.__class__(None, self.num, t)


class ContentNode(ParseNode):

    def __init__(self, s, n, t):
        super(ContentNode, self).__init__(s, n, t)
        self.name = self.data[0] if self.data else ''
        self.declareset = []
        self.ruleset = []

    def __str__(self):
        # Sort declaration
        if self.root.get_opt('sort'):
            self.declareset.sort(
                    key=lambda x: SORTING.get(x.name, 999 ))

        nl, ws, ts = self.root.cache['delims']
        semicolon = '' if self.root.cache['opts'].get('compress') else ';'

        return ''.join((

            # Self
            ''.join((

                # Selector tree
                str(self.name),

                "{%s%s%s" % (nl, ws, ws) if self.name else '',

                # Declarations
                (';%s%s%s' % ( nl, ws, ws )).join(str(d) for d in self.declareset),

                semicolon,

                '%s}%s%s' % ( nl, nl, nl ) if self.name else ''

            )) if self.declareset else '',

            # Children
            ''.join(str(r) for r in self.ruleset)
        ))


class IncludeNode(ParseNode):

    def parse(self, target):
        for node in self.data:
            if isinstance(node, Node):
                node.ctx.update(self.ctx)
                node.parse(target)

    def __str__(self):
        node = ContentNode(None, None, [])
        self.parse(node)
        return str(node)
