/*******************************************************************************
 *
 * Copyright (c) 2004-2010, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *
 *
 *******************************************************************************/ 

package hudson.console;

import hudson.MarkupText;
import org.apache.commons.io.output.ProxyWriter;
import org.kohsuke.stapler.framework.io.WriterOutputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to convert plain text console output (as byte sequence) + embedded
 * annotations into HTML (as char sequence), by using {@link ConsoleAnnotator}.
 *
 * @param <T> Context type.
 * @author Kohsuke Kawaguchi
 * @since 1.349
 */
public class ConsoleAnnotationOutputStream<T> extends LineTransformationOutputStream {

    private final Writer out;
    private final T context;
    private ConsoleAnnotator<T> ann;
    /**
     * Reused buffer that stores char representation of a single line.
     */
    private final LineBuffer line = new LineBuffer(256);
    /**
     * {@link OutputStream} that writes to {@link #line}.
     */
    private final WriterOutputStream lineOut;

    /**
     *
     */
    public ConsoleAnnotationOutputStream(Writer out, ConsoleAnnotator<? super T> ann, T context, Charset charset) {
        this.out = out;
        this.ann = ConsoleAnnotator.cast(ann);
        this.context = context;
        this.lineOut = new WriterOutputStream(line, charset);
    }

    public ConsoleAnnotator getConsoleAnnotator() {
        return ann;
    }

    /**
     * Called after we read the whole line of plain text, which is stored in
     * {@link #buf}. This method performs annotations and send the result to
     * {@link #out}.
     */
    protected void eol(byte[] in, int sz) throws IOException {
        line.reset();
        final StringBuffer strBuf = line.getStringBuffer();

        int next = ConsoleNote.findPreamble(in, 0, sz);

        List<ConsoleAnnotator<T>> annotators = null;

        { // perform byte[]->char[] while figuring out the char positions of the BLOBs
            int written = 0;
            while (next >= 0) {
                if (next > written) {
                    lineOut.write(in, written, next - written);
                    lineOut.flush();
                    written = next;
                } else {
                    assert next == written;
                }

                // character position of this annotation in this line
                final int charPos = strBuf.length();

                int rest = sz - next;
                ByteArrayInputStream b = new ByteArrayInputStream(in, next, rest);

                try {
                    final ConsoleNote a = ConsoleNote.readFrom(new DataInputStream(b));
                    if (a != null) {
                        if (annotators == null) {
                            annotators = new ArrayList<ConsoleAnnotator<T>>();
                        }
                        annotators.add(new ConsoleAnnotator<T>() {
                            public ConsoleAnnotator annotate(T context, MarkupText text) {
                                return a.annotate(context, text, charPos);
                            }
                        });
                    }
                } catch (IOException e) {
                    // if we failed to resurrect an annotation, ignore it.
                    LOGGER.log(Level.FINE, "Failed to resurrect annotation", e);
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.FINE, "Failed to resurrect annotation", e);
                }

                int bytesUsed = rest - b.available(); // bytes consumed by annotations
                written += bytesUsed;


                next = ConsoleNote.findPreamble(in, written, sz - written);
            }
            // finish the remaining bytes->chars conversion
            lineOut.write(in, written, sz - written);

            if (annotators != null) {
                // aggregate newly retrieved ConsoleAnnotators into the current one.
                if (ann != null) {
                    annotators.add(ann);
                }
                ann = ConsoleAnnotator.combine(annotators);
            }
        }

        lineOut.flush();
        MarkupText mt = new MarkupText(strBuf.toString());
        if (ann != null) {
            ann = ann.annotate(context, mt);
        }
        out.write(mt.toString(true)); // this perform escapes
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }

    /**
     * {@link StringWriter} enhancement that's capable of shrinking the buffer
     * size.
     *
     * <p> The problem is that {@link StringBuffer#setLength(int)} doesn't
     * actually release the underlying buffer, so for us to truncate the buffer,
     * we need to create a new {@link StringWriter} instance.
     */
    private static class LineBuffer extends ProxyWriter {

        private LineBuffer(int initialSize) {
            super(new StringWriter(initialSize));
        }

        private void reset() {
            StringBuffer buf = getStringBuffer();
            if (buf.length() > 4096) {
                out = new StringWriter(256);
            } else {
                buf.setLength(0);
            }
        }

        private StringBuffer getStringBuffer() {
            StringWriter w = (StringWriter) out;
            return w.getBuffer();
        }
    }
    private static final Logger LOGGER = Logger.getLogger(ConsoleAnnotationOutputStream.class.getName());
}
