package com.labredes.tf.redes;

import java.util.Arrays;
import java.util.Objects;

public class DatagramPacketInfo {

    public DatagramPacketInfo() {
    }

    public DatagramPacketInfo(byte[] fileData, long CRC, int seq) {
        this.fileData = fileData;
        this.CRC = CRC;
        this.seq = seq;
    }

    private byte[] fileData;

    private long CRC;

    private int seq;

    private int amountOfPacketsSent;

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public long getCRC() {
        return CRC;
    }

    public void setCRC(long CRC) {
        this.CRC = CRC;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatagramPacketInfo that = (DatagramPacketInfo) o;
        return CRC == that.CRC &&
                seq == that.seq &&
                amountOfPacketsSent == that.amountOfPacketsSent &&
                Arrays.equals(fileData, that.fileData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(CRC, seq, amountOfPacketsSent);
        result = 31 * result + Arrays.hashCode(fileData);
        return result;
    }
}