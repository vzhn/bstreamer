package me.vzhilin.mediaserver.client;

import java.util.ArrayList;
import java.util.List;

import me.vzhilin.mediaserver.client.rtsp.RtspCallback;
import me.vzhilin.mediaserver.client.rtsp.RtspConnection;
import me.vzhilin.mediaserver.client.rtsp.RtspConnectionHandler;
import me.vzhilin.mediaserver.client.rtsp.messages.DescribeReply;
import me.vzhilin.mediaserver.client.rtsp.messages.PlayReply;
import me.vzhilin.mediaserver.client.rtsp.messages.SetupReply;
import me.vzhilin.mediaserver.client.rtsp.messages.sdp.Media;
import me.vzhilin.mediaserver.client.rtsp.messages.sdp.SdpMessage;

class DefaultConnectionHandler implements RtspConnectionHandler {
    @Override
    public void onConnected(RtspConnection connection) {
        connection.describe(new DefaultDescribeCallback(connection));
    }

    @Override
    public void onDisconnected() {

    }

    private class DefaultDescribeCallback extends AbstractCallback<DescribeReply> {
        private DefaultDescribeCallback(RtspConnection connection) {
            super(connection);
        }

        @Override public void onSuccess(DescribeReply mesg) {
            SdpMessage sdp = mesg.getSdpMessage();
            List<Media> acceptedMedia = new ArrayList<>();

            for (Media media: sdp.getMediaDescriptions()) {
                if (media.isH264()) {
                    acceptedMedia.add(media);
                }
            }

            if (acceptedMedia.size() != 1) {
                throw new RuntimeException("must choose only one");
            }

            Media media = acceptedMedia.get(0);
            String control = concat(mesg.getContentBase(), media.getControl());

            RtspConnection connection = getConnection();
            connection.setup(control, new DefaultSetupCallback(connection, media));
        }

        private String concat(String contentBase, String control) {
            if (contentBase == null) {
                return control;
            }
            if (contentBase.endsWith("/")) {
                return contentBase + control;
            } else {
                return contentBase + "/" + control;
            }
        }
    }

    private class DefaultSetupCallback extends AbstractCallback<SetupReply> {
        private final Media media;

        private DefaultSetupCallback(RtspConnection connection, Media acceptedMedia) {
            super(connection);
            this.media = acceptedMedia;
        }

        @Override public void onSuccess(SetupReply mesg) {
            RtspConnection connection = getConnection();
            connection.play(mesg.getSession(), new DefaultPlayCallback(connection));
        }
    }

    /**
     * PLAY
     */
    private class DefaultPlayCallback extends AbstractCallback<PlayReply> {
        public DefaultPlayCallback(RtspConnection connection) {
            super(connection);
        }

        @Override public void onSuccess(PlayReply mesg) { }
    }

    private abstract static class AbstractCallback<T> implements RtspCallback<T> {
        private final RtspConnection connection;

        private AbstractCallback(RtspConnection connection) {
            this.connection = connection;
        }

        protected RtspConnection getConnection() {
            return connection;
        }

        @Override public void onError() {
            connection.disconnect();
        }
    }
}
