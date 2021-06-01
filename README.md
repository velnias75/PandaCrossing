![Screenshot of PandaCrossing](https://media.forgecdn.net/attachments/368/395/2021-06-01_11.png)

# PandaCrossing [![Build Status](https://travis-ci.com/velnias75/PandaCrossing.svg?branch=main)](https://travis-ci.com/velnias75/PandaCrossing)
A *Fabric Mod* to create QR codes

This simple mod creates a 23x23 block horizontal concrete QR code with the bottom left corner below the player's feet, representing *a given text*.

Commands
--------

* `/qr [text]` creates the QR code resp. shows the usage if no *text* is provided
* `/qrundo` or `/pcundo` undoes the last creation

To get examples for how to use *text* see https://github.com/zxing/zxing/wiki/Barcode-Contents

Using on a server
-----------------

To create a QR code on a multiplayer server you'll need the permission for the `/setblock` command.

Caveat
------

*undo* works with the data received by the client. I.e. if another player modifies the same area in the meantime, *undo* will restore to the state at the invokation of `/qr`.
Further *entities* and *block entities* are currently **NOT** restored.
