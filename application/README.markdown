# _Xita Innovation day application_ 

## Application
The application is a simple webshop that is based on the 'Xebia-Stack" consisting of: 
- Akka
- Spray
- AngularJS
- Twitter Bootstrap

## Build and Run
- Execute ```./sbt.sh run``` to run the server locally. 
- Open a browser using the following url: ```http://localhost:8080/```
- There is a nice user interface that let's you call all endpoints of the REST interface

## Create Executable jar
- Execute ```./sbt.sh assembly```. This will create an executable jar in ```target/scala-2.10```
- Run the server as follows: ```java -jar xita-innovationday-assembly-<version>.jar```


## Load source code in IDE
_Eclipse_
- Download Eclipse version Kepler (4.3) for Scala version **2.10.x**: http://scala-ide.org/download/sdk.html
- Or install the scala plugin for version **2.10.x**: http://scala-ide.org/download/current.html
- Run ```./sbt.sh eclipse``` to generate eclipse files and import project

_Idea_
- Install Scala plugin
- Install sbt plugin
- Import project (Generation of idea files is not necessary)

## Test REST API manually 
_Note:_ Since it's a shopping cart the REST API is session-based. The name of the session cookie is *session-id*.

- Add an item to the shopping cart:
```
curl -b session-id=12121212 -d '{"itemId":"dell-venue"}' -H "Content-Type: application/json"  http:localhost:8080/cart
```

- Retrieve shopping cart contents:
```
curl -b session-id=12121212 http://localhost:8080/cart
```

- Remove an item from the shopping cart:
```
curl -b session-id=12121212 -X "DELETE" http://localhost:8080/cart?itemId=dell-venue
```

- Place order
```
curl -b session-id=12121212 -X "PUT" http://localhost:8080/order
```

##TODO
The current implementation of the application uses an Actor (ShoppingCartActor) that keeps the shopping cart in memory. This implementation we want to improve. During the innovation day we will rebuild this application - in Java if wanted - to:
- use Event Sourcing (by means of a persistant actor http://www.slideshare.net/ktoso/akka-persistence-event-sourcing-in-30-minutes) to process all shopping cart events
- use clustering for on-demand scaling 

