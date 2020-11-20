package com.labredes.tf.redes;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientEnvia {

    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        //estabelecendo que esse socket roda na porta 6789
        DatagramSocket clientSocket = new DatagramSocket(6789);

        InetAddress ipAddress = InetAddress.getByName("localhost");
        int port = 9876;
        final int SLOW_START_MAX_DATA_PACKAGES = 8;
        List<DatagramPacketInfo> packets = new ArrayList<>();

        //16 pacotes
        packets.add(new DatagramPacketInfo("abcd".getBytes(), "CRC", 2));
        packets.add(new DatagramPacketInfo("2".getBytes(), "CRC", 3));
        packets.add(new DatagramPacketInfo("3".getBytes(), "CRC", 4));
        packets.add(new DatagramPacketInfo("4".getBytes(), "CRC", 5));
        packets.add(new DatagramPacketInfo("5".getBytes(), "CRC", 6));
        packets.add(new DatagramPacketInfo("6".getBytes(), "CRC", 7));
        packets.add(new DatagramPacketInfo("7".getBytes(), "CRC", 8));
        packets.add(new DatagramPacketInfo("8".getBytes(), "CRC", 9));
        packets.add(new DatagramPacketInfo("9".getBytes(), "CRC", 10));
        packets.add(new DatagramPacketInfo("10".getBytes(), "CRC", 11));
        packets.add(new DatagramPacketInfo("11".getBytes(), "CRC", 12));
        packets.add(new DatagramPacketInfo("12".getBytes(), "CRC", 13));
        packets.add(new DatagramPacketInfo("13".getBytes(), "CRC", 14));
        packets.add(new DatagramPacketInfo("14".getBytes(), "CRC", 15));
        packets.add(new DatagramPacketInfo("15".getBytes(), "CRC", 16));
        packets.add(new DatagramPacketInfo("16".getBytes(), "CRC", 17));


        // cria o stream do teclado para ler caminho do arquivo
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Digite o caminho do arquivo");
        byte[] filePath = inFromUser.readLine().getBytes();

        //comentado pq ainda n tamo fragmentando arquivo
//        Path path = Paths.get(filePath);
//
//        String data = Files.readAllLines(path).get(0);
//
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

        int listIterator = initializeSlowStart(packets, clientSocket, ipAddress, port, SLOW_START_MAX_DATA_PACKAGES);

        CongestionAvoidance(packets, clientSocket, ipAddress, port, listIterator);


//        DatagramPacketInfo sendData = new DatagramPacketInfo(filePath, "CRC", 1);
//
//        String message = Arrays.toString(sendData.getFileData()) + "-" + sendData.getCRC() + "-" + sendData.getSeq();
//
//        System.out.println("mensagem pra enviar: " + message);
//
//        byte[] packetData = message.getBytes();
//
//        DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, ipAddress, port);
//
//        clientSocket.send(sendPacket);
//
//        byte[] receiveData = new byte[1024];
//        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//
//        clientSocket.receive(receivePacket);
//
//        System.out.println("Chegou mensagem: " + new String(receivePacket.getData()));

        // fecha o cliente
        //clientSocket.close();
    }

    public static int initializeSlowStart(List<DatagramPacketInfo> packets, DatagramSocket socket, InetAddress ipAddress, int port, int packageLimit) throws Exception {
        int pacotesParaEnviar = 1;

        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, port);

        int listIterator = 0;

        DatagramPacketInfo info;

        while (pacotesParaEnviar <= packageLimit) {
            for (listIterator = listIterator; listIterator < pacotesParaEnviar; listIterator++) {
                try {
                    info = packets.get(listIterator);
                } catch (Exception ex) {
                    //acabou de iterar, enviou tudo
                    break;
                }

                String message = Arrays.toString(info.getFileData()) + "-" + info.getCRC() + "-" + info.getSeq();

                System.out.println("enviando mensagem: " + message);

                byte[] packetData = message.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, ipAddress, port);

                socket.send(sendPacket);

                socket.receive(receivePacket);

                DatagramPacketResponse response = parseMessage(receivePacket);

                //recebeu o ACK, tudo ok
                if (response.getMessage() == "ACK" && response.getSeq() == info.getSeq() + 1) {
                    System.out.println("tudo ok");
                }

                //ACK duplicado, deu pau..
                if (response.getSeq() == info.getSeq()) {
                    System.out.println("recebeu um ack duplicado");
                }
            }

            pacotesParaEnviar *= 2;
        }

        return listIterator;
    }

    public static void CongestionAvoidance(List<DatagramPacketInfo> packets, DatagramSocket socket, InetAddress ipAddress, int port, int listIterator) throws IOException {

        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, port);

        DatagramPacketInfo info;

        int quantPacketSend = 3;

        while(packets.size() != listIterator) {

            for (int i = 0; i < quantPacketSend; i++) {

                try {
                    info = packets.get(listIterator);
                } catch (Exception ex) {
                    //acabou de iterar, enviou tudo
                    break;
                }

                String message = Arrays.toString(info.getFileData()) + "-" + info.getCRC() + "-" + info.getSeq();

                System.out.println("enviando mensagem: " + message);

                byte[] packetData = message.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, ipAddress, port);

                socket.send(sendPacket);

                socket.receive(receivePacket);

                DatagramPacketResponse response = parseMessage(receivePacket);

                //recebeu o ACK, tudo ok
                if (response.getMessage() == "ACK" && response.getSeq() == info.getSeq() + 1) {
                    System.out.println("tudo ok");
                }

                //ACK duplicado, deu pau..
                if (response.getSeq() == info.getSeq()) {
                    System.out.println("recebeu um ack duplicado");
                }

                listIterator++;
            }

            quantPacketSend++;
        }
    }

    public static DatagramPacketResponse parseMessage(DatagramPacket message) {
        String[] split = new String(message.getData()).split("-");

        return new DatagramPacketResponse(split[0], Integer.parseInt(split[1].trim()));
    }
}
