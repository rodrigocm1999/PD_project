package pt.RemoteService;

import pt.Common.RemoteService.Observer;
import pt.Common.RemoteService.RemoteService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class Main { // TODO
	
	public static void main(String[] args) throws RemoteException, NotBoundException {
		
		// recebe um argumento, ip do RMI registry
		// liga-se ao registry e pede a lista de serviços para obter todos os servidores
		// fazer um menuzito para escolher entre os vários servers
		
		// depois de escolher o server tem-se outro menu para escolher o que se quer fazer
		
		String registryAddress = "localhost";
		if (args.length >= 1) {
			registryAddress = args[0];
		}
		
		Registry registry = LocateRegistry.getRegistry(registryAddress);
		
		System.out.println("All remote services : " + Arrays.toString(registry.list())); // Aqui temos os vários servidores. podemos fazer uma lista a partir um menu a partir do que isto retorna
		
		RemoteService remoteService = (RemoteService) registry.lookup(registry.list()[0]);
		
		Observer observer = new RMIObserver();
		
		remoteService.addObserver(observer);
	}
	
}
