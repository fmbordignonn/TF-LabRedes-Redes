package com.labredes.tf.redes;

import com.google.gson.annotations.SerializedName;

public class DatagramPacketInfo {

    public DatagramPacketInfo(byte[] fileData, String CRC, int seq) {
        this.fileData = fileData;
        this.CRC = CRC;
        this.seq = seq;
    }

    @SerializedName("fileData")
    private byte[] fileData;

    @SerializedName("CRC")
    private String CRC;

    @SerializedName("seq")
    private int seq;

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