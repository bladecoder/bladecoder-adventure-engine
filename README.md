Bladecoder Adventure Engine
===========================

The **Bladecoder Adventure Engine** is a set of tools to create interactive graphic adventures (classical point and click games).

I think that this type of games are a great medium to tell stories and mobile devices provide a big opportunity to rebirth and evolve them.

By creating the **Bladecoder Adventure Engine**, I want to create a platform to tell stories. Interactive stories with modern graphics, animations and music.

The **Bladecoder Adventure Engine** is composed of the following subprojects:

* **adventure-editor**: the graphical editor for creating point and click games.
* **blade-engine**: the engine to run the games created with `adventure-editor`.

The **Bladecoder Adventure Engine** has been developed using the [LibGDX](http://libgdx.badlogicgames.com/) framework and the project generates a layout similar to any LibGDX project. This lowers the learning curve and eases development and deploy on several platforms.

### Adventure Editor
The **Adventure Editor** is a graphical editor to create full point and click games with minimal programming.

![adventure editor 2014-09-26](https://cloud.githubusercontent.com/assets/6229260/4420346/1d3a1b8a-4578-11e4-8eec-415f5e27c005.png)

### Blade Engine
The Engine has the following features:
* Multi platform support: Android, IOS and Desktop (Windows, OSX and Linux)
* Several animation techniques: sprite/atlas animation, Spine (cutout) animation and 3d model animation
* 3d character support
* Multiresolution to deal with different densities and screen sizes
* Multilanguage support

### The Goddess Robbery

The **Bladecoder Adventure Engine** is currently under continuous development and it's ready for production. **The Goddess Robbery** is a test game created to show the features of the Engine.

The source of **The Goddess Robbery** can be downloaded  [here](https://github.com/bladecoder/bladecoder-adventure-tests/tree/master/venus) and it's useful to learn how to use the **Adventure Editor**.

**The Goddess Robbery** is also available for Android devices at the Google Play Store.

[![The Goddess Robbery on Google Play](http://i.imgur.com/yn6RYuX.png)](https://play.google.com/store/apps/details?id=org.bladecoder.engine)

### Documentation

All available documentation is in the [wiki page](https://github.com/bladecoder/bladecoder-adventure-engine/wiki). The documentation is not good enough and needs to improve, we are working on it. Meanwhile you can download and look into the [test projects](https://github.com/bladecoder/bladecoder-adventure-tests/).

### Download latest release

Check the [release page](https://github.com/bladecoder/bladecoder-adventure-engine/releases/latest) to download the latest version.

For Linux users, there is a Flatpack package with all the dependencies included.

<a href="https://flathub.org/apps/details/com.bladecoder.adventure-editor"><img width="240" alt="Download on Flathub" src="https://flathub.org/assets/badges/flathub-badge-en.png"/></a>

### Building and running
In order to compile, build and run the engine, the Java platform is necessary. The project uses Gradle to build and package.

Build:

    $ ./gradlew build

Run the Adventure Editor:

    $ ./gradlew run

Create a distribution package for the Adventure Editor:

    $ ./gradlew distZip

A zip package ready for distribution is created in the folder 'bladecoder-adventure-engine/adventure-editor/build/distributions'

### License
The **Bladecoder Adventure Engine** is licensed under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0.html), meaning you
can use it free of charge, without strings attached in commercial and non-commercial projects.
