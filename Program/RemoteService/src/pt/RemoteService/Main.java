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

public class Main { // TODO

    public static void main(String[] args) throws RemoteException, NotBoundException {

        // recebe um argumento, ip do RMI registry
        // liga-se ao registry e pede a lista de serviços para obter todos os servidores


        // TODO depois de escolher o server tem-se outro menu para escolher o que se quer fazer

        String registryAddress = "localhost";
        if (args.length >= 1) {
            registryAddress = args[0];
        }

        Registry registry = LocateRegistry.getRegistry(registryAddress);
        // Can obtain list server like this : registry.list();
        // Then do : registry.lookup(nameObtainedInList);

        System.out.println("All remote services : " + Arrays.toString(registry.list())); // Aqui temos os vários servidores. podemos fazer uma lista a partir um menu a partir do que isto retorna

        Scanner scanner = new Scanner(System.in);
        // TODO fazer um menuzito para escolher entre os vários servers
        while (true) {


            System.out.println("Servers available:");
            String[] list = registry.list();
            for (int i = 0; i < list.length; i++) {
                String serverName = list[i];
                System.out.println(i + 1 + " - " + serverName);
            }
            System.out.print("-> ");
            int choice = Integer.parseInt(scanner.nextLine());

            RemoteService remoteService = (RemoteService) registry.lookup(list[choice - 1]);


            while (choice != 0) {
                System.out.println("0 - Go back");
                System.out.println("1 - Register new User");
                System.out.println("2 - Send message to all");
                System.out.println("3 - Registry observer");
                System.out.println("4 - Remove observer");
                System.out.print("-> ");
                choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 0 -> {
                        break;
                    }
                    case 1 -> {
						System.out.println("Username:");
                    	String name = scanner.nextLine();
						System.out.println("Password:");
						String pwd = scanner.nextLine();

						String result = remoteService.registerNewUser(new UserInfo(name, pwd));
						System.out.println(result);
					}


                }

            }
        }
    }


}
