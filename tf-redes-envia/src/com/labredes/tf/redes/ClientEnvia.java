package com.labredes.tf.redes;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientEnvia {

    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        // declara socket cliente
        DatagramSocket clientSocket = new DatagramSocket();

        // obtem endereco IP do servidor com o DNS
        InetAddress IPAddress = InetAddress.getByName("localhost");

        byte[] receiveData = new byte[1024];

        // cria o stream do teclado para ler caminho do arquivo
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Digite o caminho do arquivo");
        byte[] filePath = inFromUser.readLine().getBytes();

        //comentado pq ainda n tamo fragmentando
//        Path path = Paths.get(filePath);
//
//        String data = Files.readAllLines(path).get(0);
//
//        List<byte[]> packetsData = new ArrayList<>();
//
//        //1 caracter = 1 byte, então 300 bytes = 300 caracteres
//        int endIndex = 300;
//
//        //coloca na lista de dados de cada packet o que deve ser enviado, em ordem
//        for (int startIndex = 0; startIndex < data.length(); startIndex += 300, endIndex += 300) {
//            packetsData.add(
//                    data.substring(startIndex, endIndex).getBytes()
//            );
//        }
//
//        //aqui será feito o envio pacote a pacote ao socket
//        for (int i = 0; i < packetsData.stream().count(); i++) {
//            //DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
//        }

        DatagramPacketInfo sendData = new DatagramPacketInfo(filePath, "CRC", 1);

        Gson gson = new Gson();
        String json = gson.toJson(sendData);

        System.out.println("JSON formado: " + json);

        byte[] packetData = json.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, IPAddress, 9876);

        //envia o pacote
        clientSocket.send(sendPacket);

        //recebendo resposta
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        clientSocket.receive(receivePacket);

        System.out.println("Chegou mensagem: " + new String(receivePacket.getData()));

        // fecha o cliente
        //clientSocket.close();
    }
}
