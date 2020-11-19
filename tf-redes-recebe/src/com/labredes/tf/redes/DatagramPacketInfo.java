package com.labredes.tf.redes;

public class DatagramPacketInfo {

    public DatagramPacketInfo() {
    }

    public DatagramPacketInfo(byte[] fileData, String CRC, int seq) {
        this.fileData = fileData;
        this.CRC = CRC;
        this.seq = seq;
    }

    private byte[] fileData;

    private String CRC;

    private int seq;

    private int amountOfPacketsSent;

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getCRC() {
        return CRC;
    }

    public void setCRC(String CRC) {
        this.CRC = CRC;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }
}