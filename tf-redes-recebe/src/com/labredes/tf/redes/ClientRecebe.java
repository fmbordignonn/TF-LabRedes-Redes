package com.labredes.tf.redes;

import java.io.*;
import java.net.*;

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
            // declara o pacote a ser recebido
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // recebe o pacote do cliente
            serverSocket.receive(receivePacket);

            // pega os dados, o endereco IP e a porta do cliente
            // para poder mandar a msg de volta
            String sentence = new String(receivePacket.getData());
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            System.out.println("Mensagem recebida: " + sentence);

            DatagramPacket test = new DatagramPacket(testData, testData.length, IPAddress, port);

            test.setData(new byte[] { 'O', 'i', ' ', 'c', 'h', 'e', 'g', 'u', 'e', 'i'});

            serverSocket.send(test);
        }
    }
}
