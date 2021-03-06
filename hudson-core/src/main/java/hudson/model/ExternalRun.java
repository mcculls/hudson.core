/*******************************************************************************
 *
 * Copyright (c) 2004-2013 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi, Roy Varghese
 *
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.Proc;
import hudson.util.DecodingStream;
import hudson.util.DualOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * {@link Run} for {@link ExternalJob}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ExternalRun extends Run<ExternalJob, ExternalRun> 
    implements BuildNavigable {

    /**
     * Loads a run from a log file.
     */
    ExternalRun(ExternalJob owner, File runDir) throws IOException {
        super(owner, runDir);
    }

    /**
     * Creates a new run.
     */
    ExternalRun(ExternalJob project) throws IOException {
        super(project);
    }

    /**
     * Instead of performing a build, run the specified command, record the log
     * and its exit code, then call it a build.
     */
    public void run(final String[] cmd) {
        run(new Runner() {
            public Result run(BuildListener listener) throws Exception {
                Proc proc = new Proc.LocalProc(cmd, getEnvironment(listener), System.in, new DualOutputStream(System.out, listener.getLogger()));
                return proc.join() == 0 ? Result.SUCCESS : Result.FAILURE;
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });
    }

    /**
     * Instead of performing a build, accept the log and the return code from a
     * remote machine.
     *
     * <p> The format of the XML is:
     *
     * <pre><xmp>
     * <run>
     *  <log>...console output...</log>
     *  <result>exit code</result>
     * </run>
     * </xmp></pre>
     */
    @SuppressWarnings({"Since15"})
    public void acceptRemoteSubmission(final Reader in) throws IOException {
        final long[] duration = new long[1];
        run(new Runner() {
            private String elementText(XMLStreamReader r) throws XMLStreamException {
                StringBuilder buf = new StringBuilder();
                while (true) {
                    int type = r.next();
                    if (type == CHARACTERS || type == CDATA) {
                        buf.append(r.getTextCharacters(), r.getTextStart(), r.getTextLength());
                    } else {
                        return buf.toString();
                    }
                }
            }

            public Result run(BuildListener listener) throws Exception {
                PrintStream logger = null;
                try {
                    logger = new PrintStream(new DecodingStream(listener.getLogger()));

                    XMLInputFactory xif = XMLInputFactory.newInstance();
                    XMLStreamReader p = xif.createXMLStreamReader(in);

                    p.nextTag();    // get to the <run>
                    p.nextTag();    // get to the <log>

                    charset = p.getAttributeValue(null, "content-encoding");
                    while (p.next() != END_ELEMENT) {
                        int type = p.getEventType();
                        if (type == CHARACTERS || type == CDATA) {
                            logger.print(p.getText());
                        }
                    }
                    p.nextTag(); // get to <result>



                    Result r = Integer.parseInt(elementText(p)) == 0 ? Result.SUCCESS : Result.FAILURE;

                    p.nextTag();  // get to <duration> (optional)
                    if (p.getEventType() == START_ELEMENT
                            && p.getLocalName().equals("duration")) {
                        duration[0] = Long.parseLong(elementText(p));
                    }

                    return r;
                } finally {
                    IOUtils.closeQuietly(logger);
                }
            }

            public void post(BuildListener listener) {
                // do nothing
            }

            public void cleanUp(BuildListener listener) {
                // do nothing
            }
        });

        if (duration[0] != 0) {
            super.duration = duration[0];
            // save the updated duration
            save();
        }
    }

    @Override
    public BuildNavigator getBuildNavigator() {
        return null;
    }

    @Override
    public void setBuildNavigator(BuildNavigator buildNavigator) {
        // no-op. Let super classes take care of managing builds
        // for now.
    }
}
