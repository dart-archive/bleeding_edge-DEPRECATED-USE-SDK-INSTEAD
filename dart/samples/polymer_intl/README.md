## A polymer component with localized messages

A simple demonstration of internationalization/localization
with Polymer components. It defines a Polymer component that lets you
choose from a selection of locales and displays a message translated
for the selected locale.

The component is defined in localized.html and localized.dart, and the
main entry point is polymer_intl.html

### Files

* `pubspec.yaml`: the Pub dependencies for this application.

* `build.dart`: creates the deployed Polymer application.

* `web/polymer_intl.html`: the main application file. This imports and
uses the `<localized-example>` custom element.

* `web/polymer_intl.css`: Styles for the application.

* `web/localized.html`, `web/localized.dart`: Defines the 
`<localized-example>` custom element.

* `intl_messages.arb`, `translation_fr.arb`, and `translation_pt.arb`:
Data files used in the translation process.

* `messages_all.dart`, `messages_fr.dart`, and `messages_pt.dart`:
Generated code defining Dart libraries with the translated messages.
