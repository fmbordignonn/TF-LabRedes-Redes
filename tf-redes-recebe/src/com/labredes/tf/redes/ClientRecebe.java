package com.labredes.tf.redes;

import java.io.*;
import java.net.*;
import java.util.Arrays;


class ClientRecebe {
    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        //estabelecendo que esse socket roda na porta 9876
        DatagramSocket serverSocket = new DatagramSocket(8080);

        byte[] receiveData;

        byte[] testData;

        while (true) {

            Thread.sleep(500);

            receiveData = new byte[10024];
            testData = new byte[1024];

            //Primeiro pacote, só pra dizer
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String receivedMessage = new String(receivePacket.getData());

            InetAddress IPAddress = receivePacket.getAddress();

            int port = receivePacket.getPort();

            System.out.println("Mensagem recebida: " + receivedMessage.replace("-", " - "));

            DatagramPacketInfo packetInfo = parseMessage(receivedMessage);

            //retirando delimiter da msg no client q envia o file (variavel FILE_END_DELIMITER_CHAR)
            for (int i = 0; i < packetInfo.getFileData().length; i++) {
                if(packetInfo.getFileData()[i] == 124){
                    packetInfo.getFileData()[i] = 0;
                }
            }

            DatagramPacket response = new DatagramPacket(testData, testData.length, IPAddress, port);

            packetInfo.setSeq(packetInfo.getSeq() + 1);

            response.setData(("ACK-" + packetInfo.getSeq()).getBytes());

            serverSocket.send(response);

        }
    }

    public static DatagramPacketInfo parseMessage(String message) {
        DatagramPacketInfo packetInfo = new DatagramPacketInfo();

        String[] splitMessage = message.split("-");

        packetInfo.setFileData(fazGambiarra(splitMessage[0]));
        packetInfo.setCRC(splitMessage[1]);
        packetInfo.setSeq(Integer.parseInt(splitMessage[2].trim()));

        return packetInfo;
    }

    //monta o fileData do DatagramPacketInfo
    public static byte[] fazGambiarra(String message) {
        String gambiarraInicial = message
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "");

        String[] gambiarraParte2 = gambiarraInicial.split(",");

        byte[] gambiarraFinal = new byte[gambiarraParte2.length];

        for (int i = 0; i < gambiarraParte2.length; i++) {
            gambiarraFinal[i] = Byte.parseByte(gambiarraParte2[i]);
        }

        return gambiarraFinal;
    }
}
