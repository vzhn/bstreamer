package me.vzhilin.bstreamer.client.rtsp.messages.sdp;

import java.io.IOException;
import java.util.Base64;

public final class SpropParameterSets {

    /** SPS */
    private final String spsStr;

    /** PPS */
    private final String ppsStr;

    /**
     * Конструктор
     * @param sprops sps и pps в формате base64
     */
    public SpropParameterSets(String sprops) {
        spsStr = sprops.split(",")[0];
        ppsStr = sprops.split(",")[1];
    }

    /**
     * @return Sequence parameter set
     * @throws IOException ошибкп
     */
    public byte[] getSps() throws IOException {
        return decode(spsStr);
    }

    /**
     * @return Picture parameter set
     * @throws IOException ошибка
     */
    public byte[] getPps() throws IOException {
        return decode(ppsStr);
    }

    /**
     * Декодирует base64
     * @param s строка в формате base64
     * @throws IOException ошибка
     * @return байты
     */
    private byte[] decode(String s) throws IOException {
        return Base64.getDecoder().decode(s);
    }
}
