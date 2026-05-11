import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModnationExporterGUI {
    private static JTextField inputField;
    private static JTextField outputField;
    private static JTextField jarField;
    private static JCheckBox includeTextures;
    private static JCheckBox dumpPNGs;
    private static JTextArea log;
    private static JComboBox<String> gameSelector;
    private static JButton runButton;
    private static JProgressBar progressBar;
    private static final File CONFIG_FILE = new File("modnation_exporter_config.properties");
    private static final AtomicBoolean isExporting = new AtomicBoolean(false);

    public static void main(String[] args) {
        try {
            try {
                Class.forName("com.formdev.flatlaf.FlatDarkLaf");
                Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
                java.lang.reflect.Method setupMethod = flatLafClass.getMethod("setup");
                setupMethod.invoke(null);
            } catch (ClassNotFoundException e) {
                System.err.println("FlatLaf not found. Using system default look and feel.");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ex) {
            System.err.println("Failed to set look and feel: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("ModNation Exporter");
        frame.setSize(750, 600);
        frame.setMinimumSize(new Dimension(700, 550));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        loadLogo(topPanel);
        frame.add(topPanel, BorderLayout.NORTH);

        JPanel mainPanel = createMainPanel();
        frame.add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        frame.add(bottomPanel, BorderLayout.SOUTH);

        loadSavedPaths();

        frame.pack();
        frame.setSize(750, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void loadLogo(JPanel topPanel) {
        try {
            InputStream imgStream = ModnationExporterGUI.class.getResourceAsStream("/FIXED UFG.png");
            if (imgStream != null) {
                ImageIcon originalIcon = new ImageIcon(ImageIO.read(imgStream));
                Image scaledImage = originalIcon.getImage().getScaledInstance(320, 100, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                logoLabel.setHorizontalAlignment(JLabel.CENTER);
                topPanel.add(logoLabel, BorderLayout.CENTER);
            } else {
                JLabel titleLabel = new JLabel("ModNation Exporter GUI");
                titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
                titleLabel.setHorizontalAlignment(JLabel.CENTER);
                topPanel.add(titleLabel, BorderLayout.CENTER);
            }
        } catch (Exception e) {
            System.err.println("Failed to load logo: " + e.getMessage());
            JLabel titleLabel = new JLabel("ModNation Exporter GUI");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            topPanel.add(titleLabel, BorderLayout.CENTER);
        }
    }

    private static JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        inputField = new JTextField(40);
        outputField = new JTextField(40);
        jarField = new JTextField(40);
        gameSelector = new JComboBox<>(new String[]{"ModNation Racers", "LBP Karting"});

        inputField.setMinimumSize(new Dimension(300, 25));
        outputField.setMinimumSize(new Dimension(300, 25));
        jarField.setMinimumSize(new Dimension(300, 25));
        gameSelector.setMinimumSize(new Dimension(300, 25));

        mainPanel.add(makeRow("Input Folder:", inputField, e -> chooseDirectory(inputField)));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(makeRow("Output Folder:", outputField, e -> chooseDirectory(outputField)));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(makeRow("ufg-exporter-0.1.jar:", jarField, e -> chooseFile(jarField)));
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(makeRow("Game:", gameSelector));
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        includeTextures = new JCheckBox("Include textures");
        dumpPNGs = new JCheckBox("Dump textures as PNG");
        includeTextures.setSelected(true);
        dumpPNGs.setSelected(true);
        checkPanel.add(includeTextures);
        checkPanel.add(dumpPNGs);
        mainPanel.add(checkPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runButton = new JButton("Run Export");
        JButton helpButton = new JButton("What's UFG?");
        JButton clearLogButton = new JButton("Clear Log");
        runButton.setPreferredSize(new Dimension(120, 30));
        helpButton.setPreferredSize(new Dimension(120, 30));
        clearLogButton.setPreferredSize(new Dimension(120, 30));
        buttonPanel.add(runButton);
        buttonPanel.add(helpButton);
        buttonPanel.add(clearLogButton);
        mainPanel.add(buttonPanel);

        runButton.addActionListener(e -> runExport());
        helpButton.addActionListener(e -> showHelp());
        clearLogButton.addActionListener(e -> clearLog());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(500, 20));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(progressBar);

        return mainPanel;
    }

    private static JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        log = new JTextArea(8, 60);
        log.setFont(new Font("Monospaced", Font.PLAIN, 11));
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(log);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Export Log"));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel credit = new JLabel("Made by Clickbate");
        credit.setFont(new Font("Arial", Font.PLAIN, 9));
        credit.setHorizontalAlignment(JLabel.LEFT);
        credit.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        bottomPanel.add(credit, BorderLayout.SOUTH);

        return bottomPanel;
    }

    private static void appendLog(String text) {
        SwingUtilities.invokeLater(() -> log.append(text));
    }

    private static JPanel makeRow(String label, JComponent field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(150, 25));
        panel.add(jLabel);
        panel.add(field);
        return panel;
    }

    private static JPanel makeRow(String label, JTextField field, ActionListener browseAction) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(150, 25));
        panel.add(jLabel, BorderLayout.WEST);

        field.setPreferredSize(new Dimension(300, 25));
        panel.add(field, BorderLayout.CENTER);

        JButton browse = new JButton("Browse");
        browse.setPreferredSize(new Dimension(80, 25));
        browse.setMinimumSize(new Dimension(80, 25));
        browse.addActionListener(browseAction);
        panel.add(browse, BorderLayout.EAST);

        return panel;
    }

    private static void chooseDirectory(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Directory");

        String currentPath = target.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setCurrentDirectory(currentDir);
            }
        }

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static void chooseFile(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Select JAR File");

        String currentPath = target.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.exists()) {
                chooser.setCurrentDirectory(currentFile.getParentFile());
            }
        }

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static void clearLog() {
        log.setText("");
        log.append("Log cleared.\n");
    }

    private static void runExport() {
        if (isExporting.get()) {
            log.append("Export already in progress. Please wait.\n");
            return;
        }

        String inputPath = inputField.getText().trim();
        String outputPath = outputField.getText().trim();
        String jarPath = jarField.getText().trim();

        if (inputPath.isEmpty() || outputPath.isEmpty() || jarPath.isEmpty()) {
            log.append("ERROR: Please fill in all required fields.\n");
            return;
        }

        File inputDir = new File(inputPath);
        File outputDir = new File(outputPath);
        File jar = new File(jarPath);

        if (!inputDir.exists() || !inputDir.isDirectory()) {
            log.append("ERROR: Input directory does not exist or is not a directory.\n");
            return;
        }

        if (!outputDir.exists()) {
            try {
                if (!outputDir.mkdirs()) {
                    log.append("ERROR: Could not create output directory.\n");
                    return;
                }
            } catch (Exception e) {
                log.append("ERROR: Could not create output directory: " + e.getMessage() + "\n");
                return;
            }
        }

        if (!jar.exists() || !jar.isFile()) {
            log.append("ERROR: JAR file does not exist or is not a file.\n");
            return;
        }

        if (!jar.getName().toLowerCase().endsWith(".jar")) {
            log.append("WARNING: Selected file may not be a JAR file.\n");
        }

        // Capture Swing state before leaving the EDT
        String selectedGame = (String) gameSelector.getSelectedItem();
        boolean textures = includeTextures.isSelected();
        boolean pngs = dumpPNGs.isSelected();

        isExporting.set(true);
        runButton.setEnabled(false);
        runButton.setText("Exporting...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Starting export...");

        new Thread(() -> {
            try {
                performExport(inputDir, outputDir, jar, selectedGame, textures, pngs);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    isExporting.set(false);
                    runButton.setEnabled(true);
                    runButton.setText("Run Export");
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                });
            }
        }).start();
    }

    private static void performExport(File inputDir, File outputDir, File jar, String selectedGame, boolean textures, boolean pngs) {
        appendLog("Starting export process...\n");
        String game = selectedGame.toLowerCase().contains("karting") ? "lbpk" : "mnr";
        List<File> allDirs = new ArrayList<>();
        collectDirs(inputDir, allDirs);

        if (allDirs.isEmpty()) {
            appendLog("No directories found to process.\n");
            return;
        }

        appendLog("Found " + allDirs.size() + " directories to process.\n\n");

        boolean ranAny = false;
        int processedCount = 0;

        for (File dir : allDirs) {
            final int currentCount = processedCount;
            SwingUtilities.invokeLater(() ->
                progressBar.setString("Processing directory " + (currentCount + 1) + " of " + allDirs.size())
            );

            boolean didSomething = false;

            List<File> potentialModels = findPotentialModels(dir);

            for (File model : potentialModels) {
                appendLog("✓ Found potential model: " + model.getName() + " in: " + dir.getAbsolutePath() + "\n");
                appendLog("→ Exporting model...\n");
                boolean success = runCommand(jar, model, null, null, outputDir, dir.getName() + "_" + model.getName(), false, game);
                appendLog(success ? "✔ Done exporting model.\n" : "✗ Failed to export model.\n");
                didSomething = true;
                ranAny = true;
            }

            if (textures) {
                Map<File, File> texturePairs = findTexturePairs(dir);

                for (Map.Entry<File, File> pair : texturePairs.entrySet()) {
                    File permFile = pair.getKey();
                    File idxFile = pair.getValue();
                    String baseName = permFile.getName().replace(".PERM.BIN", "");

                    appendLog("✓ Found texture pair:\n");
                    appendLog("     PERM: " + permFile.getName() + "\n");
                    appendLog("     IDX : " + idxFile.getName() + "\n");
                    appendLog("→ Exporting textures...\n");

                    File modelToUse = potentialModels.isEmpty() ? null : potentialModels.get(0);
                    boolean success = runCommand(jar, modelToUse, permFile, idxFile, outputDir, dir.getName() + "_" + baseName, pngs, game);
                    appendLog(success ? "✔ Done exporting textures.\n" : "✗ Failed to export textures.\n");
                    didSomething = true;
                    ranAny = true;
                }
            }

            appendLog(didSomething ? "\n" : "⨯ Skipped " + dir.getName() + " (no valid model or texture pair)\n\n");
            processedCount++;
        }

        appendLog(ranAny ? "Export process completed.\n" : "No valid exports found.\n");

        SwingUtilities.invokeLater(() -> savePaths());
    }

    private static List<File> findPotentialModels(File dir) {
        List<File> models = new ArrayList<>();
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) continue;

                if (file.getName().equals("CHARMODELPACKSTREAMING.BIN")) {
                    models.add(file);
                    continue;
                }

                String name = file.getName().toUpperCase();
                if (name.endsWith(".BIN") &&
                    !name.endsWith(".PERM.BIN") &&
                    !name.endsWith(".IDX") &&
                    !name.contains("TEXTURES")) {
                    models.add(file);
                }
            }
        }

        return models;
    }

    private static Map<File, File> findTexturePairs(File dir) {
        Map<File, File> pairs = new HashMap<>();
        Map<String, File> permFiles = new HashMap<>();
        Map<String, File> idxFiles = new HashMap<>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) continue;

                String name = file.getName().toUpperCase();
                if (name.endsWith(".PERM.BIN")) {
                    permFiles.put(name.substring(0, name.lastIndexOf(".PERM.BIN")), file);
                } else if (name.endsWith(".PERM.IDX")) {
                    idxFiles.put(name.substring(0, name.lastIndexOf(".PERM.IDX")), file);
                } else if (name.endsWith(".BIN") && name.contains("TEXTURE")) {
                    permFiles.put(name.substring(0, name.lastIndexOf(".BIN")), file);
                } else if (name.endsWith(".IDX") && name.contains("TEXTURE")) {
                    idxFiles.put(name.substring(0, name.lastIndexOf(".IDX")), file);
                }
            }

            for (String baseName : permFiles.keySet()) {
                if (idxFiles.containsKey(baseName)) {
                    pairs.put(permFiles.get(baseName), idxFiles.get(baseName));
                }
            }
        }

        return pairs;
    }

    private static boolean runCommand(File jar, File model, File perm, File idx, File outputRoot, String subfolderName, boolean dump, String game) {
        try {
            List<String> cmd = new ArrayList<>(List.of(
                    "java", "-jar", jar.getAbsolutePath(),
                    "-g", game
            ));

            if (model != null) cmd.add(model.getAbsolutePath());
            cmd.add("-o");
            cmd.add(new File(outputRoot, subfolderName).getAbsolutePath());

            if (perm != null && idx != null) {
                cmd.add("-tps");
                cmd.add(perm.getAbsolutePath());
                cmd.add(idx.getAbsolutePath());
                if (dump) cmd.add("--dump-textures");
            }

            appendLog("Running: " + String.join(" ", cmd) + "\n");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                appendLog(line + "\n");
            }

            int exit = p.waitFor();
            appendLog("Exit Code: " + exit + "\n\n");

            return exit == 0;
        } catch (Exception ex) {
            appendLog("FAILED: " + ex.getMessage() + "\n\n");
            return false;
        }
    }

    private static void collectDirs(File root, List<File> list) {
        if (root.isDirectory()) {
            list.add(root);
            File[] sub = root.listFiles();
            if (sub != null) {
                for (File f : sub) {
                    if (f.isDirectory()) collectDirs(f, list);
                }
            }
        }
    }

    private static void showHelp() {
        String htmlContent =
            "<html><body style='font-family: Arial; font-size: 12px;'>" +
            "<p><b>UFG Exporter</b> is the tool this GUI wraps around.<br>" +
            "It was originally on GitHub, but now the only known download link is:</p>" +
            "<p><a href='https://mega.nz/file/8Y0DHLYA#Pa4Yv9FIkhyzBfZGBL7zIFGs956eOmlqVz-BQ4DChCg'>" +
            "https://mega.nz/file/8Y0DHLYA#Pa4Yv9FIkhyzBfZGBL7zIFGs956eOmlqVz-BQ4DChCg</a></p>" +
            "<p>You MUST download <b>ufg-exporter-0.1.jar</b> and place it anywhere.<br>" +
            "Then use the Browse button to select it in the GUI before exporting.</p>" +
            "<p><b>Features:</b></p>" +
            "<ul>" +
            "<li>Recursively finds models and textures in subfolders</li>" +
            "<li>Works with both ModNation Racers and LBP Karting</li>" +
            "<li>Option to export textures as PNGs</li>" +
            "<li>Progress tracking and detailed logging</li>" +
            "<li>Modern dark theme UI</li>" +
            "</ul></body></html>";

        JEditorPane help = new JEditorPane("text/html", htmlContent);
        help.setEditable(false);
        help.setBackground(null);
        help.setBorder(null);
        help.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        help.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    System.err.println("Failed to open link: " + ex.getMessage());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(help);
        scroll.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(null, scroll, "What is UFG?", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void savePaths() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("input", inputField.getText());
            props.setProperty("output", outputField.getText());
            props.setProperty("jar", jarField.getText());
            props.setProperty("textures", String.valueOf(includeTextures.isSelected()));
            props.setProperty("png", String.valueOf(dumpPNGs.isSelected()));
            props.setProperty("game", (String) gameSelector.getSelectedItem());
            props.store(out, "Saved Paths");
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    private static void loadSavedPaths() {
        if (!CONFIG_FILE.exists()) return;
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.load(in);
            inputField.setText(props.getProperty("input", ""));
            outputField.setText(props.getProperty("output", ""));
            jarField.setText(props.getProperty("jar", ""));
            includeTextures.setSelected(Boolean.parseBoolean(props.getProperty("textures", "true")));
            dumpPNGs.setSelected(Boolean.parseBoolean(props.getProperty("png", "true")));
            gameSelector.setSelectedItem(props.getProperty("game", "ModNation Racers"));
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }
    }
}
