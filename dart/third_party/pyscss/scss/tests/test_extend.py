import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_extend(self):
        src = """
        .error {
            border: 1px #f00;
            background-color: #fdd;
        }
        .error .intrusion {
            background-image: url("/image/hacked.png");
        }
        .seriousError {
            @extend .error;
            border-width: 3px;
        }
        """
        test = ".error, .seriousError{border:1px #f00;background-color:#fdd}.error .intrusion, .seriousError .intrusion{background-image:url('/image/hacked.png')}.seriousError{border-width:3px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
