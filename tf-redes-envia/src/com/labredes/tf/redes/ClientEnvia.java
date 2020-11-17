package com.labredes.tf.redes;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClientEnvia {

    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        // declara socket cliente
        DatagramSocket clientSocket = new DatagramSocket();

        // obtem endereco IP do servidor com o DNS
        InetAddress IPAddress = InetAddress.getByName("localhost");

        //serão enviados 300 bytes por pacote
        byte[] sendData = new byte[300];


                byte[] receiveData = new byte[1024];

        // cria o stream do teclado para ler caminho do arquivo
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        String filePath = inFromUser.readLine();

        Path path = Paths.get(filePath);

        String data = Files.readAllLines(path).get(0);

        List<byte[]> packetsData = new ArrayList<>();

        //1 caracter = 1 byte, então 300 bytes = 300 caracteres
        int endIndex = 300;

        //coloca na lista de dados de cada packet o que deve ser enviado, em ordem
        for (int startIndex = 0; startIndex < data.length(); startIndex += 300, endIndex += 300) {
            packetsData.add(
                    data.substring(startIndex, endIndex).getBytes()
            );
        }

        //aqui será feito o envio pacote a pacote ao socket
        for (int i = 0; i < packetsData.stream().count(); i++) {
            //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
        }

        DatagramPacketRequest teste = new DatagramPacketRequest();

        teste.setFileData("asdasdasdsad".getBytes());
        teste.setCRC("teste crc");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(teste);

        final byte[] pData = baos.toByteArray();

        //tava tentando envia um objeto pelo socket
        //https://stackoverflow.com/questions/10358981/how-to-send-object-over-datagram-socket
        //http://www.java2s.com/example/java/network/udp-send-object.html
        //http://www.java2s.com/example/java/network/receiving-a-datagram.html


        sendData = filePath.getBytes();

        // cria pacote com o dado, o endereco do server e porta do servidor
        DatagramPacket sendPacket = new DatagramPacket(pData, pData.length, IPAddress, 9876);

        //envia o pacote
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        clientSocket.receive(receivePacket);

        System.out.println("Chegou mensagem: " + new String(receivePacket.getData()));

        // fecha o cliente
        //clientSocket.close();
    }
}
