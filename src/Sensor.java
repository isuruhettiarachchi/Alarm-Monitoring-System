
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Sensor {

	private static BufferedReader in;
	private static PrintWriter out;

	// server information variables
	public static String sensorId;
	private static int temperature;
	private static int batteryLevel;
	private static int smokeLevel;
	private static int CO2Level;

	// GUI elements declaration
	JFrame frame = new JFrame("Sensor");
	JPanel pane = new JPanel(new GridLayout(0, 1));
	JTextField sensorIdTextField = new JTextField(10);
	static JTextField temperatureTextField = new JTextField(10);
	static JTextField batteryLevelTextField = new JTextField(10);
	static JTextField smokeLevelTextField = new JTextField(10);
	static JTextField CO2LevelTextField = new JTextField(10);

	public Sensor() {

		// Setting up the GUI
		sensorIdTextField.setEditable(false);
		temperatureTextField.setEditable(false);
		batteryLevelTextField.setEditable(false);
		smokeLevelTextField.setEditable(false);
		CO2LevelTextField.setEditable(false);

		frame.setLayout(new GridLayout(0, 1));
		frame.add(pane);
		pane.add(new JLabel("Sensor Id"));
		pane.add(sensorIdTextField);
		pane.add(new JLabel("Temperature"));
		pane.add(temperatureTextField);
		pane.add(new JLabel("Battery Level"));
		pane.add(batteryLevelTextField);
		pane.add(new JLabel("Smoke Level"));
		pane.add(smokeLevelTextField);
		pane.add(new JLabel("CO2 Level"));
		pane.add(CO2LevelTextField);

		frame.pack();
	}

	// get server address
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame, "Enter IP Address of the Server:", JOptionPane.QUESTION_MESSAGE);
	}

	// get a ID for sensor
	private String getSensorID() {
		return JOptionPane.showInputDialog(frame, "Choose a sensor ID:", JOptionPane.PLAIN_MESSAGE);
	}
	
	// enter password to access server
	private String authorize() {
		return JOptionPane.showInputDialog(frame, "Enter the password", JOptionPane.PLAIN_MESSAGE);
	}

	private void run() throws IOException {
		startSensor();
		String serverAddress = getServerAddress();
		Socket socket = new Socket(serverAddress, 9001);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		while (true) {

			String line = in.readLine();
			System.out.println("line " + line);
			if (line != null) {

				// server address is set. asking for the sensor id
				if (line.startsWith("SubmitSensorId")) {
					System.out.println(line);
					sensorId = getSensorID();
					out.println(sensorId);

				} else if (line.startsWith("submitPassword")) {
					String password = authorize(); // get password
					out.println(password);
					
				} else if (line.startsWith("SensorIdAccepted")) { // valid sensor id is saved in the server
					sensorIdTextField.setText(sensorId); // Set sensor id to GUI

				} else if (line.startsWith("HourlyRequest")) { // send a hourly updates from sensor
					out.println("HourlyData" + sensorId + ">>" + getCurrentTime() + ">>" + temperature + ">>"
							+ batteryLevel + ">>" + smokeLevel + ">>" + CO2Level);

				} else if (line.startsWith("DataRequest")) { // send a updates upon a request from a monitor or server
					out.println("RequestData" + sensorId + ">>" + getCurrentTime() + ">>" + temperature + ">>"
							+ batteryLevel + ">>" + smokeLevel + ">>" + CO2Level);
				}
			}
		}
	}

	public static void startSensor() {
		System.out.println("Sensor Started");

		// Set initial sensor values
		temperature = 27;
		batteryLevel = 100;
		smokeLevel = 7;
		CO2Level = 300;

		// Create thread for sensor
		Thread alarmSensor;
		alarmSensor = new Thread() {

			// Random value to change sensor values
			Random random = new Random();

			public void run() {
				System.out.println("flag1");
				for (;;) {

					// Send crtical data update to the server if values exceeding the normal readings
					if (temperature > 50 || smokeLevel > 7) {
						System.out.println("Normal levels exceeded");
						out.println("DataCritical" + sensorId + ">>" + getCurrentTime() + ">>" + temperature + ">>"
								+ batteryLevel + ">>" + smokeLevel + ">>" + CO2Level);
					}

					// Shutdown sensor when battery level is 0
					if (batteryLevel == 0) {
						// TODO : Shutdown sensor
					}

					// Sleep thread for 5 minutes and update the values
					try {
						int interval = 1000 * 10; // TODO: change to 5 minutes interval
						temperatureTextField.setText(temperature + " Celcius");
						batteryLevelTextField.setText(batteryLevel + "%");
						smokeLevelTextField.setText(smokeLevel + " ");
						CO2LevelTextField.setText(CO2Level + " ppm");
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// update the temperature
					int temperatureValue = random.nextInt();
					if (temperatureValue < 0) {
						temperature += 1;
					} else {
						temperature -= 1;
					}

					// reduce the battery level
					batteryLevel = batteryLevel - 1;

					// update the smoke level
					int smokeLevelValue = random.nextInt();
					if (smokeLevelValue < 0 && smokeLevel < 10) {
						smokeLevel += 1;
					} else if (smokeLevelValue > 0 && smokeLevel > 1) {
						smokeLevel -= 1;
					}

					// update CO2 level
					int CO2value = random.nextInt();
					if (CO2value < 0) {
						CO2Level += 1;
					} else {
						CO2Level -= 1;
					}
				}
			}
		};

		// start thread
		alarmSensor.start();
	}

	// get the current time to send with the readings
	public static String getCurrentTime() {
		return LocalDateTime.now().toString();
	}

	public static void main(String[] args) throws IOException {
		Sensor sensor = new Sensor();
		sensor.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sensor.frame.setVisible(true);
		sensor.run();
	}

}
