package com.labredes.tf.redes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientEnvia {

    public static void main(String args[]) throws Exception
    {
        System.out.println("Iniciou");

        // cria o stream do teclado
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        // declara socket cliente
        DatagramSocket clientSocket = new DatagramSocket();

        // obtem endere�o IP do servidor com o DNS
        InetAddress IPAddress = InetAddress.getByName("localhost");

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        // l� uma linha do teclado
        String sentence = inFromUser.readLine();
        sendData = sentence.getBytes();

        // cria pacote com o dado, o endere�o do server e porta do servidor
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

        //envia o pacote
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        clientSocket.receive(receivePacket);

        System.out.println("Chegou mensagem: " + new String(receivePacket.getData()));

        // fecha o cliente
        //clientSocket.close();
    }
}
