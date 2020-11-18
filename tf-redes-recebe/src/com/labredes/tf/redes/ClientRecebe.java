package com.labredes.tf.redes;

import java.io.*;
import java.net.*;

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

            Gson gson = new Gson();

            DatagramPacketInfo packetInfo = gson.fromJson(receivedMessage, DatagramPacketInfo.class);

            System.out.println("valor que chegou: " + packetInfo.getFileData());

        }
    }
}
