package pt.RemoteService;

import pt.Common.RemoteService.Observer;
import pt.Common.RemoteService.RemoteService;
import pt.Common.UserInfo;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
	
	public static class ServerService {
		public String name;
		public RemoteService remoteService;
		public Observer observer;
		
		public ServerService(String name, RemoteService remoteService) {
			this.name = name;
			this.remoteService = remoteService;
		}
	}
	
	public static void main(String[] args) throws RemoteException, NotBoundException {
		
		// recebe um argumento, ip do RMI registry
		// liga-se ao registry e pede a lista de serviços para obter todos os serviços dos servidores
		
		String registryAddress = "localhost";
		if (args.length >= 1) {
			registryAddress = args[0];
			System.out.println(registryAddress);
		}
		
		Registry registry = LocateRegistry.getRegistry(registryAddress);
		// Can obtain list server like this : registry.list();
		// Then do : registry.lookup(nameObtainedInList);
		
		System.out.println("All services from the servers: " + Arrays.toString(registry.list())); // Aqui temos os vários servidores. podemos fazer uma lista a partir um menu a partir do que isto retorna
		
		Map<String, ServerService> services = new HashMap<>();
		Scanner scanner = new Scanner(System.in);
		
		while (true) {
			System.out.println("\n0 - Exit \n1 - Refresh \nServers available:");
			
			String[] list = registry.list();
			for (int i = 0; i < list.length; i++) {
				String serverName = list[i];
				System.out.println(i + 2 + " - " + serverName);
			}
			int choice = getUserChoice(scanner);
			
			if (choice == 0) {
				System.exit(0);
			} else if (choice == 1) {
				continue;
			}
			
			int chosenIndex = choice - 2;
			if (chosenIndex >= list.length) {
				System.out.println("Invalid choice");
				continue;
			}
			
			try {
				String chosenService = list[chosenIndex];
				ServerService serverService = services.get(chosenService);
				
				if (serverService == null) {
					RemoteService temp = (RemoteService) registry.lookup(chosenService);
					serverService = new ServerService(chosenService, temp);
					services.put(chosenService, serverService);
				}
				
				RemoteService remoteService = serverService.remoteService;
				System.out.println("Connected to server with service name : " + serverService.name);
				
				while (choice != 0) {
					System.out.println("\nRun Action on service : " + serverService.name);
					System.out.println("0 - Go back");
					System.out.println("1 - Register new User");
					System.out.println("2 - Send message to all");
					System.out.println("3 - " + (serverService.observer == null ? "Register" : "Remove") + " observer");
					choice = getUserChoice(scanner);
					switch (choice) {
						case 0 -> {}
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
							if (serverService.observer == null) {
								serverService.observer = new RMIObserver(serverService);
								remoteService.addObserver(serverService.observer);
								System.out.println("Observer added");
							} else {
								remoteService.removeObserver(serverService.observer);
								serverService.observer = null;
								System.out.println("Observer removed");
							}
						}
						default -> System.out.println("Invalid Choice");
					}
				}
			} catch (RemoteException e) {
				System.out.println("Exception : " + e.getMessage());
			}
		}
	}
	
	public static int getUserChoice(Scanner scanner) {
		System.out.print("-> ");
		try {
			return Integer.parseInt(scanner.nextLine());
		} catch (Exception e) {
			return getUserChoice(scanner);
		}
	}
}
