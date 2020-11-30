package com.labredes.tf.redes;

import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


class ClientRecebe {

    static Map<Integer, byte[]> receivedFileData = new HashMap<>();
    static DatagramSocket serverSocket;
    static InetAddress ipAddress;
    static int port;

    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        //estabelecendo que esse socket roda na porta 9876
        serverSocket = new DatagramSocket(9876);

        while (true) {

            Thread.sleep(1000);

            DatagramPacketInfo packetInfo = receivePacket();

            int finalPacketSeqNumber = 0;

            if (packetInfo.isFinalPacket()) {

                receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());

                finalPacketSeqNumber = packetInfo.getSeq();

                int missingPacket = 0;

                do {
                    //ultimo packet recebido
                    //validar se recebeu o ultimo?

                    missingPacket = checkMissingPackets(finalPacketSeqNumber);

                    if (!(missingPacket == 0)) {
                        System.out.println("TÁ NO PACOTE FINAL, E ALGUM FOI PERDIDO NO MEIO DO CAMINHO, INICIANDO REQUISIÇAO DESSE PACOTE");

                        //missingPacket contém o pacote perdido
                        //server deve requisitar ele novamente
                        sendResponsePacket("ACK-" + missingPacket, ipAddress, port);

                        packetInfo = receivePacket();

                        if(packetInfo.getSeq() == missingPacket){
                            receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());
                            continue;
                        }
                    }

                } while (missingPacket != 0);

                sendResponsePacket("FINISHED", ipAddress, port);

                System.out.println("TERMINOU DE RECEBER TODOS PACOTES! DESCONECTANDO CLIENT....");

                //avaliar
                for (int i = 0; i < packetInfo.getFileData().length; i++) {
                    if (packetInfo.getFileData()[i] == 124) {
                        packetInfo.getFileData()[i] = 0;
                        System.out.println("pacote final");
                    }
                }
            }

            //insere no dicionario de pacotes recebidos os dados desse arquivo, com chave = seq
            //VALIDAR CRC AQUI ANTES DE ADD NO DICIONARIO,MAS FODASE POR ENQUANTO PQ TO FAZENDO FAST RETRANSMIT
            receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());

            int missingPacket = checkMissingPackets(packetInfo.getSeq());

            if (!(missingPacket == 0)) {
                //missingPacket contém o pacote perdido
                //server deve requisitar ele novamente
                sendResponsePacket("ACK-" + missingPacket, ipAddress, port);

                continue;
            }

            //retirando delimiter da msg no client q envia o file (variavel FILE_END_DELIMITER_CHAR)
            //MUDA ISSO DPS TA MT FEIO
            if (packetInfo.isFinalPacket()) {
                System.out.println("pacote final");
            }

            packetInfo.setSeq(packetInfo.getSeq() + 1);

            sendResponsePacket("ACK-" + packetInfo.getSeq(), ipAddress, port);

        }
    }

    public static void sendResponsePacket(String message, InetAddress ipAddress, int port) throws Exception {
        byte[] sendData = new byte[1024];

        DatagramPacket response = new DatagramPacket(sendData, sendData.length, ipAddress, port);

        response.setData(message.getBytes());

        serverSocket.send(response);
    }

    public static DatagramPacketInfo receivePacket() throws Exception {
        byte[] receiveData = new byte[10024];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);

        String receivedMessage = new String(receivePacket.getData());

        ipAddress = receivePacket.getAddress();

        port = receivePacket.getPort();

        System.out.println("Mensagem recebida: " + receivedMessage.replace("-", " - "));

        DatagramPacketInfo packetInfo = parseInputMessage(receivedMessage);

        return packetInfo;

    }

    /**
     * @param seqReceived sequence number recebido no pacote atual
     * @return 0 se nao há pacotes faltando, senao retorna o sequence number do pacote faltando
     */
    public static int checkMissingPackets(int seqReceived) {
        //pega todos os seqs antes do que chegou agora
        List<Integer> lista = receivedFileData.keySet()
                .stream()
                .filter(seq -> seq <= seqReceived)
                .collect(Collectors.toList());

        //verifica se os ultimos pacotes antes do que chegou agora chegaram ok..
        //ver se o size nao vai buga
        for (int seq = 1; seq <= seqReceived; seq++) {

            //se um pacote nao chegou, precisa pedir de novo
            //seq - 1 no lista.get pq os index começam em 0
            if (seq != lista.get(seq - 1)) {
                return seq;//faltou pacote aqui (i tá faltando)

                //se o pacote 1 e 2 tiverem faltando, ele vai pedir o 1 até conseguir receber, pra só depois pedir o 2, e assim em diante
                //como mexe direto no dicionario de dados recebidos, ele vai ficar pedindo o mesmo sempre até receber ele
            }
        }

        return 0;
    }

    public static DatagramPacketInfo parseInputMessage(String message) {
        DatagramPacketInfo packetInfo = new DatagramPacketInfo();

        String[] splitMessage = message.split("-");

        packetInfo.setFileData(fazGambiarra(splitMessage[0]));
        packetInfo.setCRC(Long.parseLong(splitMessage[1]));
        packetInfo.setSeq(Integer.parseInt(splitMessage[2].trim()));

        try {
            //Dentro de um try-catch pq só no ultimo pacote vem essa info preenchida como true
            packetInfo.setFinalPacket(Boolean.parseBoolean(splitMessage[3].trim()));
        } catch (IndexOutOfBoundsException ex) {
            //é o ultimo pacote...
        }


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
