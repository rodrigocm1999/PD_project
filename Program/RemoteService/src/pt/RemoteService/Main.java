package pt.RemoteService;

import pt.Common.RemoteService.Observer;
import pt.Common.RemoteService.RemoteService;
import pt.Common.UserInfo;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws RemoteException, NotBoundException {

        // recebe um argumento, ip do RMI registry
        // liga-se ao registry e pede a lista de serviços para obter todos os servidores

        String registryAddress = "localhost";
        if (args.length >= 1) {
            registryAddress = args[0];
            System.out.println(registryAddress);
        }

        Registry registry = LocateRegistry.getRegistry(registryAddress);
        // Can obtain list server like this : registry.list();
        // Then do : registry.lookup(nameObtainedInList);

        System.out.println("All remote services : " + Arrays.toString(registry.list())); // Aqui temos os vários servidores. podemos fazer uma lista a partir um menu a partir do que isto retorna

        Scanner scanner = new Scanner(System.in);


        while (true) {
            System.out.println("0 - Exit \n1 - Refresh \nServers available:");

            String[] list = registry.list();
            for (int i = 0; i < list.length; i++) {
                String serverName = list[i];
                System.out.println(i + 2 + " - " + serverName);
            }
            System.out.print("-> ");
            int choice = Integer.parseInt(scanner.nextLine());

            if (choice == 0) {
                System.exit(0);
            } else if (choice == 1) {
                continue;
            }

            RemoteService remoteService = (RemoteService) registry.lookup(list[choice - 2]);
            System.out.println(remoteService);
            Observer observer = null;

            while (choice != 0) {
                System.out.println("0 - Go back");
                System.out.println("1 - Register new User");
                System.out.println("2 - Send message to all");
                System.out.println("3 - " + (observer == null ? "Register" : "Remove") + " observer");
                System.out.print("-> ");
                choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 0 -> {
                    }
                    case 1 -> {
                        System.out.println("Username/Name:");
                        String name = scanner.nextLine();
                        System.out.println("Password:");
                        String pwd = scanner.nextLine();

                        String result = remoteService.registerNewUser(new UserInfo(name, name, pwd, null));
                        System.out.println(result);
                    }
                    case 2 -> {
                        System.out.println("Username/Name:");
                        String name = scanner.nextLine();
                        System.out.println("Password:");
                        String pwd = scanner.nextLine();
                        System.out.println("Message Content:");
                        String content = scanner.nextLine();

                        String result = remoteService.sendMessageToAllConnected(name, pwd, content);
                        System.out.println(result);
                    }
                    case 3 -> {
                        if (observer == null) {
                            observer = new RMIObserver();
                            remoteService.addObserver(observer);
                            System.out.println("Observer added");
                        } else {
                            remoteService.removeObserver(observer);
                            observer = null;
                            System.out.println("Observer removed");
                        }
                    }
                }
            }
        }
    }


}
