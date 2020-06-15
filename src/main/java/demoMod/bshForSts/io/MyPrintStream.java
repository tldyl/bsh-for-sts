package demoMod.bshForSts.io;

import java.io.OutputStream;
import java.io.PrintStream;

public class MyPrintStream extends PrintStream {
    public MyPrintStream(OutputStream out) {
        super(out, true);
    }
}
