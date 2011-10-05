import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_math(self):
        src = """
           margin: 2px - 5em -1px 0;
        """
        test = ""
        out = self.parser.loads(src)
        self.assertEqual(test, out)
