package se.chalmers.turtlebotmission.rosstarter.handlers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import turtlebotmission.LineTask;
import turtlebotmission.Mission;
import turtlebotmission.ReturnToStartTask;
import turtlebotmission.ShortestPathTask;
import turtlebotmission.Task;
import turtlebotmission.TurtleBot;
import turtlebotmission.WayPoint;
import turtlebotmission.impl.TurtleBotImpl;

/**
 * A handler that is called when the user clicks on the turtlebot icon in the menu.
 * Allows to calculate a list of waypoints to visit and then initiates the creation of the Python file.
 * 
 */
public class CreatePythonFromModelHandler extends AbstractHandler {

	String debugString = new String();
	
	/**
	 * Called whenever a user clicks on the turtlebot icon
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IEditorPart editor = window.getActivePage().getActiveEditor();
		if (editor instanceof XtextEditor) {
			IXtextDocument doc = ((XtextEditor) editor).getDocument();

			doc.modify(new IUnitOfWork<Void, XtextResource>() {
				@Override
				public java.lang.Void exec(XtextResource archimodel) {
					// now we access the model 
					for (EObject modelObject : archimodel.getContents()) {
						if (modelObject instanceof TurtleBotImpl) {
							
							//Now you have the access to your model:
							TurtleBot turtle = (TurtleBot) modelObject;
							
														
							List<WayPoint> wpList = new ArrayList<WayPoint>();	// The list of way points we want the turtle bot to move through
							WayPoint wpStart = turtle.getBot_start();
							WayPoint wpCurrent = wpStart;
							for(Mission mission : turtle.getMissions()) {
								for(Task task : mission.getTask()) {
									String taskName = new String();	// Debug
									List<WayPoint> subPath = null;
									if(task instanceof ShortestPathTask) {
										subPath = findShortestPath(wpCurrent, ((ShortestPathTask) task).getWaypoints());
										taskName = "ShortestPath";
									} else
									if(task instanceof LineTask) {
										subPath = ((LineTask) task).getWaypoints();
										taskName = "Line";
									} else
									if(task instanceof ReturnToStartTask) {
										subPath = new ArrayList<WayPoint>();
										subPath.add(wpStart);
										taskName = "ReturnToStart";
									}
									wpCurrent = subPath.get(subPath.size()-1);
									wpList.addAll(subPath);
									System.out.println(mission.getName()+"::"+taskName+": "+toString(subPath));
									debugString += mission.getName()+"::"+taskName+": "+toString(subPath)+"\n";
								}
							}
							
							
							
							IWorkbenchWindow window;
							try {
								window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
								MessageDialog.openInformation(window.getShell(), "Launch ROS",
										"You will find the resulting file in this workspace, in generated_mission.py");
								
								
								//This is were you should parse the model, create a plan, and fill the string template
								//Don't hesitate to use extra classes and methods to structure your code
								String pythoncode = "coordinate_list = " + toString(wpList) + " \n";
								/*String pythoncode = "#!/usr/bin/env python\n"+
										"import rospy\n"+
										"from geometry_msgs.msg  import Twist\n"+
										"from turtlesim.msg import Pose\n"+
										"from math import pow,atan2,sqrt\n"+
										"\n"+
										"class turtle():\n"+
										"	def __init__(self):\n"+
										"		rospy.init_node('turtlebot_controller', anonymous=True)\n"+
										"		self.velocity_publisher = rospy.Publisher('/turtle1/cmd_vel', Twist, queue_size=10)\n"+
										"		self.pose_subscriber = rospy.Subscriber('/turtle1/pose', Pose, self.callback)\n"+
										"		self.pose = Pose()\n"+
										"		self.rate = rospy.Rate(10)\n"+
										"		self.tolerance = 0.1\n"+
										"\n"+
										"	def callback(self, data):\n"+
										"		self.pose = data\n"+
										"		self.pose.x = round(self.pose.x, 4)\n"+
										"		self.pose.y = round(self.pose.y, 4)\n"+
										"\n"+
										"	def get_distance(self, goal_x, goal_y):\n"+
										"		distance = sqrt(pow((goal_x - self.pose.x), 2) + pow((goal_y - self.pose.y), 2))\n"+
										"		return distance\n"+
										"\n"+
										"	def move2goal(self,posX,posY):\n"+
										"		goal_pose = Pose()\n"+
										"		goal_pose.x = posX\n"+
										"		goal_pose.y = posY\n"+
										"		distance_tolerance = self.tolerance\n"+
										"		vel_msg = Twist()\n"+
										"		angErrorLast = 0.0\n"+
										"		while sqrt(pow((goal_pose.x - self.pose.x), 2) + pow((goal_pose.y - self.pose.y), 2)) >= distance_tolerance:\n"+
										"			vel_msg.linear.x = 1.0 * sqrt(pow((goal_pose.x - self.pose.x), 2) + pow((goal_pose.y - self.pose.y), 2))\n"+
										"			vel_msg.linear.y = 0\n"+
										"			vel_msg.linear.z = 0\n"+
										"\n"+
										"			angError = atan2(goal_pose.y - self.pose.y, goal_pose.x - self.pose.x) - self.pose.theta\n"+
										"			vel_msg.angular.x = 0\n"+
										"			vel_msg.angular.y = 0\n"+
										"			vel_msg.angular.z = 4.0 * angError - 2.0 *(angError - angErrorLast)\n"+
										"			angErrorLast = angError\n"+
										"\n"+
										"			self.velocity_publisher.publish(vel_msg)\n"+
										"			self.rate.sleep()\n"+
										"		vel_msg.linear.x = 0\n"+
										"		vel_msg.angular.z =0\n"+
										"		self.velocity_publisher.publish(vel_msg)\n"+
										"\n"+
										"if __name__ == '__main__':\n"+
										"	try:\n"+
										"		tb = turtle()\n"+
										//"	coordinate_list = [(1,1),(1,2),(3,3),(6,6),(10,10),(1,1)]\n"+
										"		coordinate_list = " + toString(wpList) + " \n" +
										"\n"+
										"		for coordinate in coordinate_list:\n"+
										"			x,y = coordinate\n"+
										"			tb.move2goal(x,y)\n"+
										"\n"+
										"	except rospy.ROSInterruptException: pass" + "\n\n'''DEBUG\n" + debugString+"\nUpdate'''"; */
								
								
								
								IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
								IProject myProject = myWorkspaceRoot.getProjects()[0];
								// create a new file
								IFile resultFile = myProject.getFile("generated_mission.py");
								try {
									if (!resultFile.exists())
										resultFile.create(new ByteArrayInputStream(new byte[0]), false, null);
									
									//fill the file
									resultFile.setContents(new ByteArrayInputStream(pythoncode.getBytes("UTF-8")), 0, null);
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
						}
					}
					return null;
				}

				public String toString(List<WayPoint> wpList) {
					String str = new String();
					for(WayPoint wp : wpList) {
						str += "(" + ((Integer)wp.getCoord_x()).toString() + "," + ((Integer)wp.getCoord_y()).toString() + "),";
					}
					str = str.substring(0, str.length()-1); // Remove trailing ","
					str = "[" + str + "]";
					return str;
				}
				
				public List<WayPoint> findShortestPath(WayPoint current, List<WayPoint> wps) {
					List<WayPoint> solution = new ArrayList<WayPoint>();
					//solution.addAll(wps);	//TODO: Do something smarter
					
					// Find all permutations
					Permute permuter = new Permute();
					List<Integer> indexList = new ArrayList<Integer>();
					for(int n = 0; n < wps.size(); n++) {
						indexList.add(n);
					}					
					List<ArrayList<Integer>> permutations = permuter.permute(indexList);
					
					// Find cheapest permutation
					float minCost = Float.MAX_VALUE;
					int minCostPermutationIndex = 0;
					for(int n = 0; n < permutations.size(); n++) {
						float cost = calculatePathCost(permutations.get(n), wps, current);
						if(cost < minCost) {
							minCost = cost;
							minCostPermutationIndex = n;
						}
						//debugString += "P" + ((Integer)n).toString() + ": " + ((Float)cost).toString() + " :: ";
						for(Integer index : permutations.get(n)) {
							//debugString += index.toString() + ","; 
						}
						//debugString += "\n";
					}
					
					// Generate shortest path
					for(int index : permutations.get(minCostPermutationIndex)) {
						solution.add(wps.get(index));
					}
					
					return solution;		
				}
				
				class Permute {					
					public List<ArrayList<Integer>> permutations; 
					
				    void permute_(java.util.List<Integer> arr, int k){
				        for(int i = k; i < arr.size(); i++){
				            java.util.Collections.swap(arr, i, k);
				            permute_(arr, k+1);
				            java.util.Collections.swap(arr, k, i);
				        }
				        if (k == arr.size() -1){
				        	ArrayList<Integer> permutation = new ArrayList<Integer>();
				        	permutation.addAll(arr);
				        	permutations.add(permutation);
				        }
				    }
				    
				    public List<ArrayList<Integer>> permute(List<Integer> values) {
				    	permutations = new ArrayList<ArrayList<Integer>>();
				    	permute_(values,0);
				    	return permutations;
				    }
				}
				
				public float calculatePathCost(List<Integer> indexes, List<WayPoint> wps, WayPoint start) {
					float cost = 0;
					WayPoint prev = start;
					for(Integer index : indexes) {
						WayPoint next = wps.get(index);
						cost =  (float) (cost + Math.sqrt((float) ((next.getCoord_x()-prev.getCoord_x())*(next.getCoord_x()-prev.getCoord_x()) + 
														  		   (next.getCoord_y()-prev.getCoord_y())*(next.getCoord_y()-prev.getCoord_y()))));
						prev = next;
					}
					return cost;
				}
			});
		}

		return null;
	}


}
