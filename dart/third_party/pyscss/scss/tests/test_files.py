import os.path
import unittest

from scss import parser


class ScssCache(unittest.TestCase):

    def test_cache(self):
        path = os.path.join(os.path.dirname(__file__), 'example.scss')
        src = open(path).read()
        test = parser.parse(src)
        out = parser.load(open(path), precache=True)
        self.assertEqual(test, out)
