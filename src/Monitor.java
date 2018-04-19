
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class Monitor extends UnicastRemoteObject implements Listeners, Runnable {

	private static Monitor monitor;
	private static Server server;

	private static HashMap<String, String[]> sensorReadings = new HashMap<String, String[]>();

	DefaultListModel<String> sensors = new DefaultListModel<String>();
	JList<String> sensorList = new JList<String>(sensors);

	Timer timer = new Timer();

	public static JFrame frame = new JFrame("Monitor");
	JScrollPane scrollPaneData = new JScrollPane();
	JScrollPane scrollPaneSensors = new JScrollPane();
	DefaultTableModel model = new DefaultTableModel();
	JComboBox comboBox = new JComboBox();
	JTextArea messageArea = new JTextArea();
	JTable table;
	private static JButton requestDataButton = new JButton("Request Data");
	private static JLabel label1 = new JLabel("No of sensors");

	public Monitor() throws RemoteException {
		
		//get password and verify
		while (true) {
			String password = authorize();
			if (password.equals("abcd1234")) {
				break;
			}
		}
		
		System.out.println("Starting monitor");

		frame.setSize(700, 500);
		frame.setLayout(new GridLayout(0, 1));
		JPanel panel1 = new JPanel(new GridLayout(0, 1));
		JPanel panel2 = new JPanel(new GridLayout(0, 1));
		JPanel panel3 = new JPanel(new GridLayout(0, 1));
		table = new JTable(model);

		table.setRowSelectionAllowed(true);
		// messageArea.setForeground(Color.blue);
		scrollPaneData.setViewportView(table);
		panel1.add(scrollPaneData);
		scrollPaneSensors.add(messageArea);
		panel1.setBorder(new TitledBorder("Sensor Data"));
		frame.add(panel1);

		panel3.setBorder(new TitledBorder("Sensors List"));
		panel3.add(new JScrollPane(sensorList));
		panel3.add(requestDataButton); // TODO : set button size
		frame.add(panel3);

		messageArea.setBorder(new TitledBorder("Notifications"));
		scrollPaneSensors.setViewportView(messageArea);
		panel2.add(label1);
		panel2.add(scrollPaneSensors);
		table.setAutoscrolls(true);
		table.setCellSelectionEnabled(true);
		messageArea.append("Monitor started");

		frame.add(panel2);

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		String columnNames[] = new String[] { "Sensor", "Time", "Temperature", "BatteryLevel", "Smoke Level",
				"CO2 Level" };
		model.setColumnIdentifiers(columnNames);

		// request data from a specific data
		requestDataButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Button clicked");
				String id = getSelectedSensorId(); // get the selected sensor id from the sensor list
				if (id != null) {
					try {
						requestData(id); // request data
					} catch (RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

			}
		});

		// frame close event to remove the monitor from server
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(frame, "Are you sure to close this window?", "Readings will be lost",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					try {
						server.removeListener(monitor); // remote method in server called to remove the monitor
						System.exit(0);
					} catch (Exception ex) {
						System.out.println(ex);
					}

				}
			}
		});
	}
	
	// enter password to access server
		private String authorize() {
			return JOptionPane.showInputDialog(frame, "Enter the password:", JOptionPane.PLAIN_MESSAGE);
		}

	public static void main(String[] args) throws NotBoundException {
		System.out.println("Main method");
		try {
			monitor = new Monitor();
			Monitor.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
			String registration = "//localhost/AlarmSensor";
			Remote remoteService = Naming.lookup(registration);
			server = (Server) remoteService;
			server.addListener(monitor);
			monitor.requestSensorsList();

			System.out.println("Run called");

			String num = server.sendSensorMonitorCount();
			String[] SensMonitorNum = num.split("//");
			label1.setText("Number of sensors: " + SensMonitorNum[0] + " \t Number of monitors: " + SensMonitorNum[1]);
			monitor.run();
		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	@Override
	public void run() {
		try {
			updateData(); // update the monitor with current readings saved in the server
			updateSensorMonitorCount(); // update the current sensor and monitor count
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		timer.scheduleAtFixedRate(update, 10000, 1000 * 60 * 60 * 60); // update readings each hour

	}

	// task to update the readings from one hour interval
	TimerTask update = new TimerTask() {
		public void run() {
			try {
				updateData(); // update the monitor with current readings saved in the server
				updateSensorMonitorCount(); // update the current sensor and monitor count
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	};

	
	// get selected id from the list
	private String getSelectedSensorId() {
		String id = null;
		if (sensorList.getSelectedIndex() != -1) {
			for (String sensorId : sensorList.getSelectedValuesList()) {
				id = sensorId;
			}
		}
		return id;
	}

	// update the readings in monitor
	@Override
	public void updateData() throws RemoteException {
		System.out.println("Update with new readings");
		sensorReadings = server.getSensorData(); // call a remote method get the current saved readings in server
		model.getDataVector().removeAllElements();
		model.setRowCount(0);
		int rows = table.getRowCount();
		System.out.println(sensorReadings);
		sensorReadings.forEach((key, value) -> {
			System.out.println(Arrays.asList(value));
			// for (int i = 0; i < rows; i++) {
			// if (key.equals(table.getValueAt(i, 0).toString()) && value != null) {
			// model.removeRow(i);
			// }
			// }
			// if (model.getRowCount() > 0) {
			// for (int i = model.getRowCount() - 1; i > -1; i--) {
			// model.removeRow(i);
			// }
			// }
			if (value != null) {
				String dataArray[] = sensorReadings.get(key);
				model.addRow(new Object[] { dataArray[0], dataArray[1], dataArray[2], dataArray[3], dataArray[4],
						dataArray[5] });
			}
		});

		updateSensorMonitorCount(); // update the monitor and sensor count
		model.fireTableDataChanged();
	}

	
	// connection lost between sensor and server
	@Override
	public void lostConnection(String sensorId) throws RemoteException {
		int rows = table.getRowCount();
		
		// remove the sensor value from the table
		if (rows > 0) {
			for (int i = 0; i < rows; i++) {
				if (sensorId.equals(table.getValueAt(i, 0).toString())) {
					model.removeRow(i);
				}
			}
		}
		updateSensorMonitorCount(); // update the sensor count

		messageArea.append("\n" + sensorId + ": lost the connection with server");
	}

	
	// request data from a specific sensor
	@Override
	public void requestData(String sensorId) throws RemoteException {
		server.requestData(sensorId); // call a remote method to get readings from the sensor
		messageArea.append("\n" + "Data request from " + sensorId);
	}

	// update the monitor and sensor count from server
	@Override
	public void updateSensorMonitorCount() throws RemoteException {
		String num = server.sendSensorMonitorCount(); // call a remote method to get the current sensor and monitor count
		String[] SensMonitorNum = num.split("//");
		label1.setText("Number of sensors: " + SensMonitorNum[0] + " \t Number of monitors: " + SensMonitorNum[1]);
	}

	
	// show notification upon the hourly update status
	@Override
	public void hourlyReadingLost(String sensorId) throws RemoteException {
		messageArea.append("\n" + sensorId + ": Did not recived hourly updates");
	}

	// show a warning if the readings exceed normal values
	@Override
	public void sendWarning(String sensorId) throws RemoteException {
		messageArea.append("\n" + sensorId + ": Exceeded normal values. Warning!");

	}

	// reqeust current connected sensor list from server
	@Override
	public void requestSensorsList() throws RemoteException {
		HashSet<String> list = server.sendSensorList(); // call a remote method in server to get the sensor list

		// save the list in GUI to get the specific readings
		if (list.size() > 0) {
			String[] tempSensors = list.toArray(new String[list.size()]);
			sensorList.setListData(tempSensors);
			sensorList.setSelectedIndex(0);
		}
	}

	
	// notify monitor upon the recieving of request data from a specific sensor
	@Override
	public void requestedDataUpdated(String sensorId) throws RemoteException {
		messageArea.append("\n" + "Requested readings recieved from " + sensorId);
	}

}
