import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

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
        JFrame frame = new JFrame("ModNation Exporter");
        frame.setSize(700, 540);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // LOGO from inside JAR
        JPanel topPanel = new JPanel(new BorderLayout());
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
        inputField = new JTextField(40);
        outputField = new JTextField(40);
        jarField = new JTextField(40);
        gameSelector = new JComboBox<>(new String[]{"ModNation Racers", "LBP Karting"});

        mainPanel.add(makeRow("Input Folder:", inputField, e -> chooseDirectory(inputField)));
        mainPanel.add(makeRow("Output Folder:", outputField, e -> chooseDirectory(outputField)));
        mainPanel.add(makeRow("ufg-exporter-0.1.jar:", jarField, e -> chooseFile(jarField)));
        mainPanel.add(makeRow("Game:", gameSelector));

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        includeTextures = new JCheckBox("Include textures");
        dumpPNGs = new JCheckBox("Dump textures as PNG");
        includeTextures.setSelected(true);
        dumpPNGs.setSelected(true);
        checkPanel.add(includeTextures);
        checkPanel.add(dumpPNGs);
        mainPanel.add(checkPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runButton = new JButton("Run Export");
        JButton helpButton = new JButton("What’s UFG?");
        runButton.setPreferredSize(new Dimension(120, 25));
        helpButton.setPreferredSize(new Dimension(120, 25));
        buttonPanel.add(runButton);
        buttonPanel.add(helpButton);
        mainPanel.add(buttonPanel);

        runButton.addActionListener(e -> runExport());
        helpButton.addActionListener(e -> showHelp());

        frame.add(mainPanel, BorderLayout.CENTER);

        // BOTTOM: log + credit
        JPanel bottomPanel = new JPanel(new BorderLayout());
        log = new JTextArea(8, 60);
        log.setFont(new Font("Monospaced", Font.PLAIN, 11));
        log.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(log);
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel credit = new JLabel("Made by Clickbate");
        credit.setFont(new Font("Arial", Font.PLAIN, 9));
        credit.setHorizontalAlignment(JLabel.LEFT);
        bottomPanel.add(credit, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        loadSavedPaths();
        frame.setVisible(true);
    }

    private static JPanel makeRow(String label, JComponent field) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        panel.add(new JLabel(label));
        panel.add(field);
        return panel;
    }

    private static JPanel makeRow(String label, JTextField field, ActionListener browseAction) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        panel.add(new JLabel(label));
        panel.add(field);
        JButton browse = new JButton("Browse");
        browse.setPreferredSize(new Dimension(80, 22));
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
                File model = new File(dir, "CHARMODELPACKSTREAMING.BIN");

                if (model.exists()) {
                    log.append("✓ Found model in: " + dir.getAbsolutePath() + "\n");
                    log.append("→ Exporting model...\n");
                    runCommand(jar, model, null, null, outputDir, dir.getName() + "_MODEL_ONLY", false, game);
                    log.append("✔ Done exporting model.\n");
                    didSomething = true;
                    ranAny = true;
                }

                if (includeTextures.isSelected()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            String name = file.getName().toUpperCase();
                            if (name.endsWith(".PERM.BIN")) {
                                String base = name.replace(".PERM.BIN", "");
                                File idx = new File(dir, base + ".PERM.IDX");

                                if (idx.exists()) {
                                    log.append("✓ Found texture pair:\n");
                                    log.append("     PERM: " + file.getName() + "\n");
                                    log.append("     IDX : " + idx.getName() + "\n");
                                    log.append("→ Exporting textures...\n");
                                    runCommand(jar, model.exists() ? model : null, file, idx, outputDir, dir.getName() + "_" + base, dumpPNGs.isSelected(), game);
                                    log.append("✔ Done exporting textures.\n");
                                    didSomething = true;
                                    ranAny = true;
                                }
                            }
                        }
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
