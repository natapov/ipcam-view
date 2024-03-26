package com.github.niqdev.mjpeg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class MjpegInputStream extends DataInputStream {
    //todo add initialization once we know frame size
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 150000;
    private final byte[] SOI_MARKER = {(byte) 0xFF, (byte) 0xD8};
    private final byte[] EOF_MARKER = {(byte) 0xFF, (byte) 0xD9};
    public byte[] frameBuffer = new byte[FRAME_MAX_LENGTH];
    public byte[] headerBuffer = new byte[HEADER_MAX_LENGTH];
    private final String CONTENT_LENGTH = "Content-Length";
    // no more accessible
    MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
    }
    private int getEndOfSequence(DataInputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for (int i = 0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if (c == sequence[seqIndex]) {
                seqIndex++;
                if (seqIndex == sequence.length) {
                    return i + 1;
                }
            } else {
                seqIndex = 0;
            }
        }
        return -1;
    }
    private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {
        int end = getEndOfSequence(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }
    private int parseContentLength(byte[] headerBytes) throws IOException, IllegalArgumentException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }
    int readMjpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH + HEADER_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        int contentLength;
        reset();
        readFully(headerBuffer, 0, headerLen);
        try {
            contentLength = parseContentLength(headerBuffer);
        } catch (IllegalArgumentException iae) {
            contentLength = getEndOfSequence(this, EOF_MARKER);
        }
        //todo catch exceptions
        readFully(frameBuffer, 0, contentLength);
        return contentLength;
    }
}