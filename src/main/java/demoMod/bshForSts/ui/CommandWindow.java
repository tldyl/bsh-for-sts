/*
 * Created by JFormDesigner on Sun Jun 14 18:23:03 CST 2020
 */

package demoMod.bshForSts.ui;

import java.awt.event.*;

import bsh.EvalError;
import bsh.Interpreter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import demoMod.bshForSts.BshForSts;
import demoMod.bshForSts.actions.ExecuteCommandAction;
import demoMod.bshForSts.io.MyOutputStream;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
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
    private List<AbstractCard> cards = new ArrayList<>();
    private List<AbstractRelic> relics = new ArrayList<>();

    public CommandWindow() {
        initComponents();
        try {
            err = new PrintStream(new MyOutputStream(this.txtAreaResult), true, "UTF-8");
            out = new PrintStream(new MyOutputStream(this.txtAreaResult), true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
                InputStreamReader reader = new InputStreamReader(in);
                char[] buf = new char[512];
                int len;
                while ((len = reader.read(buf)) != -1) {
                    txtAreaPreview.append(new String(buf, 0, len));
                }
                reader.close();
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
                File f = createFile(path);
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

    private void thisComponentResized(ComponentEvent e) {
        scrPreview.setSize(this.getWidth() - 50, scrPreview.getHeight());
        scrResult.setSize(this.getWidth() - 50, this.getHeight() - 460);
        txtScript.setSize(this.getWidth() - 285, txtScript.getHeight());
        txtSingleLine.setBounds(txtSingleLine.getX(), this.getHeight() - 80, this.getWidth() - 349, txtSingleLine.getHeight());
        btnLoad.setBounds(this.getWidth() - 260, btnLoad.getY(), btnLoad.getWidth(), btnLoad.getHeight());
        btnSave.setBounds(btnLoad.getX() + 80, btnSave.getY(), btnSave.getWidth(), btnSave.getHeight());
        btnNew.setBounds(btnSave.getX() + 80, btnNew.getY(), btnNew.getWidth(), btnNew.getHeight());
        btnExecute.setBounds(this.getWidth() - 155, this.getHeight() - 80, btnExecute.getWidth(), btnExecute.getHeight());
        lblLn.setBounds(this.getWidth() - 110, lblLn.getY(), lblLn.getWidth(), lblLn.getHeight());
        lblLnNum.setBounds(lblLn.getX() + 20, lblLnNum.getY(), lblLnNum.getWidth(), lblLnNum.getHeight());
        lblCo.setBounds(lblLnNum.getX() + 20, lblCo.getY(), lblCo.getWidth(), lblCo.getHeight());
        lblCoNum.setBounds(lblCo.getX() + 20, lblCoNum.getY(), lblCoNum.getWidth(), lblCoNum.getHeight());
        lblSingleLine.setBounds(lblSingleLine.getX(), this.getHeight() - 80, lblSingleLine.getWidth(), lblSingleLine.getHeight());
        separator1.setSize(this.getWidth() - 49, separator1.getHeight());
        separator2.setSize(this.getWidth() - 49, separator2.getHeight());
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
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                thisComponentResized(e);
            }
        });
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
        btnExecute.addActionListener(e -> BshForSts.actionList.add(new ExecuteCommandAction(this::execute)));
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

    private void execute(Void aVoid) {
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

    public String getCardIdByName(String name) {
        if (cards.size() == 0) {
            cards.addAll(CardLibrary.getAllCards());
        }
        for (AbstractCard card : cards) {
            if (card.name.equals(name)) {
                return card.cardID;
            }
        }
        return "";
    }

    public String getRelicIdByName(String name) {
        if (relics.size() == 0) {
            Field[] fields = RelicLibrary.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().getSimpleName().contains("ArrayList")) {
                    field.setAccessible(true);
                    try {
                        ArrayList<AbstractRelic> relics1 = (ArrayList) field.get(null);
                        for (AbstractRelic relic : relics1) {
                            if (!relics.contains(relic)) {
                                relics.add(relic);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        for (AbstractRelic relic : relics) {
            if (relic.name.equals(name)) {
                return relic.relicId;
            }
        }
        return "";
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
