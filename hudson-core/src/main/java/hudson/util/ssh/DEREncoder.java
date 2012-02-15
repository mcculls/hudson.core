/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/

package hudson.util.ssh;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import org.apache.commons.codec.binary.Base64;

/**
 * ASN.1 DER encoder.
 *
 * This is the binary packaging format used by OpenSSH key.
 *
 * @author Kohsuke Kawaguchi
 */
final class DEREncoder {

    private final DataOutputStream out;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public DEREncoder() {
        this.out = new DataOutputStream(baos);
    }

    public void reset() {
        baos.reset();
    }

    public byte[] toByteArray() {
        return baos.toByteArray();
    }

    /**
     * Converts that to base64 with new lines to make it 64-chars per line.
     */
    public String toBase64() {
        byte[] r = Base64.encodeBase64(toByteArray());

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < r.length; i++) {
            buf.append(r[i]);
            if (i % 64 == 63) {
                buf.append('\n');
            }
        }
        if (r.length % 64 != 0) {
            buf.append('\n');
        }
        return buf.toString();
    }

    public DEREncoder writeSequence(byte[] data, int offset, int length) throws IOException {
        out.write(0x30);
        writeLength(length);
        out.write(data, offset, length);
        return this;
    }

    public DEREncoder writeSequence(byte[] data) throws IOException {
        return writeSequence(data, 0, data.length);
    }

    public void writeLength(int len) throws IOException {
        if (len < 0x80) {
            // short form
            out.write(len);
            return;
        }

        // how many bytes do we need to store this length?
        int bytes = countByteLen(len);

        out.write(0x80 | bytes);
        for (int i = bytes - 1; i >= 0; i--) {
            out.write((len >> (8 * i)) & 0xFF);
        }
    }

    private int countByteLen(int len) {
        int bytes = 0;
        while (len > 0) {
            bytes++;
            len >>= 8;
        }
        return bytes;
    }

    public DEREncoder write(BigInteger i) throws IOException {
        out.write(0x02);
        byte[] bytes = i.toByteArray();
        writeLength(bytes.length);
        out.write(bytes);
        return this;
    }

    public DEREncoder write(BigInteger... ints) throws IOException {
        for (BigInteger i : ints) {
            write(i);
        }
        return this;
    }
}
