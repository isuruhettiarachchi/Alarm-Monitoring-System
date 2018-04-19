# Alarm Monitoring System

This program is done as an assignment in the SLIIT 3rd Year Distributed System Module. Central Server is listening to sensors and send updates to monitors. Sensors and server communication is done using socket connections and Monitors and server communicate throguht JAVA RMI.

## Getting Started

Clone the project

Add the project folder to classpath
```
set CLASSPATH=%CLASSPATH%;<<your directory path here>> 
```

Start RMI registry
```
start rmiregistry
```

Start the Central Server
```
java –Djava.security.policy=allowall.policy CentralServer
```

Start any number of Sensors and Monitors


### Prerequisites

You need Java Runtime Enviornment to run this program


## Authors

* **Isuru Hettiarachchi** - *Initial work* - [robotikka](https://github.com/robotikka)


## Assignment Task

2018 – Semester 1 
SE3020 – Distributed Systems 
Programming Assignment 1 
 
Imagine that you have been asked to develop a fire alarm monitoring system for a high-rise building. You will be using fire alarms that contain sensors that can measure the temperature (celcius), battery level of the sensor (percentage value) and the smoke level (out of a scale of 0-10) and the CO2 level (parts per million. Average is around 300 in normal atmosphere). The sensors fixed to the fire alarms do these measurements at five minute intervals. The fire alarm sensors do have JVMs with TCP/IP network ports but they can only perform basic socket communication to send data to a central server. The central server is at the control room and the server is part of the Local area network in the building. A server machine is available with higher memory and processing speed to deploy the server. Each fire alarm sensor is uniquely identified by an ID of the following form, 23-13, where the first number is the floor number and the second number is the number of the sensor in that particular floor. 
 
The administrative staff can connect to the server and monitor the fire alarm system in the building remotely. At a given time, several clients may be connected to the server to monitor the status of the fire alarm system. These clients are called ‘monitors’ by the administrative staff. 
 
The fire alarm sensors should send periodic updates (every 1 hour) of the above readings to a remote server. If the temperature exceeds 50 degrees Celsius or the smoke level is above 7 (out of the 0-10 scale), alerts should be sent to all monitoring clients by the server. Each monitoring client should be able to view the IDs of the sensors connected, along with their latest readings. If required, the monitoring stations should be able to query for the current readings of each fire alarm sensor. The system should be scalable so that additional weather sensors and/or monitoring stations can be added as required. If a particular sensor doesn’t send the 1 hour update, all monitoring clients should be alerted.  
 
Each monitoring client should be able to view the number of sensors and monitoring stations (clients) connected to the server at any given time.  (Note: These two numbers should be thread safe, meaning that when one server thread is updating these values, these valued should be locked and other threads will not be able to update these values). 
Basic security/authentication mechanisms should be there to authenticate each sensor and that data that is sent by the sensors to the server. Also, some basic authentication method can be there to authenticate the monitors when they connect and communicate with the server.  
 
