# Termux:Float

[![Build status](https://github.com/termux/termux-float/workflows/Build/badge.svg)](https://github.com/termux/termux-float/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

A [Termux](https://termux.com/) add-on app to show the terminal in a floating
terminal window.

When developing (or packaging), note that this app needs to be signed with the
same key as the main Termux app in order to have the permission to modify the
required font or color files.

## Installation

Termux:Float application can be obtained from:

- [Google Play](https://play.google.com/store/apps/details?id=com.termux.window)
- [F-Droid](https://f-droid.org/en/packages/com.termux.window/)
- [Kali Nethunter Store](https://store.nethunter.com/en/packages/com.termux.window/)

Additionally we offer development builds for those who want to try out latest
features ready to be included in future versions. Such build can be obtained
directly from [Cirrus CI artifacts](https://api.cirrus-ci.com/v1/artifact/github/termux/termux-float/debug-build/output/app/build/outputs/apk/debug/app-debug.apk).

Signature keys of all offered builds are different. Before you switch the
installation source, you will have to uninstall the Termux application and
all currently installed plugins.
