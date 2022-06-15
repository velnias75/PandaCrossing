# PandaCrossing [![Build Status](https://travis-ci.com/velnias75/PandaCrossing.svg?branch=1.19)](https://travis-ci.com/velnias75/PandaCrossing) [![Java CI with Gradle](https://github.com/velnias75/PandaCrossing/actions/workflows/gradle.yml/badge.svg?branch=1.19)](https://github.com/velnias75/PandaCrossing/actions/workflows/gradle.yml)
A *Fabric Mod* to create QR codes

This simple mod creates a horizontal, vertical or stairway QR code with the bottom left corner below the player's feet resp. in front of the player, representing *a given text*.

With [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) you can use any material, else black and white concrete is used.

Commands
--------

* `/qr [text]`, `/vqr [text]`, `/sqr [text]` creates the QR code resp. `/qr [text]` shows the usage if no *text* is provided
* `/qrcalc [text]` calculates the dimensions of the resulting QR code
* `/qrundo` or `/pcundo` undoes the last creation in reverse order of creation

To get examples for how to use *text* see https://github.com/zxing/zxing/wiki/Barcode-Contents

Using on a server
-----------------

To create a QR code on a multiplayer server you'll need the permission for the `/setblock` command.

Caveat
------

*undo* works with the data received by the client. I.e. if another player modifies the same area in the meantime, *undo* will restore to the state at the invokation of `/qr`.
Further *entities* and *block entities* are currently **NOT** restored.

Dependencies
------------

* [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) **strongly recommended** for more control over the mod (get the settings menu with the key `u`)

![Screenshot of PandaCrossing](https://user-images.githubusercontent.com/4481414/120403323-42bff180-c344-11eb-8baa-7c0fa88aeea9.png)
