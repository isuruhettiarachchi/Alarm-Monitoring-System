
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class CentralServer extends UnicastRemoteObject implements Server, Runnable {

	private static final int PORT = 9001;

	private static HashSet<String> sensors = new HashSet<String>();
	private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
	private static HashSet<Listeners> monitorList = new HashSet<Listeners>();

	public static ArrayList<String> sensorReadings = new ArrayList<String>();

	private static HashMap<String, String[]> sensorsStatus = new HashMap<String, String[]>();
	private static HashMap<String, PrintWriter> sensorsToWriter = new HashMap<String, PrintWriter>();

	private static String formattedData[];

	Timer timer = new Timer();

	@Override
	public void run() {
		timer.scheduleAtFixedRate(getReadingsPeriodically, 10000, 1000 * 60 * 60 * 60); // get hourly readings
	}

	protected CentralServer() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Server started");
		try {
			CentralServer server = new CentralServer();
			String registry = "localhost";
			String registration = "rmi://" + registry + "/AlarmSensor";
			Naming.rebind(registration, server);
			Thread thread = new Thread(server);
			thread.start();
		} catch (Exception ex) {
			System.out.println(ex);
		}

		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				new Handler(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	private static class Handler extends Thread {
		private String sensorId;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;

		public Handler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			System.out.println("run thread");
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				// ask sensor to submit a valid sensor id
				while (true) {
					out.println("SubmitSensorId");
					sensorId = in.readLine();
					System.out.println("SensorId: " + sensorId);

					if (sensorId == null)
						return;

					// check the submitted sensor id is in the correct format
					if (sensorId.matches("[0-9]+[-]+[0-9]+")) {
						synchronized (sensors) {
							if (!sensors.contains(sensorId)) {
								sensors.add(sensorId);
								break;
							}
						}
					}
				}

				// valid sensor id recieved. add sensor writers
				out.println("SensorIdAccepted");
				writers.add(out);
				
				while (true) {
					out.println("submitPassword");
					String password = in.readLine();
					if (password.equals("abcd1234")) {
						break;
					}
				}
				
				sensorsStatus.put(sensorId, null); // add a status to check the hourly updates recieved or not
				sensorsToWriter.put(sensorId, out); // add sensors and their writer to request data from a specific
													// sensor

				// send the monitors and sensors count to all monitors
				for (Listeners monitor : monitorList) {
					monitor.updateSensorMonitorCount();
				}

				// get readings from newly added sensor
				while (true) {
					out.println("DataRequest");
					String input = in.readLine();
					if (input.contains(">>")) {
						saveSensorData(input, 11); // save sensor data to hash map
						System.out.println("break");
						sensorReadings.add(input.substring(11));
						break;
					}
				}

				// send the current connected sensors list to all the monitors
				// monitors can request readings from specific sensor using this list
				sendSensorsToMonitors();

				// send the newly connected monitors
				updateMonitors();

				for (;;) {
					String input = in.readLine();
					System.out.println("input:" + input);
					if (input == null) {
						return;
					} else if (input.startsWith("HourlyRequest")) { // hourly data recieved from sensor
						System.out.println("Hourly reading recievied from sensor: " + sensorId);

						// set sensor reading to null to find the lost hourly data updates
						synchronized (sensorsStatus) {
							sensorsStatus.forEach((key, value) -> {
								value = null;
							});
						}

						saveSensorData(input, 13); // save new readings
						updateMonitors(); // update all monitors with new readings
						sendHourlyUpdateStatus(); // send a status of lost hourly updates

					} else if (input.startsWith("DataCritical")) { // values exceeded the normal readings
						System.out.println("Critical data reading reciveid from sensor: " + sensorId);
						saveSensorData(input, 12); // save the readings
						updateMonitors(); // update the monitors

						// sned a warning to the all monitors
						for (Listeners monitor : monitorList) {
							try {
								monitor.sendWarning(sensorId);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					} else if (input.startsWith("RequestData")) { // request data from a specific sensor recivied
						System.out.println("Request sensor data recieved from sensor: " + sensorId);

						saveSensorData(input, 11); // save readings
						updateMonitors(); // update monitors

						// update monitors about the request data recieving
						for (Listeners monitor : monitorList) {
							try {
								monitor.requestedDataUpdated(sensorId);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						// has to send the readings to requested monitor only
						// not implemented
						// TODO : send to the monitor

					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (sensorId != null) {
					// inform all the monitors about the sensor disconnection
					for (Listeners monitor : monitorList) {
						try {
							monitor.lostConnection(sensorId);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println(sensorId + " lost the connection with the server");

					// remove sensor
					synchronized (sensors) {
						sensors.remove(sensorId);
					}

					// remove sensor reading
					synchronized (sensorsStatus) {
						sensorsStatus.remove(sensorId);
					}

					// remove sensor writer
					synchronized (sensorsToWriter) {
						sensorsToWriter.remove(sensorId);
					}
				}

				sendSensorsToMonitors(); // update the current connected sensor list

				// update the current connected monitor and sensor count
				for (Listeners monitor : monitorList) {
					try {
						monitor.updateSensorMonitorCount();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (out != null) {
					writers.remove(out);
				}

				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

	// send the current connected sensor list to monitors
	private static void sendSensorsToMonitors() {
		for (Listeners monitor : monitorList) {
			try {
				monitor.requestSensorsList(); // call a remote method in monitors to request the sensor list
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// send the hourly updates status
	private static void sendHourlyUpdateStatus() {
		for (Listeners monitor : monitorList) {
			sensorsStatus.forEach((key, value) -> {
				if (value == null) {
					try {
						monitor.hourlyReadingLost(key); // call a remote method in monitors to inform the update status
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	}

	// save new readings from sensor
	public static void saveSensorData(String reading, int readingType) {
		synchronized (sensorsStatus) {
			reading = reading.substring(readingType);
			formattedData = reading.split(">>");
			System.out.println(Arrays.asList(formattedData));

			// save reading to hash map for quick access
			sensorsStatus.forEach((key, value) -> {
				if (key.toString().equals(formattedData[0].toString())) {
					sensorsStatus.put(key, formattedData);
				}
			});
			System.out.println(sensorsStatus.toString());
		}

	}

	// update the monitors with newly readings
	public static void updateMonitors() {
		for (Listeners monitor : monitorList) {
			try {
				monitor.updateData(); // call a remote method in monitor to update the data
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// task to get readings from 5 minutes interval
	TimerTask getReadingsPeriodically = new TimerTask() {

		@Override
		public void run() {
			System.out.println("Timer called");

			// set current readings to null to checkupdates recieved or not
			synchronized (sensorsStatus) {
				sensorsStatus.forEach((key, value) -> {
					value = null;
				});
			}

			// request update from all sensors
			if (sensorsStatus.size() > 0) {
				for (PrintWriter writer : writers) {
					writer.println("HourlyRequest");
					System.out.println("Hourly request sent");

					try {
						Thread.sleep(5);
					} catch (Exception ex) {
						System.out.println(ex);
					}
				}
			}
		}

	};

	// add new monitor
	@Override
	public void addListener(Listeners listener) throws RemoteException {
		synchronized (monitorList) {
			monitorList.add(listener);

		}

		// update the current connected monitor and sensor count
		for (Listeners monitor : monitorList) {
			monitor.updateSensorMonitorCount();
		}

	}

	// remove a monitor
	// close a monitor
	@Override
	public void removeListener(Listeners listener) throws RemoteException {
		synchronized (monitorList) {
			monitorList.remove(listener);

		}

		// update the current connected monitor and sensor count
		for (Listeners monitor : monitorList) {
			monitor.updateSensorMonitorCount();
		}
	}

	// return the hash map that saves the readings from sensors
	// this method use to update the values in monitors
	@Override
	public HashMap<String, String[]> getSensorData() throws RemoteException {
		synchronized (sensorsStatus) {
			return sensorsStatus;
			// return null;
		}
	}

	// request data from a speicific sensor
	// called from a monitor
	@Override
	public void requestData(String sensorId) throws RemoteException {
		synchronized (sensorsToWriter) {
			sensorsToWriter.get(sensorId).println("DataRequest");
		}
	}

	// send the current sensor and monitor count
	@Override
	public String sendSensorMonitorCount() throws RemoteException {
		synchronized (sensors) {
			synchronized (monitorList) {
				return sensors.size() + "//" + monitorList.size();
			}
		}

	}

	// sned the current connected sensor list
	@Override
	public HashSet<String> sendSensorList() throws RemoteException {
		synchronized (sensors) {
			return sensors;
		}
	}

}
