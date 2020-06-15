package demoMod.bshForSts.io;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MyOutputStream extends OutputStream {
    private JTextComponent component;
    private byte[] buf = new byte[4096];
    private int len = 0;

    public MyOutputStream(JTextComponent c) {
        component = c;
    }

    @Override
    public void write(int b) {
        buf[len++] = (byte) b;
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        String s = new String(b, StandardCharsets.UTF_8).substring(off, len);
        Document doc = component.getDocument();
        if (doc != null) {
            try {
                doc.insertString(doc.getLength(), s, null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void flush() {
        write(buf, 0, len);
        len = 0;
    }
}
