package com.labredes.tf.redes;

import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


class ClientRecebe {

    static Map<Integer, byte[]> receivedFileData = new HashMap<>();
    static DatagramSocket serverSocket;


    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        //estabelecendo que esse socket roda na porta 9876
        serverSocket  = new DatagramSocket(9876);

        int lastSeqReceived = -1;

        byte[] receiveData;

        while (true) {

            Thread.sleep(1000);

            receiveData = new byte[10024];

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String receivedMessage = new String(receivePacket.getData());

            InetAddress IPAddress = receivePacket.getAddress();

            int port = receivePacket.getPort();

            System.out.println("Mensagem recebida: " + receivedMessage.replace("-", " - "));

            DatagramPacketInfo packetInfo = parseInputMessage(receivedMessage);

            //insere no dicionario de pacotes recebidos os dados desse arquivo, com chave = seq
            //VALIDAR CRC AQUI ANTES DE ADD NO DICIONARIO,MAS FODASE POR ENQUANTO PQ TO FAZENDO FAST RETRANSMIT
            receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());

            int missingPacket = checkMissingPackets(packetInfo.getSeq());

            if(!(missingPacket == 0)){
                //missingPacket contém o pacote perdido
                //server deve requisitar ele novamente
                sendResponsePacket(missingPacket, IPAddress, port);

                continue;
            }

            //retirando delimiter da msg no client q envia o file (variavel FILE_END_DELIMITER_CHAR)
            //MUDA ISSO DPS TA MT FEIO
            for (int i = 0; i < packetInfo.getFileData().length; i++) {
                if (packetInfo.getFileData()[i] == 124) {
                    packetInfo.getFileData()[i] = 0;
                    System.out.println("pacote final");
                }
            }

            packetInfo.setSeq(packetInfo.getSeq() + 1);

            sendResponsePacket(packetInfo.getSeq(), IPAddress, port);

            //recebeu pacote de um ack q tava duplicado

            //TEM QUE MUDAR O JEITO QUE IDENTIFICA ACK REPLICADO
            //da pra fazer uma validação "backwards" - recebe seq 3, entao verifica se o 1 e 2 ja estao preenchidos na lista ou
            //foram perdidos no limbo
            //se tiver um faltando, retorna os acks repetidos pra fechar 3 e reenvia,
            //vai fazer isso pra todos os itens que faltarem na lista
//            if (lastSeqReceived < packetInfo.getSeq() - 1 && lastSeqReceived != -1) {
//                int lostPacket = packetInfo.getSeq() - 1;
//
//                System.out.println("FALTOU PACOTE! - SEQ RECEBIDO [" + packetInfo.getSeq() + "] - PACKET QUE FALTA [" + lostPacket + "]");
//
//                //e se falhar mais de um em seguida?
//
//                receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());
//
//                DatagramPacket response = new DatagramPacket(sendData, sendData.length, IPAddress, port);
//
//                response.setData(("ACK-" +
//                        //necessario parenteses senao ele trata tudo como string. ex: retorna 71 ao invés de 8
//                        (packetInfo.getSeq())
//                ).getBytes());
//
//                serverSocket.send(response);
//                lastSeqReceived = packetInfo.getSeq();
//                continue;
//            }
//
//            //validaçao pra -1 pq é o valor inicial da variavel
//            //se o valor da ultima sequencia recebida for diferente da seq do pacote recebido agora -1, significa que faltou algum pacote
//            //no caminho.
//            //isso é o "fast retransmit"
//            if (lastSeqReceived != packetInfo.getSeq() - 1 && lastSeqReceived != -1) {
//                DatagramPacket response = new DatagramPacket(sendData, sendData.length, IPAddress, port);
//
//                response.setData(("ACK-" + lastSeqReceived).getBytes());
//
//                serverSocket.send(response);
//
//                continue;
//            }
//
//            lastSeqReceived = packetInfo.getSeq();

        }
    }

    public static void sendResponsePacket(int seq, InetAddress ipAddress, int port) throws Exception{
        byte[] sendData  = new byte[1024];

        DatagramPacket response = new DatagramPacket(sendData, sendData.length, ipAddress, port);

        response.setData(("ACK-" + seq).getBytes());

        serverSocket.send(response);
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
        for (int seq = 1; seq <= lista.size(); seq++) {
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
        packetInfo.setCRC(splitMessage[1]);
        packetInfo.setSeq(Integer.parseInt(splitMessage[2].trim()));

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
