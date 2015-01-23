# GWTC -> A GWT compiler written in GWT

This is a prototype of advanced GWT features, such as Java 8, Elemental, JsInterop and Web Components.

As such, it depends on code that is not yet released in GWT master.
If you'd like to take a crack at building it yourself, you will first need to clone and build [this fork of GWT](https://github.com/WeTheInternet/gwt-sandbox/).
You should use version 2.7.1 and Java 8 to run `ant elemental dist-dev`

Next, you will also need to build the snapshot of [the XApi framework](http://github.com/WeTheInternet/xapi) as well.
This build may or may not work without some advice, so reach out to James -At- WeTheInter -Dot- Net if you need to.

In order to reduce the work needed to get started, I will be cutting a new version of both these dependencies and uploading to github.

In the meanwhile, the brave may feel free to clone and build away. 
