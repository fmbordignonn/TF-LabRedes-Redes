package com.labredes.tf.redes;

import java.io.*;
import java.net.*;
import java.util.Arrays;


class ClientRecebe {
    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        //estabelecendo que esse socket roda na porta 9876
        DatagramSocket serverSocket = new DatagramSocket(9876);

        int lastSeqReceived = -1;

        byte[] receiveData;

        byte[] sendData;

        while (true) {

            Thread.sleep(500);

            receiveData = new byte[10024];
            sendData = new byte[1024];

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String receivedMessage = new String(receivePacket.getData());

            InetAddress IPAddress = receivePacket.getAddress();

            int port = receivePacket.getPort();

            System.out.println("Mensagem recebida: " + receivedMessage.replace("-", " - "));

            DatagramPacketInfo packetInfo = parseMessage(receivedMessage);

            //validaçao pra -1 pq é o valor inicial da variavel
            //se o valor da ultima sequencia recebida for diferente da seq do pacote recebido agora -1, significa que faltou algum pacote
            //no caminho

            //isso é o "fast retransmit"
            if(lastSeqReceived != packetInfo.getSeq() - 1 && lastSeqReceived != -1){
                DatagramPacket response = new DatagramPacket(sendData, sendData.length, IPAddress, port);

                response.setData(("ACK-" + lastSeqReceived).getBytes());

                serverSocket.send(response);

                continue;
            }

            lastSeqReceived = packetInfo.getSeq();

            //retirando delimiter da msg no client q envia o file (variavel FILE_END_DELIMITER_CHAR)
            for (int i = 0; i < packetInfo.getFileData().length; i++) {
                if(packetInfo.getFileData()[i] == 124){
                    packetInfo.getFileData()[i] = 0;
                }
            }

            packetInfo.setSeq(packetInfo.getSeq() + 1);

            DatagramPacket response = new DatagramPacket(sendData, sendData.length, IPAddress, port);

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
