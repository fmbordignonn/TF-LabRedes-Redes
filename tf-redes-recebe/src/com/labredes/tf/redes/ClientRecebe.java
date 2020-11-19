package com.labredes.tf.redes;

import java.io.*;
import java.net.*;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;


class ClientRecebe {
    public static void main(String args[])  throws Exception
    {
        System.out.println("Iniciou");

        // cria socket do servidor com a porta 9876
        DatagramSocket serverSocket = new DatagramSocket(9876);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        byte[] receiveData = new byte[1024];

        byte[] testData = new byte[1024];


        while(true)
        {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            serverSocket.receive(receivePacket);

            String receivedMessage = new String(receivePacket.getData());

            InetAddress IPAddress = receivePacket.getAddress();

            int port = receivePacket.getPort();

            System.out.println("Mensagem recebida: " + receivedMessage);

            DatagramPacket response = new DatagramPacket(testData, testData.length, IPAddress, port);

            response.setData(new byte[] { 'O', 'i', ' ', 'c', 'h', 'e', 'g', 'u', 'e', 'i'});

            serverSocket.send(response);

            //Gson gson = new Gson();
            //n quer funciona entao fodase
            //DatagramPacketInfo packetInfo = gson.fromJson(receivedMessage, DatagramPacketInfo.class);

            DatagramPacketInfo packetInfo = new DatagramPacketInfo();

            String[] message = receivedMessage.split("-");

            packetInfo.setFileData(fazGambiarra(message[0]));
            packetInfo.setCRC(message[1]);
            packetInfo.setSeq(Integer.parseInt(message[2].trim()));


            System.out.println("asva");
        }
    }

    public static byte[] fazGambiarra(String message){
        String gambiarraInicial = message.replace("[", "").replace("]", "").replace(" ", "");

        String[] gambiarraParte2 = gambiarraInicial.split(",");

        byte[] gambiarraFinal = new byte[gambiarraParte2.length];

        for (int i = 0; i < gambiarraParte2.length; i++) {
            gambiarraFinal[i] = Byte.parseByte(gambiarraParte2[i]);
        }

        return gambiarraFinal;
    }
}
