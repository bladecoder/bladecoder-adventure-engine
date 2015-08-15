Bladecoder Adventure Engine
===========================

The **Bladecoder Adventure Engine** is a set of tools to create interactive graphic adventures (classical point ant click games).

I think that this type of games are a great medium to tell stories and mobile devices provide a big opportunity to rebirth and evolve them.

By creating the **Bladecoder Adventure Engine**, I want to create a platform to tell stories. Interactive stories with modern graphics, animations and music.

The **Bladecoder Adventure Engine** is composed of the following subprojects:

* **adventure-composer**: the graphical editor for creating point and click games.
* **blade-engine**: the engine to run the games created with `adventure-composer`.
 
The **Bladecoder Adventure Engine** has been developed using the [LibGDX](http://libgdx.badlogicgames.com/) framework and the project generates a layout similar to any LibGDX project. This lowers the learning curve and eases development and deploy on several platforms.

### Adventure Composer
The **Adventure Composer** is a graphical editor to create full point and click games with minimal programming.

![adventure composer 2014-09-26](https://cloud.githubusercontent.com/assets/6229260/4420346/1d3a1b8a-4578-11e4-8eec-415f5e27c005.png)

### Blade Engine
The Engine has the following features:
* Multi platform support: Android, IOS, Desktop (Windows, OSX, Linux) and HTML
* Several animation techniques: sprite/atlas animation, Spine (cutout) animation and 3d model animation
* 3d character support
* Multiresolution to deal with different densities and screen sizes
* Multilanguage support

### The Goddess Robbery

The **Bladecoder Adventure Engine** is currently under continuous development and it's almost ready for production. **The Goddess Robbery** is a test game created to show the features of the Engine.

The source of **The Goddess Robbery** can be downloaded  [here](https://github.com/bladecoder/bladecoder-adventure-tests/tree/master/venus) and it's useful to learn how to use the **Adventure Composer**.

**The Goddess Robbery** is also available for Android devices at the Google Play Store.

[![The Goddess Robbery on Google Play](http://developer.android.com/images/brand/en_app_rgb_wo_60.png)](https://play.google.com/store/apps/details?id=org.bladecoder.engine)

### Building and running
In order to compile, build and run the engine, the Java platform is necessary. The project uses Gradle to build and package.

Build:

    $ ./gradlew build

Run the Adventure Composer:

    $ ./gradlew run

Create a distribution package for the Adventure Composer:

    $ ./gradlew distZip

A zip package ready for distribution is created in the folder 'bladecoder-adventure-engine/adventure-composer/build/distributions'

### License
The **Bladecoder Adventure Engine** is licensed under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0.html), meaning you
can use it free of charge, without strings attached in commercial and non-commercial projects.


