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
