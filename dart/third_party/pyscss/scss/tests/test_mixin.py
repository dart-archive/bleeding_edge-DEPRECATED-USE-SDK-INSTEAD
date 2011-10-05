import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_mixin(self):
        src = """
        @mixin font {
            font: {
                weight: inherit;
                style: inherit;
                size: 100%;
                family: inherit; };
            vertical-align: baseline; }

        @mixin global {
            .global {
                border:red;
                @include font;
            }
        }

        @include global;

        @mixin rounded-top( $radius:10px ) {
            $side: top;
            border-#{$side}-radius: $radius;
            -moz-border-radius-#{$side}: $radius;
            -webkit-border-#{$side}-radius: $radius;
        }
        #navbar li { @include rounded-top; }
        #footer { @include rounded-top(5px); }
        """
        test = ".global{border:#f00;vertical-align:baseline;font-weight:inherit;font-style:inherit;font-size:100%;font-family:inherit}#navbar li{border-top-radius:10px;-moz-border-radius-top:10px;-webkit-border-top-radius:10px}#footer{border-top-radius:5px;-moz-border-radius-top:5px;-webkit-border-top-radius:5px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
