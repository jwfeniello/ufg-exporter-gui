![image](https://github.com/user-attachments/assets/71978f8c-9870-45f4-b5c8-47dbe1cf8d2d)
## UFG Exporter GUI

This is an attempt to make Java GUI that wraps around `ufg-exporter-0.1.jar` to (hopefully) make exporting ModNation Racers and LBP Karting models/textures easier.

You just give it one of the game folders and it will do everything else.

### Features

- Recursively finds models and textures in subfolders
- Works even if folders only have models, only textures, or both
- Works with both ModNation and LBP Karting
- Option to export textures as PNGs
- Shows Progress
- Modern dark theme UI
- Has me trying to think of more things to sound impressive

### Dependencies

You need to download FlatLaf:

https://repo1.maven.org/maven2/com/formdev/flatlaf/3.6/flatlaf-3.6.jar

Place it in the project root directory (same folder as `ModnationExporterGUI.java`)

### How to Build

You need Java (version 11 or higher)

```bash
javac -cp "flatlaf-3.6.jar" ModnationExporterGUI.java
mkdir temp && cd temp && jar xf ../flatlaf-3.6.jar && cd ..
jar cfm ModnationExporterGUI.jar manifest.txt ModnationExporterGUI.class "FIXED UFG.png" -C temp .
rmdir /s temp
```

Then run it with:

```bash
java -jar ModnationExporterGUI.jar
```

This is very early and there will be bugs so please lmk here or on discord (username is clickbate) if you have any issues.  

HUGE THANKS TO ENNUO FOR ALL THEIR AMAZING AND HARD WORK IN THE LBP AND MODNATION SCENES ❤️❤️❤️❤️
