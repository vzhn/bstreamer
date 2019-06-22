package me.vzhilin.mediaserver;

import me.vzhilin.mediaserver.server.RtspServer;
import org.bridj.Pointer;
import org.ffmpeg.avcodec.AVPacket;
import org.ffmpeg.avcodec.AvcodecLibrary;
import org.ffmpeg.avformat.AVFormatContext;
import org.ffmpeg.avformat.AVInputFormat;
import org.ffmpeg.avformat.AVStream;
import org.ffmpeg.avformat.AvformatLibrary;

public class Mediaserver {
    public static void main(String... argv) {
        new Mediaserver().start();
    }

    private void start() {
        new RtspServer().start();
    }
}
