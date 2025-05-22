import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import com.formdev.flatlaf.FlatDarkLaf;

public class ModnationExporterGUI {
    private static JTextField inputField;
    private static JTextField outputField;
    private static JTextField jarField;
    private static JCheckBox includeTextures;
    private static JCheckBox dumpPNGs;
    private static JTextArea log;
    private static JComboBox<String> gameSelector;
    private static JButton runButton;
    private static final File CONFIG_FILE = new File("modnation_exporter_config.properties");

    public static void main(String[] args) {
        // Set FlatLaf Dark Look and Feel
        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        JFrame frame = new JFrame("ModNation Exporter");
        frame.setSize(700, 540);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // LOGO from inside JAR
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        try {
            InputStream imgStream = ModnationExporterGUI.class.getResourceAsStream("/FIXED UFG.png");
            if (imgStream != null) {
                ImageIcon originalIcon = new ImageIcon(ImageIO.read(imgStream));
                Image scaledImage = originalIcon.getImage().getScaledInstance(320, 100, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                logoLabel.setHorizontalAlignment(JLabel.CENTER);
                topPanel.add(logoLabel, BorderLayout.CENTER);
            } else {
                System.out.println("Logo not found inside JAR.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        frame.add(topPanel, BorderLayout.NORTH);

        // CENTER
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        inputField = new JTextField(40);
        outputField = new JTextField(40);
        jarField = new JTextField(40);
        gameSelector = new JComboBox<>(new String[]{"ModNation Racers", "LBP Karting"});

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
        runButton.setPreferredSize(new Dimension(120, 30));
        helpButton.setPreferredSize(new Dimension(120, 30));
        buttonPanel.add(runButton);
        buttonPanel.add(helpButton);
        mainPanel.add(buttonPanel);

        runButton.addActionListener(e -> runExport());
        helpButton.addActionListener(e -> showHelp());

        frame.add(mainPanel, BorderLayout.CENTER);

        // BOTTOM: log + credit
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        log = new JTextArea(8, 60);
        log.setFont(new Font("Monospaced", Font.PLAIN, 11));
        log.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(log);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Export Log"));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel credit = new JLabel("Made by Clickbate");
        credit.setFont(new Font("Arial", Font.PLAIN, 9));
        credit.setHorizontalAlignment(JLabel.LEFT);
        credit.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        bottomPanel.add(credit, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        loadSavedPaths();
        
        // Force proper layout calculation
        frame.pack();
        frame.setSize(700, 540);  // Reset to desired size after pack
        frame.setLocationRelativeTo(null);  // Center on screen
        frame.setVisible(true);
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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(150, 25));
        panel.add(jLabel);
        panel.add(field);
        JButton browse = new JButton("Browse");
        browse.setPreferredSize(new Dimension(80, 25));
        browse.addActionListener(browseAction);
        panel.add(browse);
        return panel;
    }

    private static void chooseDirectory(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static void chooseFile(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static void runExport() {
        log.setText("");
        File inputDir = new File(inputField.getText());
        File outputDir = new File(outputField.getText());
        File jar = new File(jarField.getText());

        if (!inputDir.exists() || !outputDir.exists() || !jar.exists()) {
            log.append("ERROR: One or more paths are invalid.\n");
            return;
        }

        runButton.setEnabled(false);
        runButton.setText("Racing...");

        new Thread(() -> {
            String game = gameSelector.getSelectedItem().toString().toLowerCase().contains("karting") ? "lbpk" : "mnr";
            List<File> allDirs = new ArrayList<>();
            collectDirs(inputDir, allDirs);

            boolean ranAny = false;

            for (File dir : allDirs) {
                boolean didSomething = false;
                
                // Find all potential model files - look for any BIN file that might be a model
                List<File> potentialModels = findPotentialModels(dir);
                
                for (File model : potentialModels) {
                    log.append("✓ Found potential model: " + model.getName() + " in: " + dir.getAbsolutePath() + "\n");
                    log.append("→ Exporting model...\n");
                    runCommand(jar, model, null, null, outputDir, dir.getName() + "_" + model.getName(), false, game);
                    log.append("✔ Done exporting model.\n");
                    didSomething = true;
                    ranAny = true;
                }

                if (includeTextures.isSelected()) {
                    // Find all texture pairs (.PERM.BIN and .PERM.IDX)
                    Map<File, File> texturePairs = findTexturePairs(dir);
                    
                    for (Map.Entry<File, File> pair : texturePairs.entrySet()) {
                        File permFile = pair.getKey();
                        File idxFile = pair.getValue();
                        String baseName = permFile.getName().replace(".PERM.BIN", "");
                        
                        log.append("✓ Found texture pair:\n");
                        log.append("     PERM: " + permFile.getName() + "\n");
                        log.append("     IDX : " + idxFile.getName() + "\n");
                        log.append("→ Exporting textures...\n");
                        
                        // Use the first model if available, otherwise just export textures
                        File modelToUse = potentialModels.isEmpty() ? null : potentialModels.get(0);
                        runCommand(jar, modelToUse, permFile, idxFile, outputDir, dir.getName() + "_" + baseName, dumpPNGs.isSelected(), game);
                        log.append("✔ Done exporting textures.\n");
                        didSomething = true;
                        ranAny = true;
                    }
                }

                if (!didSomething) {
                    log.append("⨯ Skipped " + dir.getName() + " (no valid model or texture pair)\n");
                }

                log.append("\n");
            }

            if (!ranAny) log.append("No valid exports found.\n");

            savePaths();
            SwingUtilities.invokeLater(() -> {
                runButton.setEnabled(true);
                runButton.setText("Run Export");
            });
        }).start();
    }

    // Find all potential model files (any .BIN file that could be a model)
    private static List<File> findPotentialModels(File dir) {
        List<File> models = new ArrayList<>();
        File[] files = dir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                // Look for standard model file
                if (file.getName().equals("CHARMODELPACKSTREAMING.BIN")) {
                    models.add(file);
                    continue;  // No need to check other patterns for this file
                }
                
                // Look for any .BIN file that might be a model (not textures)
                String name = file.getName().toUpperCase();
                if (name.endsWith(".BIN") && 
                    !name.endsWith(".PERM.BIN") && 
                    !name.endsWith(".IDX") && 
                    !name.contains("TEXTURES")) {
                    // Potential model file
                    models.add(file);
                }
            }
        }
        
        return models;
    }

    // Find all texture pairs (.PERM.BIN and .PERM.IDX)
    private static Map<File, File> findTexturePairs(File dir) {
        Map<File, File> pairs = new HashMap<>();
        Map<String, File> permFiles = new HashMap<>();
        Map<String, File> idxFiles = new HashMap<>();
        
        File[] files = dir.listFiles();
        if (files != null) {
            // First collect all potential texture files
            for (File file : files) {
                String name = file.getName().toUpperCase();
                if (name.endsWith(".PERM.BIN")) {
                    String baseName = name.substring(0, name.lastIndexOf(".PERM.BIN"));
                    permFiles.put(baseName, file);
                } else if (name.endsWith(".PERM.IDX")) {
                    String baseName = name.substring(0, name.lastIndexOf(".PERM.IDX"));
                    idxFiles.put(baseName, file);
                } else if (name.endsWith(".BIN") && name.contains("TEXTURE")) {
                    // Special case for files with TEXTURE in the name
                    String baseName = name.substring(0, name.lastIndexOf(".BIN"));
                    permFiles.put(baseName, file);
                } else if (name.endsWith(".IDX") && name.contains("TEXTURE")) {
                    // Special case for files with TEXTURE in the name
                    String baseName = name.substring(0, name.lastIndexOf(".IDX"));
                    idxFiles.put(baseName, file);
                }
            }
            
            // Match pairs
            for (String baseName : permFiles.keySet()) {
                if (idxFiles.containsKey(baseName)) {
                    pairs.put(permFiles.get(baseName), idxFiles.get(baseName));
                }
            }
        }
        
        return pairs;
    }

    private static void runCommand(File jar, File model, File perm, File idx, File outputRoot, String subfolderName, boolean dump, String game) {
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

            log.append("Running: " + String.join(" ", cmd) + "\n");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line + "\n");
            }

            int exit = p.waitFor();
            log.append("Exit Code: " + exit + "\n\n");
        } catch (Exception ex) {
            log.append("FAILED: " + ex.getMessage() + "\n\n");
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
        JTextArea help = new JTextArea(
                "UFG Exporter is the tool this GUI wraps around.\n" +
                "It was originally on GitHub, but now the only known download link is:\n\n" +
                "https://mega.nz/file/8Y0DHLYA#Pa4Yv9FIkhyzBfZGBL7zIFGs956eOmlqVz-BQ4DChCg\n\n" +
                "You MUST download ufg-exporter-0.1.jar and place it anywhere.\n" +
                "Then use the Browse button to select it in the GUI before exporting.\n"
        );
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setEditable(false);
        help.setBackground(null);
        help.setBorder(null);

        JScrollPane scroll = new JScrollPane(help);
        scroll.setPreferredSize(new Dimension(500, 250));

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
        } catch (IOException ignored) {}
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
        } catch (IOException ignored) {}
    }
}