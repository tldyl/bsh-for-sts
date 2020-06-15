/*
 * Created by JFormDesigner on Sun Jun 14 18:23:03 CST 2020
 */

package demoMod.bshForSts.ui;

import java.awt.event.*;

import bsh.EvalError;
import bsh.Interpreter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import demoMod.bshForSts.BshForSts;
import demoMod.bshForSts.io.MyOutputStream;
import demoMod.bshForSts.io.MyPrintStream;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.BadLocationException;

/**
 * @author Temple9
 */
public class CommandWindow extends JFrame {
    private static final Interpreter interpreter = new Interpreter();
    private JFileChooser fileChooser = new JFileChooser();
    private static Map<String, UIStrings> uiStrings;
    private File currentFile;
    private PrintStream err;
    private PrintStream out;

    public CommandWindow() {
        initComponents();
        err = new MyPrintStream(new MyOutputStream(this.txtAreaResult));
        out = new MyPrintStream(new MyOutputStream(this.txtAreaResult));
        interpreter.setOut(out);
        interpreter.setErr(err);
        try {
            interpreter.set("cons", this);
            interpreter.eval("import demoMod.bshForSts.ui.CommandWindow;");
            interpreter.eval("import java.util.*;");
            interpreter.eval("import java.lang.reflect.*;");
            interpreter.eval("console = (CommandWindow) cons;");
            InputStreamReader sourceIn = new InputStreamReader(Gdx.files.internal("scripts/initial import.txt").read(), StandardCharsets.UTF_8);
            interpreter.eval(sourceIn, interpreter.getNameSpace(), "eval stream");
            interpreter.unset("cons");
        } catch (EvalError e) {
            e.printStackTrace(err);
        }
        BshForSts.commandWindow = this;
        this.initGlobalFont(new Font("", Font.BOLD, 10));
        for (Map.Entry<String, UIStrings> uiStringEntry : uiStrings.entrySet()) {
            try {
                Field field = CommandWindow.class.getDeclaredField(uiStringEntry.getKey());
                field.setAccessible(true);
                Object elem = field.get(this);
                if (elem instanceof JLabel) {
                    ((JLabel)elem).setText(uiStringEntry.getValue().TEXT[0]);
                } else if (elem instanceof AbstractButton) {
                    ((AbstractButton)elem).setText(uiStringEntry.getValue().TEXT[0]);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSb(SpriteBatch sb) {
        if (sb == null) return;
        try {
            interpreter.eval("import com.badlogic.gdx.graphics.g2d.*");
            interpreter.set("sb1", sb);
            interpreter.eval("sb = (SpriteBatch) sb1");
            interpreter.unset("sb1");
        } catch (EvalError evalError) {
            evalError.printStackTrace(err);
        }
    }

    private void initGlobalFont(Font font) {
        FontUIResource fontRes = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }
    }

    private void btnLoadActionPerformed(ActionEvent e) {
        int r = fileChooser.showOpenDialog(new JFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            txtScript.setText(currentFile.getAbsolutePath());
            txtAreaPreview.setText("");
            try {
                FileInputStream in = new FileInputStream(currentFile);
                byte[] buf = new byte[512];
                int len;
                while ((len = in.read(buf)) != -1) {
                    txtAreaPreview.append(new String(buf, 0, len, StandardCharsets.UTF_8));
                }
                in.close();
                BshForSts.lastLoadedFilePath = currentFile.getAbsolutePath();
                BshForSts.saveSettings();
            } catch (IOException e1) {
                e1.printStackTrace(err);
            }
        }
    }

    private void btnSaveActionPerformed(ActionEvent ev) {
        int r = 0;
        if (fileChooser.getSelectedFile() == null) {
            r = fileChooser.showSaveDialog(new JFrame());
        }
        if (r == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getPath();
            try {
                File f = createFile(path);
                FileOutputStream out = new FileOutputStream(f);

                out.write(txtAreaPreview.getText().getBytes());
                out.close();
                txtScript.setText(path);
                fileChooser.setSelectedFile(f);
                currentFile = f;
            } catch (Exception e) {
                e.printStackTrace(err);
            }
        }
    }

    private void btnNewActionPerformed(ActionEvent ev) {
        int r = fileChooser.showSaveDialog(new JFrame());
        if (r == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getPath();
            try {
                File f = new File(path);
                f.createNewFile();
                txtScript.setText(path);
                fileChooser.setSelectedFile(f);
                currentFile = f;
                BshForSts.lastLoadedFilePath = currentFile.getAbsolutePath();
                BshForSts.saveSettings();
            } catch (Exception e) {
                e.printStackTrace(err);
            }
        }
    }

    private File createFile(String path) throws IOException {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        return f;
    }

    private boolean txtAreaPreviewFocused = false;

    private void txtAreaPreviewKeyTyped(KeyEvent e) {
        if (txtAreaPreviewFocused) {
            int c = txtAreaPreview.getCaretPosition();
            int line = 1;
            int col = 1;
            try {
                line = txtAreaPreview.getLineOfOffset(c) + 1;
                col = c - txtAreaPreview.getLineStartOffset(line-1) + 1;
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
            lblLnNum.setText(Integer.toString(line));
            lblCoNum.setText(Integer.toString(col));
        }
    }

    private void txtAreaPreviewFocusGained(FocusEvent e) {
        txtAreaPreviewFocused = true;
    }

    private void txtAreaPreviewFocusLost(FocusEvent e) {
        txtAreaPreviewFocused = false;
    }

    private void btnClearActionPerformed(ActionEvent e) {
        txtAreaResult.setText("");
    }

    private void txtAreaPreviewMouseClicked(MouseEvent e) {
        txtAreaPreviewKeyTyped(null);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        txtScript = new JTextField();
        lblScript = new JLabel();
        btnLoad = new JButton();
        btnSave = new JButton();
        lblPreview = new JLabel();
        scrPreview = new JScrollPane();
        txtAreaPreview = new JTextArea();
        txtSingleLine = new JTextField();
        lblSingleLine = new JLabel();
        lblResult = new JLabel();
        scrResult = new JScrollPane();
        txtAreaResult = new JTextArea();
        separator1 = new JSeparator();
        btnExecute = new JButton();
        btnNew = new JButton();
        btnClear = new JButton();
        lblLn = new JLabel();
        lblLnNum = new JLabel();
        lblCo = new JLabel();
        lblCoNum = new JLabel();
        separator2 = new JSeparator();

        //======== this ========
        setTitle("BeanShell for STS");
        setFont(new Font("Arial", Font.PLAIN, 10));
        setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(null);

        //---- txtScript ----
        txtScript.setEditable(false);
        txtScript.setBackground(Color.white);
        contentPane.add(txtScript);
        txtScript.setBounds(20, 35, 475, 25);

        //---- lblScript ----
        lblScript.setText("Script Path:");
        contentPane.add(lblScript);
        lblScript.setBounds(new Rectangle(new Point(20, 15), lblScript.getPreferredSize()));

        //---- btnLoad ----
        btnLoad.setText("Load");
        btnLoad.addActionListener(e -> btnLoadActionPerformed(e));
        contentPane.add(btnLoad);
        btnLoad.setBounds(500, 35, 75, 25);

        //---- btnSave ----
        btnSave.setText("Save");
        btnSave.addActionListener(e -> btnSaveActionPerformed(e));
        contentPane.add(btnSave);
        btnSave.setBounds(580, 35, 75, 25);

        //---- lblPreview ----
        lblPreview.setText("Code Preview:");
        contentPane.add(lblPreview);
        lblPreview.setBounds(new Rectangle(new Point(20, 70), lblPreview.getPreferredSize()));

        //======== scrPreview ========
        {

            //---- txtAreaPreview ----
            txtAreaPreview.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    txtAreaPreviewKeyTyped(e);
                }
            });
            txtAreaPreview.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    txtAreaPreviewFocusGained(e);
                }
                @Override
                public void focusLost(FocusEvent e) {
                    txtAreaPreviewFocusLost(e);
                }
            });
            txtAreaPreview.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    txtAreaPreviewMouseClicked(e);
                }
            });
            scrPreview.setViewportView(txtAreaPreview);
        }
        contentPane.add(scrPreview);
        scrPreview.setBounds(20, 90, 715, 215);
        contentPane.add(txtSingleLine);
        txtSingleLine.setBounds(185, 480, 420, 25);

        //---- lblSingleLine ----
        lblSingleLine.setText("Single-Line Script:");
        contentPane.add(lblSingleLine);
        lblSingleLine.setBounds(20, 480, lblSingleLine.getPreferredSize().width, 25);

        //---- lblResult ----
        lblResult.setText("Result:");
        contentPane.add(lblResult);
        lblResult.setBounds(20, 325, 85, lblResult.getPreferredSize().height);

        //======== scrResult ========
        {

            //---- txtAreaResult ----
            txtAreaResult.setEditable(false);
            scrResult.setViewportView(txtAreaResult);
        }
        contentPane.add(scrResult);
        scrResult.setBounds(20, 375, 715, 100);
        contentPane.add(separator1);
        separator1.setBounds(20, 65, 715, 7);

        //---- btnExecute ----
        btnExecute.setText("Execute");
        contentPane.add(btnExecute);
        btnExecute.setBounds(610, 480, 125, 25);

        //---- btnNew ----
        btnNew.setText("New");
        btnNew.addActionListener(e -> btnNewActionPerformed(e));
        contentPane.add(btnNew);
        btnNew.setBounds(660, 35, 75, 25);

        //---- btnClear ----
        btnClear.setText("Clear");
        btnClear.addActionListener(e -> btnClearActionPerformed(e));
        contentPane.add(btnClear);
        btnClear.setBounds(20, 345, 85, 25);

        //---- lblLn ----
        lblLn.setText("Ln");
        contentPane.add(lblLn);
        lblLn.setBounds(new Rectangle(new Point(655, 305), lblLn.getPreferredSize()));

        //---- lblLnNum ----
        lblLnNum.setText("1");
        contentPane.add(lblLnNum);
        lblLnNum.setBounds(675, 305, 20, lblLnNum.getPreferredSize().height);

        //---- lblCo ----
        lblCo.setText("Co");
        contentPane.add(lblCo);
        lblCo.setBounds(new Rectangle(new Point(695, 305), lblCo.getPreferredSize()));

        //---- lblCoNum ----
        lblCoNum.setText("1");
        contentPane.add(lblCoNum);
        lblCoNum.setBounds(715, 305, 20, lblCoNum.getPreferredSize().height);
        contentPane.add(separator2);
        separator2.setBounds(20, 325, 715, 5);

        contentPane.setPreferredSize(new Dimension(750, 545));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        fileChooser.setCurrentDirectory(new File(BshForSts.lastLoadedFilePath));
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return true;
            }

            @Override
            public String getDescription() {
                return "All files(*.*)";
            }
        });
        txtAreaResult.setLineWrap(false);
        txtSingleLine.setText("");
        btnExecute.addActionListener(e -> execute());
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField txtScript;
    private JLabel lblScript;
    private JButton btnLoad;
    private JButton btnSave;
    private JLabel lblPreview;
    private JScrollPane scrPreview;
    private JTextArea txtAreaPreview;
    private JTextField txtSingleLine;
    private JLabel lblSingleLine;
    private JLabel lblResult;
    private JScrollPane scrResult;
    private JTextArea txtAreaResult;
    private JSeparator separator1;
    private JButton btnExecute;
    private JButton btnNew;
    private JButton btnClear;
    private JLabel lblLn;
    private JLabel lblLnNum;
    private JLabel lblCo;
    private JLabel lblCoNum;
    private JSeparator separator2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    private void execute() {
        txtAreaResult.setText("");
        if ("".equals(txtSingleLine.getText())) {
            out.println("Executing script...");
            try {
                File f = createFile("saves/BshForStsTempCode.tmp");
                FileOutputStream out = new FileOutputStream(f);

                out.write(txtAreaPreview.getText().getBytes());
                out.close();
                interpreter.source(f.getAbsolutePath());
            } catch (Exception | Error e) {
                out.println("\nAn error occurs when executing.");
                e.printStackTrace(err);
            }
        } else {
            out.println("Executing single-line script...");
            try {
                interpreter.eval(txtSingleLine.getText());
            } catch (Exception | Error evalError) {
                out.println("\nAn error occurs when executing.");
                evalError.printStackTrace(err);
            }
        }
        out.println("Done!");
    }

    public void print(String s) {
        out.print(s);
    }

    public void print(long l) {
        out.print(l);
    }

    public void println(String s) {
        out.println(s);
    }

    public void println(long l) {
        out.println(l);
    }

    static {
        uiStrings = new HashMap<>();
        Field[] fields = CommandWindow.class.getDeclaredFields();
        for (Field field : fields) {
            String s = BshForSts.makeID(field.getName());
            UIStrings uiString = CardCrawlGame.languagePack.getUIString(s);
            if (uiString != null) {
                uiStrings.put(field.getName(), uiString);
            }
        }
    }
}
