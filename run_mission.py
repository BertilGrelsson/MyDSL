#!/usr/bin/env python
import rospy
from geometry_msgs.msg  import Twist
from turtlesim.msg import Pose
from math import pow,atan2,sqrt
from generated_mission4 import coordinate_list

class turtle():
	def __init__(self):
		rospy.init_node('turtlebot_controller', anonymous=True)
		self.velocity_publisher = rospy.Publisher('/turtle1/cmd_vel', Twist, queue_size=10)
		self.pose_subscriber = rospy.Subscriber('/turtle1/pose', Pose, self.callback)
		self.pose = Pose()
		self.rate = rospy.Rate(10)
		self.tolerance = 0.1

	def callback(self, data):
		self.pose = data
		self.pose.x = round(self.pose.x, 4)
		self.pose.y = round(self.pose.y, 4)

	def get_distance(self, goal_x, goal_y):
		distance = sqrt(pow((goal_x - self.pose.x), 2) + pow((goal_y - self.pose.y), 2))
		return distance

	def move2goal(self,posX,posY):
		speed = 10
		goal_pose = Pose()
		goal_pose.x = posX
		goal_pose.y = posY
		distance_tolerance = self.tolerance
		vel_msg = Twist()
		angErrorLast = 0.0
		angError = atan2(goal_pose.y - self.pose.y, goal_pose.x - self.pose.x) - self.pose.theta
		while abs(angError) >= 0.0001:
			vel_msg.angular.z = speed * 2.0 * angError
			self.velocity_publisher.publish(vel_msg)
			angError = atan2(goal_pose.y - self.pose.y, goal_pose.x - self.pose.x) - self.pose.theta
			
		while sqrt(pow((goal_pose.x - self.pose.x), 2) + pow((goal_pose.y - self.pose.y), 2)) >= distance_tolerance:
			vel_msg.linear.x = speed * 1.0 * sqrt(pow((goal_pose.x - self.pose.x), 2) + pow((goal_pose.y - self.pose.y), 2))
			vel_msg.linear.y = 0
			vel_msg.linear.z = 0

			angError = atan2(goal_pose.y - self.pose.y, goal_pose.x - self.pose.x) - self.pose.theta
			vel_msg.angular.x = 0
			vel_msg.angular.y = 0
			vel_msg.angular.z = speed * 2.0 * angError - speed * 1.0 *(angError - angErrorLast)
			angErrorLast = angError

			self.velocity_publisher.publish(vel_msg)
			self.rate.sleep()
		vel_msg.linear.x = 0
		vel_msg.angular.z =0
		self.velocity_publisher.publish(vel_msg)

	def move2goal_old(self,posX,posY):
		goal_pose = Pose()
		goal_pose.x = posX
		goal_pose.y = posY
		distance_tolerance = self.tolerance
		vel_msg = Twist()
		angErrorLast = 0.0
		while sqrt(pow((goal_pose.x - self.pose.x), 2) + pow((goal_pose.y - self.pose.y), 2)) >= distance_tolerance:
			vel_msg.linear.x = 1.0 * sqrt(pow((goal_pose.x - self.pose.x), 2) + pow((goal_pose.y - self.pose.y), 2))
			vel_msg.linear.y = 0
			vel_msg.linear.z = 0

			angError = atan2(goal_pose.y - self.pose.y, goal_pose.x - self.pose.x) - self.pose.theta
			vel_msg.angular.x = 0
			vel_msg.angular.y = 0
			vel_msg.angular.z = 4.0 * angError - 2.0 *(angError - angErrorLast)
			angErrorLast = angError

			self.velocity_publisher.publish(vel_msg)
			self.rate.sleep()
		vel_msg.linear.x = 0
		vel_msg.angular.z =0
		self.velocity_publisher.publish(vel_msg)

if __name__ == '__main__':
	try:
		tb = turtle()
#		coordinate_list = [(1,2),(3,4),(1,1),(1,2),(10,5),(3,4),(1,1)] 

		for coordinate in coordinate_list:
			x,y = coordinate
			tb.move2goal(x,y)

	except rospy.ROSInterruptException: pass

'''DEBUG
ReFuel::ShortestPath: [(1,2),(3,4)]
ReFuel::ReturnToStart: [(1,1)]
ShuttleService::ShortestPath: [(1,2),(10,5)]
ShuttleService::Line: [(3,4)]
ShuttleService::ReturnToStart: [(1,1)]

Update'''
