/**
 * dx-signer
 *
 * Copyright 2022 北京顶象技术有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dx.signer;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import dx.channel.ApkSigns;
import org.slf4j.impl.SimpleLogger;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UX {
    ExecutorService es = Executors.newSingleThreadExecutor();
    private JButton inBtn;
    private JTextField inPathTF;
    private JTabbedPane tabbedPane1;
    private JTextField ksPathTF;
    private JButton ksBtn;
    private JTextField outPathTF;
    private JButton signBtn;
    private JTextArea loggingTA;
    private JCheckBox SaveCB;
    private JComboBox<String> keyAliasCB;
    private JPasswordField keyPassPF;
    private JPasswordField ksPassPF;
    public JPanel top;
    private JProgressBar progressBar1;
    private JTextField channelPathTF;
    private JButton channelBtn;
    private JCheckBox v1SigningEnabledCheckBox;
    private JCheckBox v2SigningEnabledCheckBox;
    private JList<String> ksList;
    private final DefaultListModel<String> ksListModel = new DefaultListModel<>();
    private static final String cfg = "cfg.properties"; //最后配置文件名
    private boolean readOnly = false;
    private String inputFileName = "";

    public static void main(String[] args) throws IOException {

        System.setProperty(SimpleLogger.SHOW_LOG_NAME_KEY, "false");
        System.setProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, "false");

        if (args.length >= 1 && args[0].equals("sign")) {
            CommandLine.main(args);
            return;
        }

        JFrame frame = new JFrame("Apk签名&多渠道工具");
        frame.setContentPane(new UX().top);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        // make the frame half the height and width
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width1 = screenSize.width / 2;
        int height1 = screenSize.height / 2;


        width1 = 900;
        if (height1 < 600) {
            height1 = 600;
        }

        frame.setSize(width1, height1);

        // here's the part where i center the jframe on screen
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }


    public UX() {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView());
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        TransferHandler mTransferHandler = new TransferHandler() {
            @Override
            public boolean importData(JComponent comp, Transferable t) {
                try {
                    Object data = t.getTransferData(DataFlavor.javaFileListFlavor);
                    String filePath = data.toString();
                    if (filePath.startsWith("[")) {
                        filePath = filePath.substring(1);
                    }
                    if (filePath.endsWith("]")) {
                        filePath = filePath.substring(0, filePath.length() - 1);
                    }
                    //apk
                    if (comp == inPathTF && (filePath.endsWith(".apk") || filePath.endsWith(".aab"))) {
                        File file = new File(filePath);
                        setInput(file);
                        return true;
                    }
                    //密钥
                    if (comp == ksPathTF && (filePath.endsWith(".ks") || filePath.endsWith(".keystore")
                            || filePath.endsWith(".p12") || filePath.endsWith(".pfx") || filePath.endsWith(".jks"))) {
                        ksPathTF.setText(filePath);
                        return true;
                    }
                    //渠道列表
                    if (comp == channelPathTF && (filePath.endsWith(".txt"))) {
                        channelPathTF.setText(filePath);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    loggingTA.append("导入错误：" + e.getLocalizedMessage());
                }
                return false;
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (DataFlavor flavor : flavors) {
                    if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }
        };
        inPathTF.setTransferHandler(mTransferHandler);
        ksPathTF.setTransferHandler(mTransferHandler);
        channelPathTF.setTransferHandler(mTransferHandler);

        //app选择
        inBtn.addActionListener(e -> {
            String appPath = inPathTF.getText();
            setCurrentChoose(appPath, fileChooser);
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String s = f.getName().toLowerCase();
                    return f.isDirectory() || s.endsWith(".apk") || s.endsWith(".aab");
                }

                @Override
                public String getDescription() {
                    return "*.apk,*.aab";
                }
            });
            int result = fileChooser.showOpenDialog(inBtn);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                setInput(file);

            }
        });
        //ks选择
        ksBtn.addActionListener(e -> {
            String ksPath = ksPathTF.getText();
            setCurrentChoose(ksPath, fileChooser);
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String s = f.getName().toLowerCase();
                    return f.isDirectory() || s.endsWith(".ks") || s.endsWith(".keystore") || s.endsWith(".p12") || s.endsWith(".pfx") || s.endsWith(".jks");
                }

                @Override
                public String getDescription() {
                    return "*.ks, *.keystore, *.p12, *.pfx, *.jks";
                }
            });
            int result = fileChooser.showOpenDialog(ksBtn);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                ksPathTF.setText(file.getAbsolutePath());
            }
        });
        // 渠道按钮
        channelBtn.addActionListener(e -> {
            String channelPath = channelPathTF.getText();
            setCurrentChoose(channelPath, fileChooser);
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    String s = f.getName().toLowerCase();
                    return f.isDirectory() || (f.isFile() && s.endsWith(".txt"));
                }

                @Override
                public String getDescription() {
                    return "*.txt";
                }
            });
            int result = fileChooser.showOpenDialog(channelBtn);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                channelPathTF.setText(file.getAbsolutePath());
            }
        });
        // 签名按钮
        signBtn.addActionListener(e -> {
            String channelPath = channelPathTF.getText();

            String out = outPathTF.getText();

            if (channelPath != null && !channelPath.isEmpty()) {
                Path apkDir = CommandLine.detectOutDir(out);
                if (Files.exists(apkDir)) {
                    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(UX.this.top, "多渠道输出APK目录已经存在，是否覆盖:\n" + apkDir, "输出APK已经存在，是否覆盖", JOptionPane.OK_CANCEL_OPTION)) {
                        return;
                    }
                }
            } else {
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(UX.this.top, "输出APK已经存在，是否覆盖:\n" + out, "输出APK已经存在，是否覆盖", JOptionPane.OK_CANCEL_OPTION)) {
                    return;
                }
            }

            signBtn.setEnabled(false);

            String in = inPathTF.getText();

            String ksPass;
            String keyPass;
            try {
                ksPass = new String(ksPassPF.getPassword());
            } catch (NullPointerException ignore) {
                ksPass = "";
            }
            try {
                keyPass = new String(UX.this.keyPassPF.getPassword());
            } catch (NullPointerException ignore) {
                keyPass = null;
            }
            String keyAlias = (String) UX.this.keyAliasCB.getSelectedItem();
            Properties mConfig = new Properties();
            String ksPath0 = ksPathTF.getText();
            mConfig.put("ks", ksPath0);
            mConfig.put("in", in);
            mConfig.put("ks-key-alias", keyAlias);
            mConfig.put("in-filename", this.inputFileName);
            mConfig.put("out", this.outPathTF.getText());
            mConfig.put("channel-list", this.channelPathTF.getText());

            if (SaveCB.isSelected()) {
                mConfig.put("ks-pass", ksPass);
                mConfig.put("key-pass", keyPass);
            }
            if (!readOnly) {
                try {
                    Path configFile = getConfigPath(cfg);
                    try (BufferedWriter r = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                        mConfig.store(r, "#");
                    }
                    File ks = new File(ksPath0);
                    if (ks.exists()) {
                        String name = ks.getName() + ".properties";
                        Path ksConfigFile = getConfigPath(name);
                        try (BufferedWriter r = Files.newBufferedWriter(ksConfigFile, StandardCharsets.UTF_8)) {
                            mConfig.store(r, "#");
                        }
                        if (!ksListModel.contains(ks.getName())) {
                            ksListModel.addElement(ks.getName());
                        }
                    }
                } catch (IOException ignore) {
                }
            }

            loggingTA.setText("");

            String finalKsPass = ksPass;
            String finalKeyPass = keyPass;
            String pbOrg = progressBar1.getString();
            progressBar1.setString("签名中...");
            progressBar1.setStringPainted(true);
            progressBar1.setIndeterminate(true);

            Path ksPath = Paths.get(ksPath0);
            Path input = Paths.get(in);
            //线程池
            es.submit(() -> {
                try {
                    int result;

                    if (channelPath != null && !channelPath.isEmpty()) {
                        Path apkDir = CommandLine.detectOutDir(out);
                        result = SignWorker.signChannelApk(input, inputFileName,
                                apkDir,
                                Paths.get(channelPath),
                                ksPath, finalKsPass, keyAlias, finalKeyPass);
                        progressBar1.setIndeterminate(false);
                        progressBar1.setString(pbOrg);
                        if (result == 0) {
                            JOptionPane.showMessageDialog(UX.this.top, "多渠道成功, 输出APK文件夹\n" + apkDir);
                        } else {
                            JOptionPane.showMessageDialog(UX.this.top, "多渠道失败");
                        }
                    } else {
                        result = SignWorker.signApk(input, Paths.get(out), ksPath,
                                finalKsPass, keyAlias, finalKeyPass);
                        progressBar1.setIndeterminate(false);
                        progressBar1.setString(pbOrg);
                        if (result == 0) {
                            JOptionPane.showMessageDialog(UX.this.top, "签名成功, 输出APK\n" + out);
                        } else {
                            JOptionPane.showMessageDialog(UX.this.top, "签名失败");
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                signBtn.setEnabled(true);
            });

        });

        keyAliasCB.removeAllItems();
        keyAliasCB.addItem("{{auto}}");
        keyAliasCB.setSelectedItem("{{auto}}");
        keyAliasCB.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                KeyStore keyStore = null;
                try {
                    byte[] d = Files.readAllBytes(Paths.get(ksPathTF.getText()));
                    Set<String> keyList = new HashSet<>();
                    keyList.add(new String(ksPassPF.getPassword()));
                    keyStore = ApkSigns.loadKeyStore(d, keyList);
                } catch (Exception ignore) {
                    keyStore = null;
                }

                if (keyStore != null) {
                    keyAliasCB.removeAllItems();
                    keyAliasCB.addItem("{{auto}}");
                    try {
                        Enumeration<String> aliases = keyStore.aliases();
                        while (aliases.hasMoreElements()) {
                            String alias = aliases.nextElement();
                            keyAliasCB.addItem(alias);
                        }
                    } catch (Exception ignore) {
                    }
                    keyAliasCB.setSelectedItem("{{auto}}");
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });

        ksList.addListSelectionListener(e -> {
            String ksName = ksList.getSelectedValue();
            loadUIConfig(ksName + ".properties");
        });


        DefaultCaret caret = (DefaultCaret) loggingTA.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JTextAreaOutputStream.hijack(loggingTA);

        loadUIConfig(cfg);

        ksList.setModel(ksListModel);
        for (String config : getConfigs()) {
            ksListModel.addElement(config);
        }

        if (readOnly) {
            SaveCB.setEnabled(false);
            SaveCB.setSelected(false);
            channelBtn.setEnabled(false);
            channelPathTF.setEnabled(false);

            inBtn.setEnabled(false);
            inPathTF.setEnabled(false);

            if (ksPathTF.getText().length() > 0) {
                ksBtn.setEnabled(false);
                ksPathTF.setEnabled(false);
                keyAliasCB.setEnabled(false);
                ksPassPF.setEnabled(false);
                keyPassPF.setEnabled(false);
            }
            outPathTF.setEnabled(false);
        }

    }

    private void loadUIConfig(String fileName) {
        try {
            Path configFile = getConfigPath(fileName);
            Properties initConfig = CommandLine.load(configFile);

            readOnly = "true".equals(initConfig.getProperty("config-read-only", ""));
            ksPathTF.setText(initConfig.getProperty("ks", ""));

            String inPath = initConfig.getProperty("in", "");
            if (!inPath.isEmpty()) {
                setInput(new File(inPath), initConfig.getProperty("in-filename", ""));
            }
            String outPath = initConfig.getProperty("out", "");
            if (!outPath.isEmpty()) {
                outPathTF.setText(outPath);
            }
            ksPassPF.setText(initConfig.getProperty("ks-pass", ""));
            keyPassPF.setText(initConfig.getProperty("key-pass", ""));
            channelPathTF.setText(initConfig.getProperty("channel-list", ""));

            String s = initConfig.getProperty("ks-key-alias", "{{auto}}");
            if (!s.equals("{{auto}}") && !s.isEmpty()) {
                keyAliasCB.addItem(s);
                keyAliasCB.setSelectedItem(s);
            }

        } catch (IOException ignore) {
        }
    }

    private void setInput(File file) {
        setInput(file, null);
    }

    private void setInput(File file, String name) {
        if (name == null || name.isEmpty()) {
            name = file.getName();
        }
        this.inputFileName = name;
        inPathTF.setText(file.getAbsolutePath());

        int dot = inputFileName.lastIndexOf('.');
        String apkName = dot > 0 ? inputFileName.substring(0, dot) : inputFileName;
        String fileName = String.format("%s_sign.apk", apkName);
        File out = new File(file.getParent(), fileName);
        outPathTF.setText(out.toString());
    }

    /**
     * 获取配置文件
     *
     * @param fileName
     * @return
     */
    private static Path getConfigPath(String fileName) {
        Path HOME = Paths.get(".");
        Path configDir = HOME.resolve("etc");
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException ignore) {

            }
        }

        return configDir.resolve(fileName);
    }

    /**
     * 获取所有配置文件名
     *
     * @return
     */
    private List<String> getConfigs() {
        List<String> list = new ArrayList<>();
        File[] files = getConfigPath(cfg).getParent().toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                String s = file.getName();
                if (s.contains(".ks") || s.contains(".keystore") || s.contains(".p12") || s.contains(".pfx") || s.contains(".jks")) {
                    list.add(s.replace(".properties", ""));
                }
            }
        }
        return list;
    }

    /**
     * 设置选择目录
     *
     * @param recentPath
     * @param fileChooser
     */
    private void setCurrentChoose(String recentPath, JFileChooser fileChooser) {
        try {
            if (recentPath != null && !recentPath.isEmpty()) {
                File parent = Paths.get(recentPath).getParent().toFile();
                if (parent.exists()) {
                    fileChooser.setCurrentDirectory(parent);
                }
            }
        } catch (Exception ignored) {

        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        top = new JPanel();
        top.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1 = new JTabbedPane();
        top.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Apk签名 & 多渠道", panel1);
        inPathTF = new JTextField();
        inPathTF.setEditable(false);
        inPathTF.setText("");
        panel1.add(inPathTF, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        ksPathTF = new JTextField();
        panel1.add(ksPathTF, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("输入apk/aab");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        inBtn = new JButton();
        inBtn.setText("1.选择输入APK");
        panel1.add(inBtn, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("KeyStore");
        panel1.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ksBtn = new JButton();
        ksBtn.setText("2.选择KeyStore");
        panel1.add(ksBtn, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("KeyStore密码");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("3.输入KeyStore密码");
        panel1.add(label4, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outPathTF = new JTextField();
        panel1.add(outPathTF, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("输出apk/aab");
        panel1.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        SaveCB = new JCheckBox();
        SaveCB.setSelected(true);
        SaveCB.setText("保存密码");
        panel1.add(SaveCB, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ksPassPF = new JPasswordField();
        panel1.add(ksPassPF, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        channelPathTF = new JTextField();
        panel1.add(channelPathTF, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("渠道清单[可选]");
        panel1.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelBtn = new JButton();
        channelBtn.setText("选择渠道清单");
        panel1.add(channelBtn, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("更多设置项请选择高级");
        panel1.add(label7, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("高级", panel2);
        keyAliasCB = new JComboBox();
        panel2.add(keyAliasCB, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("KeyAlias");
        panel2.add(label8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keyPassPF = new JPasswordField();
        panel2.add(keyPassPF, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("证书密码");
        panel2.add(label9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("如果您的Keystore包含多个证书，或者您的证书密码与Keystore密码不同, 请设置下列参数");
        panel2.add(label10, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        v2SigningEnabledCheckBox = new JCheckBox();
        v2SigningEnabledCheckBox.setEnabled(false);
        v2SigningEnabledCheckBox.setSelected(true);
        v2SigningEnabledCheckBox.setText("--v2-signing-enabled");
        panel2.add(v2SigningEnabledCheckBox, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        v1SigningEnabledCheckBox = new JCheckBox();
        v1SigningEnabledCheckBox.setEnabled(false);
        v1SigningEnabledCheckBox.setSelected(true);
        v1SigningEnabledCheckBox.setText("--v1-signing-enabled");
        panel2.add(v1SigningEnabledCheckBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        top.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        signBtn = new JButton();
        signBtn.setText("         4.签名         ");
        panel3.add(signBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBar1 = new JProgressBar();
        progressBar1.setString("点击\"4.签名\"按钮开始  >>>>");
        progressBar1.setStringPainted(true);
        panel3.add(progressBar1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        top.add(scrollPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        loggingTA = new JTextArea();
        loggingTA.setDoubleBuffered(true);
        loggingTA.setEditable(true);
        loggingTA.setInheritsPopupMenu(true);
        loggingTA.setLineWrap(true);
        loggingTA.setText("   点击“4.签名”按钮开始签名...");
        scrollPane1.setViewportView(loggingTA);
        final JScrollPane scrollPane2 = new JScrollPane();
        top.add(scrollPane2, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        ksList = new JList();
        ksList.setDropMode(DropMode.ON);
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        ksList.setModel(defaultListModel1);
        ksList.setSelectionMode(1);
        scrollPane2.setViewportView(ksList);
        label1.setLabelFor(inPathTF);
        label2.setLabelFor(ksPathTF);
        label3.setLabelFor(ksPassPF);
        label4.setLabelFor(ksPassPF);
        label5.setLabelFor(outPathTF);
        label7.setLabelFor(ksPassPF);
        label8.setLabelFor(keyAliasCB);
        label9.setLabelFor(keyPassPF);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return top;
    }

}
