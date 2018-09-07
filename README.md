# Slay the Spire card exporter

A simple mod for Slay the Spire that can export card images, relics, potions, and monster images.

## How to use it

Simply load the mod with ModTheSpire. Make sure that Spire Exporter is the last item on the list, otherwise it will miss items from other mods.

At the start of the game, a directory "export" will be created that contains the exported images in a HTML format.

The size of the exported images is based on the resolution of the game. So for best results, set Slay the Spire to 1920x1080.

## Changing the export format

The exporter uses JTwig, and it is very easy to write your own templates. See [the cardlist html template](src/main/resources/templates/cardlist.html.twig) for an example. Changing the template currently requires the mod to be recompiled, but if there is demand for it this could easily be changed.

## Installation ##
1. [Download `ModTheSpire.jar`](https://github.com/kiooeht/ModTheSpire/releases)
2. Move `ModTheSpire.jar` into your **Slay The Spire** directory. This directory is likely to be found under `C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire`.
3. Create a `mods` folder in your **Slay The Spire** directory
4. [Download `BaseMod.jar`](https://github.com/daviscook477/BaseMod/releases), and place it in the `mods` folder.
5. [Download `StSExporter.jar`](https://github.com/twanvl/sts-exporter/releases), and place it in the `mods` folder.
6. Your modded version of **Slay The Spire** can now be launched by double-clicking on `ModTheSpire.jar`
7. This will open a mod select menu where you need to make sure that both `BaseMod` and `Spire Exporter` are checked before clicking **play**

