

public interface Listeners extends java.rmi.Remote {
	
	public void updateData() throws java.rmi.RemoteException;
	
	public void lostConnection(String sensorId) throws java.rmi.RemoteException;
	
	public void requestData(String sensorId) throws java.rmi.RemoteException;
	
	public void updateSensorMonitorCount() throws java.rmi.RemoteException;
	
	public void hourlyReadingLost(String sensorId) throws java.rmi.RemoteException;
	
	public void sendWarning(String sensorId) throws java.rmi.RemoteException;
	
	public void requestSensorsList() throws java.rmi.RemoteException;
	
	public void requestedDataUpdated(String sensorId) throws java.rmi.RemoteException;

}
