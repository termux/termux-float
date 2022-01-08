# Termux:Float

[![Build status](https://github.com/termux/termux-float/workflows/Build/badge.svg)](https://github.com/termux/termux-float/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

A [Termux] plugin app to show the terminal in a floating terminal window.
##



### Contents
- [Installation](#Installation)
- [Terminal and App Settings](#Terminal-and-App-Settings)
- [Debugging](#Debugging)
- [Worthy Of Note](#Worthy-Of-Note)
- [For Maintainers and Contributors](#For-Maintainers-and-Contributors)
- [Forking](#Forking)
##



### Installation

Latest version is `v0.15.0`.

**Check [`termux-app` Installation](https://github.com/termux/termux-app#Installation) for details before reading forward.**

### F-Droid

`Termux:Float` application can be obtained from `F-Droid` from [here](https://f-droid.org/en/packages/com.termux.window).

You **do not** need to download the `F-Droid` app (via the `Download F-Droid` link) to install `Termux:Float`. You can download the `Termux:Float` APK directly from the site by clicking the `Download APK` link at the bottom of each version section.

It usually takes a few days (or even a week or more) for updates to be available on `F-Droid` once an update has been released on `Github`. The `F-Droid` releases are built and published by `F-Droid` once they [detect](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.termux.window.yml) a new `Github` release. The Termux maintainers **do not** have any control over the building and publishing of the Termux apps on `F-Droid`. Moreover, the Termux maintainers also do not have access to the APK signing keys of `F-Droid` releases, so we cannot release an APK ourselves on `Github` that would be compatible with `F-Droid` releases.

The `F-Droid` app often may not notify you of updates and you will manually have to do a pull down swipe action in the `Updates` tab of the app for it to check updates. Make sure battery optimizations are disabled for the app, check https://dontkillmyapp.com/ for details on how to do that.

### Github

`Termux:Float` application can be obtained on `Github` either from [`Github Releases`](https://github.com/termux/termux-float/releases) for version `>= 0.15.0` or from [`Github Build`](https://github.com/termux/termux-float/actions/workflows/debug_build.yml) action workflows.

The APKs for `Github Releases` will be listed under `Assets` drop-down of a release. These are automatically attached when a new version is released.

The APKs for `Github Build` action workflows will be listed under `Artifacts` section of a workflow run. These are created for each commit/push done to the repository and can be used by users who don't want to wait for releases and want to try out the latest features immediately or want to test their pull requests. Note that for action workflows, you need to be [**logged into a `Github` account**](https://github.com/login) for the `Artifacts` links to be enabled/clickable. If you are using the [`Github` app](https://github.com/mobile), then make sure to open workflow link in a browser like Chrome or Firefox that has your Github account logged in since the in-app browser may not be logged in.

The APKs for both of these are [`debuggable`](https://developer.android.com/studio/debug) and are compatible with each other but they are not compatible with other sources.

### Google Play Store **(Deprecated)**

**Termux and its plugins are no longer updated on [Google Play Store](https://play.google.com/store/apps/details?id=com.termux.window) due to [android 10 issues](https://github.com/termux/termux-packages/wiki/Termux-and-Android-10) and have been deprecated. It is highly recommended to not install Termux apps from Play Store any more.** Check https://github.com/termux/termux-app#google-play-store-deprecated for details.
##



### Terminal and App Settings

The `Termux:Float` app supports defining various settings in `~/.termux/termux.float.properties` file like the `Termux` app does in `~/.termux/termux.properties` file for version `>= 0.15.0`. Currently, only the following properties are supported: `enforce-char-based-input`, `ctrl-space-workaround`, `bell-character`, `terminal-cursor-style`, `terminal-transcript-rows`, `back-key`, `default-working-directory`, `volume-keys`. Check [Terminal Settings](https://wiki.termux.com/wiki/Terminal_Settings) for more info. The `~/` is a shortcut for the Termux home directory `/data/data/com.termux/files/home/` and can also be referred by the `$HOME` shell environment variable.

You can create/edit it by running the below commands to open the `nano` text editor in the terminal. Press `Ctrl+o` and then `Enter` to save and `Ctrl+x` to exit. You can also edit it with a [SAF file browser](https://github.com/termux/termux-tasker#Creating-And-Modifying-Scripts) after creating it.

```
mkdir -p ~/.termux
nano ~/.termux/termux.float.properties
```

##



### Debugging

You can help debug problems by setting appropriate `logcat` `Log Level` in `Termux` app settings -> `Termux:Float` -> `Debugging` -> `Log Level` (Requires `Termux` app version `>= 0.118.0`). The `Log Level` defaults to `Normal` and log level `Verbose` currently logs additional information. Its best to revert log level to `Normal` after you have finished debugging since private data may otherwise be passed to `logcat` during normal operation and moreover, additional logging increases execution time.

Once log levels have been set, you can run the `logcat` command in `Termux` or `Termux:Float` app terminal to view the logs in realtime (`Ctrl+c` to stop) or use `logcat -d > logcat.txt` to take a dump of the log. You can also view the logs from a PC over `ADB`. For more information, check official android `logcat` guide [here](https://developer.android.com/studio/command-line/logcat).

##### Log Levels
- `Off` - Log nothing
- `Normal` - Start logging error, warn and info messages and stacktraces
- `Debug` - Start logging debug messages
- `Verbose` - Start logging verbose messages
##



## For Maintainers and Contributors

Check [For Maintainers and Contributors](https://github.com/termux/termux-app#For-Maintainers-and-Contributors) section of `termux/termux-app` `README` for details.
##



## Forking

Check [Forking](https://github.com/termux/termux-app#Forking) section of `termux/termux-app` `README` for details.
##



[Termux]: https://termux.com
