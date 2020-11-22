package com.labredes.tf.redes;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.zip.CRC32;

public class ClientEnvia {

    static List<DatagramPacketInfo> packets = new ArrayList<>();

    static final int SLOW_START_MAX_DATA_PACKAGES = 8;

    public static void main(String args[]) throws Exception {
        System.out.println("\n---- Terminal ----\n");
        System.out.println("Aperte 1 para estabelecer conexão");
        System.out.println("Aperte 0 para sair");

        Scanner in = new Scanner(System.in);

        int input = in.nextInt();

        switch (input) {
            case 1:
                startConection();
                break;
            case 0:
                System.exit(0);
            default:
                break;
        }
    }


    public static void startConection() throws Exception {
        //estabelecendo que esse socket roda na porta 6789
        DatagramSocket clientSocket = new DatagramSocket(6789);

        InetAddress ipAddress = InetAddress.getByName("localhost");

        System.out.println("\nConexão estabelecida!");

        int port = 9876;

        createPackets();

        int listIterator = initializeSlowStart(packets, clientSocket, ipAddress, port, SLOW_START_MAX_DATA_PACKAGES);

        if (listIterator >= packets.size()) {
            System.out.println("ja enviou tudo, nao precisa do avoidance");
        } else {
            congestionAvoidance(packets, clientSocket, ipAddress, port, listIterator);
            System.out.println("\nConexão encerrada!");
        }
    }

    public static long calculaCRC(byte[] array) {
        CRC32 crc = new CRC32();

        crc.update(array);

        long valor = crc.getValue();

        //System.out.println("Valor crc: " + valor);

        return valor;
    }

    public static void createPackets() throws Exception {

        Scanner in = new Scanner(System.in);

        System.out.println("\n---- Escolha um arquivo ----\n");
        System.out.println("1 - case 1, 300 bytes");
        System.out.println("2 - case 1, ? bytes");

        int input = in.nextInt();

        String filepath = "C:\\Users\\Felipe\\Desktop\\Trabalho final redes\\TF-LabRedes-Redes\\tf-redes-envia\\input\\case1.txt";

        long valor = 1215645;

        //16 pacotes
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 1));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 2));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 3));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 4));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 5));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 6));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 7));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 8));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 9));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 10));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 11));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 12));


        File file = new File(filepath);
        try (Scanner s = new Scanner(file)) {

            int numeroSequencia = 0;
            while (s.hasNext()) {

                String conteudoPacote = "";
                for (int i = 0; i < 300; i++) {

                    if (s.hasNext()) {
                        conteudoPacote += s.next();
                    }
                }

                byte[] arrayBytes = conteudoPacote.getBytes();

                //System.out.println("tamanho do array: " + arrayBytes.length);

                long crc = calculaCRC(arrayBytes);

                packets.add(new DatagramPacketInfo(arrayBytes, crc, numeroSequencia));

                numeroSequencia++;

            }
        } catch (IOException e) {
            throw new FileNotFoundException("can't find file directory!");
        }

        in.close();
    }

    public static int initializeSlowStart(List<DatagramPacketInfo> packets, DatagramSocket socket, InetAddress ipAddress, int port, int packageLimit) throws Exception {
        int pacotesParaEnviar = 1;

        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, port);

        int listIterator = 0;

        int actualpackageLimit = 1;
        int packetCalculo = 1;
        while (packetCalculo != packageLimit) {
            packetCalculo *= 2;
            actualpackageLimit = actualpackageLimit * 2 + 1;
        }

        List<String> acksReceived = new ArrayList<String>();

        DatagramPacketInfo info;

        while (pacotesParaEnviar <= actualpackageLimit) {
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

                acksReceived.add("recebe response: " + response.getMessage() + ":" + response.getSeq());

                //recebeu o ACK, tudo ok
                if (response.getMessage() == "ACK" && response.getSeq() == info.getSeq() + 1) {
                    System.out.println("tudo ok");
                }

                //ACK duplicado, deu pau..
                if (response.getSeq() == info.getSeq()) {
                    System.out.println("recebeu um ack duplicado");
                }
            }

            for (int i = 0; i < acksReceived.size(); i++) {
                System.out.println(acksReceived.get(i));
            }

            acksReceived = new ArrayList<String>();

            pacotesParaEnviar = pacotesParaEnviar * 2 + 1;
        }

        return listIterator;
    }

    public static void congestionAvoidance(List<DatagramPacketInfo> packets, DatagramSocket socket, InetAddress ipAddress, int port, int listIterator) throws IOException {

        System.out.println("Cheguei no CongestionAvoidance");
        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, port);

        DatagramPacketInfo info;

        List<String> acksReceived = new ArrayList<String>();

        int quantPacketSend = 3;

        while (packets.size() != listIterator) {

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

                acksReceived.add("recebe response: " + response.getMessage() + ":" + response.getSeq());

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
