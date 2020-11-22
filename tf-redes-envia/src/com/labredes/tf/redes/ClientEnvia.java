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

    public static void main(String args[]) throws Exception {
        Scanner in = new Scanner(System.in);

        int input;
        do {
            System.out.println("\n---- Terminal ----\n");
            System.out.println("Aperte 1 para estabelecer conexão");
            System.out.println("Aperte 0 para sair");
            input = in.nextInt();

            switch (input) {
                case 1: startConection();
                        break;

                default: break;
            }
        }while(input != 0);

        in.close();



    }


    public static void startConection() throws Exception {
        //estabelecendo que esse socket roda na porta 6789
        DatagramSocket clientSocket = new DatagramSocket(6789);

        InetAddress ipAddress = InetAddress.getByName("localhost");

        System.out.println("\nConexão estabelecida!");

        int port = 9876;
        final int SLOW_START_MAX_DATA_PACKAGES = 8;
        List<DatagramPacketInfo> packets = new ArrayList<>();

        DatagramPacketInfo pacote = new DatagramPacketInfo("abcd".getBytes(), 2, 2);

        long valor = 1215645;

        //16 pacotes
        packets.add(new DatagramPacketInfo("abcd".getBytes(), valor, 2));
        packets.add(new DatagramPacketInfo("2".getBytes(), valor, 3));
        packets.add(new DatagramPacketInfo("3".getBytes(), valor, 4));
        packets.add(new DatagramPacketInfo("4".getBytes(), valor, 5));
        packets.add(new DatagramPacketInfo("5".getBytes(), valor, 6));
        packets.add(new DatagramPacketInfo("abcd".getBytes(), valor, 2));
        packets.add(new DatagramPacketInfo("2".getBytes(), valor, 3));
        packets.add(new DatagramPacketInfo("3".getBytes(), valor, 4));
        packets.add(new DatagramPacketInfo("4".getBytes(), valor, 5));
        packets.add(new DatagramPacketInfo("5".getBytes(), valor, 6));
        packets.add(new DatagramPacketInfo("abcd".getBytes(), valor, 2));
        packets.add(new DatagramPacketInfo("2".getBytes(), valor, 3));
        packets.add(new DatagramPacketInfo("3".getBytes(), valor, 4));
        packets.add(new DatagramPacketInfo("4".getBytes(), valor, 5));
        packets.add(new DatagramPacketInfo("5".getBytes(), valor, 6));
        packets.add(new DatagramPacketInfo("abcd".getBytes(), valor, 2));
        packets.add(new DatagramPacketInfo("2".getBytes(), valor, 3));
        packets.add(new DatagramPacketInfo("3".getBytes(), valor, 4));
        packets.add(new DatagramPacketInfo("4".getBytes(), valor, 5));
        packets.add(new DatagramPacketInfo("5".getBytes(), valor, 6));



        // cria o stream do teclado para ler caminho do arquivo
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Digite o caminho do arquivo");
        byte[] filePath = inFromUser.readLine().getBytes();

        File file = new File("input/input.txt/");
        try (Scanner s = new Scanner(file)){

            int numeroSequencia = 0;
            while(s.hasNext()) {

                String conteudoPacote = "";
                for(int i=0; i < 300; i++) {

                    if(s.hasNext()) {
                        conteudoPacote += s.next();
                    }
                }

                byte[] arrayBytes = conteudoPacote.getBytes();

                //System.out.println("tamanho do array: " + arrayBytes.length);

                long crc = calculaCRC(arrayBytes);

                packets.add(new DatagramPacketInfo(arrayBytes, crc, numeroSequencia));

                numeroSequencia++;

            }
        }catch (FileNotFoundException e){
            throw new FileNotFoundException("input.txt not found in the program directory!");
        }

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

        System.out.println("\nConexão encerrada!");
    }

    public static long calculaCRC(byte[] array) {
        CRC32 crc = new CRC32();

        crc.update(array);

        long valor = crc.getValue();

        System.out.println("Valor crc: " + valor);

        return valor;
    }

    public static int initializeSlowStart(List<DatagramPacketInfo> packets, DatagramSocket socket, InetAddress ipAddress, int port, int packageLimit) throws Exception {
        int pacotesParaEnviar = 1;

        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, port);

        int listIterator = 0;

        int actualpackageLimit = 1;
        int packetCalculo = 1;
        while(packetCalculo != packageLimit) {
            packetCalculo *=2;
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

            for(int i = 0; i < acksReceived.size(); i++) {
                System.out.println(acksReceived.get(i));
            }

            acksReceived = new ArrayList<String>();

            pacotesParaEnviar = pacotesParaEnviar * 2 + 1;
        }

        return listIterator;
    }

    public static void CongestionAvoidance(List<DatagramPacketInfo> packets, DatagramSocket socket, InetAddress ipAddress, int port, int listIterator) throws IOException {

        System.out.println("Cheguei no CongestionAvoidance");
        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, port);

        DatagramPacketInfo info;

        List<String> acksReceived = new ArrayList<String>();

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
