

import java.util.HashMap;
import java.util.HashSet;

public interface Server extends java.rmi.Remote {

	public void addListener(Listeners listener) throws java.rmi.RemoteException;

	public void removeListener(Listeners listener) throws java.rmi.RemoteException;

	public HashMap<String, String[]> getSensorData() throws java.rmi.RemoteException;

	public void requestData(String sensorId) throws java.rmi.RemoteException;

	public String sendSensorMonitorCount() throws java.rmi.RemoteException;
	
	public HashSet<String> sendSensorList() throws java.rmi.RemoteException;

}
