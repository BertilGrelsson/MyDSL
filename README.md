# MyDSL
WASP SW course Model-based Software Engineering

The grammar is defined in
org.xtext.example.mydsl/src/org/xtext/example/mydsl/MyDsl.xtext

In the Model Explorer view:
Right click on se.chalmers.turtlebotmission
Select Run As Eclipse Application

In the RunTime Eclipse Application that opens:
Open a model DSL file; in our case one of the files hej#.mydsl where # could be 1, 2 or 3

Click on the TurtleBot icon to generate the output file generated_mission.py

Describe what actually happens ...
- Grammar parses the model DSL file

- File se.chalmers.turtlebotmission.rosstarter/src/se/chalmers/turtlebotmission/rosstarter/handlers/CreatePythonFromModelHandler.java
generates the output python file.

In our case the output file only contains the coordinate list for the mission, e.g. from the hej2.mydsl

coordinate_list = [(1,3),(3,10),(2,8),(6,5),(6,2),(10,5),(1,1),(10,1),(6,2),(6,5),(3,7),(3,10),(1,1)] 


The python node run_mission.py contains a controller to move between the waypoints in the coordinate list.

1. Start a roscore
2. Run a turtlebot simulator node (rosrun turtlesim turtlesim_node)
3. Run the node run_mission.py
In this node, import the coordinate list generated from one of the model DSL files. 
