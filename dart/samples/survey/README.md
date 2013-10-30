A polymer.dart app for creating online surveys
==============================================

This is a polymer.dart app that lets you create an online survey. The app lets
you preview seach survey question and lets you pick the widget for each user
response.

To run this app, run 'Pub Get' from the Tools menu, and click on
`web/index.html`.

The app defines three custom elements, `<survey-element>`,
`<question-element>`, and `<selection-element>`.

`<survey-element>` encapsulates the entire app. The main `index.html` file
imports and uses this element.

`<question-element>` is the element for a single question in the survey. It
performs validation to ensure that the question is correctly formed, and
displays the answers selected by the user. This element uses a
`<selection-element>` to provide a widget for the user's answers.

`<selection-element>` is the element that lets you pick the widget and format
used by the user responding to your survey. This element substitutes for
commonly used form elements like radio buttons and checkboxes.  You can use a
drop down menu to pick the widget used to answer your question:

* For a written response, use a text widget.
* When the user must pick a single option, use the
'Select one from many options' option. This feature replaces the use radio
buttons for getting a user response.

* When the user can choose multiple options, use the
'Select many from many options' option. This feature replace the use of
checkboxes for getting a user repsonse.

Please report any [bugs or feature requests](http://dartbug.com/new).

