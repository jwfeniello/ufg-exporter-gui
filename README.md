![image](https://github.com/user-attachments/assets/71978f8c-9870-45f4-b5c8-47dbe1cf8d2d)
## UFG Exporter GUI

This is an attempt to make Java GUI that wraps around `ufg-exporter-0.1.jar` to (hopefully) make exporting ModNation Racers and LBP Karting models/textures easier.

You give just give it one of the game folders and it will do everything else.

### Features

- Recursively finds models and textures in subfolders
- Works even if folders only have models, only textures, or both
- Works with both ModNation and LBP Karting
- Option to export textures as PNGs
- Shows Progress

### How to Build

You need Java (version 11 or higher)

```bash
javac ModnationExporterGUI.java
jar cfm ModnationExporterGUI.jar manifest.txt ModnationExporterGUI.class "FIXED UFG.png"
```

Make sure your `manifest.txt` file contains:

```
Main-Class: ModnationExporterGUI
```

Then run it with:

```bash
java -jar ModnationExporterGUI.jar
```

Or just double-click the JAR if you're on Windows.

This is very early and there will be bugs so please lmk here or on discord (username is clickbate) if you have any issues.  
