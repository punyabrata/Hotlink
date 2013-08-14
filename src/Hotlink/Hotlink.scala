package Hotlink

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.Random

//The Message case class
//case class Message(theSerialNumber: Int, str: String){}

//The Message case class
case class Message(theActor: Actor, str: String, theSerialNo: Int, throughAP: String){}

//Pair-of-stations-details
class PairOfStationsDetails(station1: Station,station2: Station,timeStamp: Long){
  var stationA: Station = station1
  var stationB: Station = station2
  var timeInMillis: Long = timeStamp
}

object Hotlink {

  def main(args: Array[String]): Unit = {             
    //Initializing the main actor
    val theMainActor = new TheMainActor()
    theMainActor.start
  }

}

class TheMainActor extends Actor{
  var responseCount: Int = 0
  var accessPtCount: Int = 0
  var avgDirect: Double = 0.0
  var avgThroughAP: Double = 0.0
  var arrayOfPairs: Array[PairOfStationsDetails] = new Array[PairOfStationsDetails](10)	//Array of pair of Stations
  //The act method  
  def act(){
    val computation = new Computation()	//Computation object
    //(X-Coord, Y-Coord, Station-Type, A-or-B, Access-Point, Peer-Station, Distance-From-Peer, Main-Actor)
    val accessPt = new Station(0,0,"AP",-1,null,null,200.0,this,0) //Access Point    
    
    //Creating each pair of Stations
    for(i: Int <- 0 until 10){
      //Array to hold the pair of Stations for each pair      
      //var pairOfStations: Array[Station] = new Array[Station](2)
      var pairOfStations: PairOfStationsDetails = null    
      //Randomly generated X and Y Coordinate
      var stationAX = computation.generateRandom(50)
      var stationAXS = computation.generateRandom(2)
      var stationAY = computation.generateRandom(50)
      var stationAYS = computation.generateRandom(2)
      var stationBX = computation.generateRandom(50)
      var stationBXS = computation.generateRandom(2)
      var stationBY = computation.generateRandom(50)
      var stationBYS = computation.generateRandom(2)
      
      //Putting signs for the station coordinates
      if(stationAXS == 0){
        stationAX = stationAX*(-1)
      }	
	  if(stationAYS == 0){
	    stationAY = stationAY*(-1)
	  }	
	  if(stationBXS == 0){
	    stationBX = stationBX*(-1)
	  }	
	  if(stationBYS == 0){
	    stationBY = stationBY*(-1)
	  }
	  
	  //Pair of Stations
	  var distance = computation.computeDistance(stationAX, stationAY,stationBX,stationBY)
	  if(distance > 70.71){
	    accessPtCount += 1
	  }
	  //(X-Coord, Y-Coord, Station-Type, A-or-B, Access-Point, Peer-Station, Distance-From-Peer, Main-Actor)
      val stationA = new Station(stationAX,stationAY,"Station",1,accessPt,null,distance,this,(i+1))
	  //(X-Coord, Y-Coord, Station-Type, A-or-B, Access-Point, Peer-Station, Distance-From-Peer, Main-Actor)
      val stationB = new Station(stationBX,stationBY,"Station",0,accessPt,null,distance,this,(i+1))
	  
	  //Setting the Peer
	  stationA.setPeer(stationB)
	  stationB.setPeer(stationA)         
      
      //pairOfStations(0) = stationA
      //pairOfStations(1) = stationB
	  pairOfStations = new PairOfStationsDetails(stationA,stationB,System.currentTimeMillis)
      
      arrayOfPairs(i) = pairOfStations
    }
    //println("accessPtCount: "+accessPtCount)
    accessPt.setAPCount(accessPtCount)
    //accessPt.setRoutingTable(arrayOfPairs)
    
    for(i: Int <- 0 until 10){
      arrayOfPairs(i).stationA.stationActor ! "Start"
      println("")
      print("#"+arrayOfPairs(i).stationA.theSNumber+"		")
      print("XA: "+arrayOfPairs(i).stationA.xCoordinate+"	")
      print("YA: "+arrayOfPairs(i).stationA.yCoordinate+"	")
      print("XB: "+arrayOfPairs(i).stationB.xCoordinate+"	")
      print("YB: "+arrayOfPairs(i).stationB.yCoordinate+"	")      
      var distance = arrayOfPairs(i).stationA.distanceFromPeer
      print("distance: "+distance+"		")
      if(distance > 70.71)
        print("Communication Through AP	")
      else
        print("Direct Communication	")
      println("")
    }
    
    println("---------------------")
    println("STATION INITIALIZATION ENDS")
    println("---------------------")
    
    //exit
    
    //Waiting to hear responses from the node actors
    loop{
      react{
        case msg:Message =>
          if(msg.str == "PairFinish"){
            responseCount += 1
            var totalTime: Long = System.currentTimeMillis - arrayOfPairs(msg.theSerialNo-1).timeInMillis
            println("For a "+msg.throughAP+" connection, the total time for Station"+msg.theSerialNo+" is "+totalTime+" ms")
            if(msg.throughAP == "Direct"){
              avgDirect += totalTime
            }else if(msg.throughAP == "Through AP"){
              avgThroughAP += totalTime
            }
          }
          
          //Main actor exits only if it gets messages/responses back from all the node actors
          if(responseCount == 10){
            println("------------------------------------------------------------------------------------------")
            println("------------------------------------------------------------------------------------------")
            avgDirect /= (10-accessPtCount)
            avgThroughAP /= accessPtCount
            println("Average time for Direct Communication: "+avgDirect+" ms")
            println("Average time for Through AP Communication: "+avgThroughAP+" ms")
            println("---------------------")
            println("MAIN ACTOR EXITS")
            println("---------------------")
            //Printing the total time
            //println("Total time taken: "+(System.currentTimeMillis-timeAfterTopologyBuilt)+" ms")
            exit            
          }
      }
    }    
  }
}

//Stations in the network, if sType is = "AP" then it is an Access Point, else it is just a Station
class Station(xCoord: Int,yCoord: Int,sType: String,sending: Int,ap: Station,peer: Station,distance: Double,mainActor: Actor,serialNumber: Int){
  var xCoordinate: Int = xCoord
  var yCoordinate: Int = yCoord
  var stationType: String = sType
  var isSending: Int = sending
  var accessPoint: Station = ap
  var peerStation: Station = peer
  var distanceFromPeer: Double = distance
  var theMainActor: Actor = mainActor
  var accessPointCount: Int = 0
  var theSNumber: Int = serialNumber
  var theRoutingTable: Array[Array[Station]] = null

  //This method assigns peer Station to a Station  
  def setPeer(thePeer: Station): Unit = {
	this.peerStation = thePeer
	//println("Peer Set")
  }  
  
  def setAPCount(apCount: Int){
    this.accessPointCount = apCount
  }
  
  def setRoutingTable(routingTable: Array[Array[Station]]){
    this.theRoutingTable = routingTable
  }
  
  //The Station Actor
  def stationActor: Actor = {
    val theStationActor = actor{      
      
      //Access Point
      if(this.stationType == "AP"){
        //var theASNumber: Int = 0
        //var theBSNumber: Int = 0
        var theStationA: Actor = null
        var theStationB: Actor = null
        var aptCount: Int = 0
        //println("Inside AP: Access Point Count: "+this.accessPointCount)
        //Waiting For Messages        
        loop{
          self.react{
            case "ThroughAP" =>
              //Timeout for 5 millisec
              //println("Through AP Communication: Timeout for 5 sec")
              Thread.sleep(5)
              //AP sends Acknowledgement to A
              //println("AP sends Acknowledgement to A")
              sender ! "ThroughAPACK"
                           
            case msg:Message =>
              if(msg.str == "A"){
                //Station A received
                //println("Through AP Communication: Station A received at AP")
                //theASNumber = msg.theSerialNumber
                theStationA = msg.theActor
                sender ! "StationAReceivedACK"
              }                
              else if(msg.str == "B"){
                //Station B received
                //println("Through AP Communication: Station B received at AP")                
                //theBSNumber = msg.theSerialNumber
                theStationB = msg.theActor
                sender ! "StationBReceivedACK"
              }                
            
            case "ThroughAPMessage" =>
              //theBStation.stationActor ! "ThroughAPMessage"
              //this.theRoutingTable(theBSNumber-1)(1).stationActor ! "ThroughAPMessage"
              theStationB ! "ThroughAPMessage"
            case "ThroughAPFinish" =>
              //theBStation.stationActor ! "ThroughAPFinish"
              //this.theRoutingTable(theBSNumber-1)(1).stationActor ! "ThroughAPFinish"
              theStationB ! "ThroughAPFinish"
            
            case "ThroughAPFinishACK" =>
              //theAStation.stationActor ! "ThroughAPFinishACK"
              //this.theRoutingTable(theASNumber-1)(0).stationActor ! "ThroughAPFinishACK"
              theStationA ! "ThroughAPFinishACK"
              aptCount += 1
              if(aptCount == this.accessPointCount){
                println("---------------------")
                println("ACCESS POINT FINISH EXITS")
                println("---------------------")
                exit  
              }
              
            case "ThroughAPMsgLossACK" =>
              //theAStation.stationActor ! "ThroughAPMsgLossACK"
              //this.theRoutingTable(theASNumber-1)(0).stationActor ! "ThroughAPMsgLossACK"
              theStationA ! "ThroughAPMsgLossACK"
              aptCount += 1
              if(aptCount == this.accessPointCount){
                println("---------------------")
                println("ACCESS POINT LOSS EXITS")
                println("---------------------")
                exit  
              }               
          }
        }
      }//Access Point Ends
      
      //Normal Station
      else{
        
        //Station A
        if(this.sending == 1){
          //println("--------- STATION A ------------")
          //Direct Communication
          if(this.distanceFromPeer <= 70.71){
            
            //println("Direct communication possible!")
            this.peerStation.stationActor ! "Direct"
            //Waiting for messages
            loop{
              self.react{
                case "DirectACK" =>
                  //A to B Connection Established
                  //println("Direct Communication: A to B Connection Established")
                  //Sending 10 messages to B
                  for(i: Int <- 0 until 500){
                    sender ! "DirectMessage"
                  }
                  sender ! "DirectFinish"
                
                case "DirectFinishACK" =>
                  //All message sent to A
                  //println("Direct Communication: All messages sent to B")
                  this.mainActor ! new Message(null,"PairFinish",this.theSNumber,"Direct")
                  //println("---------------------")
                  //println("STATION A FINISH EXITS")
                  //println("---------------------")
                  exit
                case "DirectMsgLossACK" =>
                  //Some of the messages are lost
                  //println("Direct Communication: Some of the messages are lost while sending to B")
                  this.mainActor ! new Message(null,"PairFinish",this.theSNumber,"Direct")
                  //println("---------------------")
                  //println("STATION A LOSS EXITS")
                  //println("---------------------")
                  exit            
              }
            }
          }
          
          //Communication through AP
          else{	
            //println("Communication through AP from A.")
            this.accessPoint.stationActor ! "ThroughAP"
            //Waiting for messages
            loop{
              self.react{
                case "ThroughAPACK" =>
                  //A to AP to B Connection Established
                  //println("Through AP Communication: A to AP to B Connection Established")
                  //Sending reference of Station A to AP
                  sender ! new Message(self, "A",-1,"")                  
                  //Sending reference of Station B to AP
                  sender ! new Message(this.peerStation.stationActor, "B",-1,"")
                case "StationAReceivedACK" =>
                  //AP has received the reference of Station A
                  //println("Through AP Communication: AP has received the reference of Station A")                  
                case "StationBReceivedACK" =>
                  //AP has received the reference of Station B
                  //println("Through AP Communication: AP has received the reference of Station B")
                  //Sending 100 messages to B via AP
                  for(i: Int <- 0 until 500){
                    sender ! "ThroughAPMessage"
                  }
                  sender ! "ThroughAPFinish"    
                  
                case "ThroughAPFinishACK" =>
                  //All message sent to A
                  //println("Through AP Communication: All messages sent to B")
                  this.mainActor ! new Message(null,"PairFinish",this.theSNumber,"Through AP")
                  //println("---------------------")
                  //println("STATION A FINISH EXITS")
                  //println("---------------------")
                  exit                  
                case "ThroughAPMsgLossACK" =>
                  //Some of the messages are lost
                  //println("Through AP Communication: Some of the messages are lost while sending to B")
                  this.mainActor ! new Message(null,"PairFinish",this.theSNumber,"Through AP")
                  //println("---------------------")
                  //println("STATION A LOSS EXITS")
                  //println("---------------------")
                  exit                  
              }
            }
          }
          
        }//Station A Ends
        
        //Station B
        else{	
          var msgCount: Int = 0
          //println("--------- STATION B ------------")
          loop{
            self.react{
              case "Direct" =>
                //Timeout for 2 sec
                //println("Direct Communication: Timeout for 2 sec")
                Thread.sleep(2)
                //B sends Acknowledgement to A
                //println("B sends Acknowledgement to A")
                sender ! "DirectACK"
              case "DirectMessage" => 
                msgCount += 1
              case "DirectFinish" =>
                if(msgCount == 10){
                  sender ! "DirectFinishACK"
                  //println("---------------------")
                  //println("STATION B FINISH EXITS")
                  //println("---------------------")
                  exit                  
                }else{
                  sender ! "DirectMsgLossACK"
                  //println("---------------------")
                  //println("STATION B LOSS EXITS")
                  //println("---------------------")
                  exit                  
                }
              
              case "ThroughAPMessage" => 
                msgCount += 1
              case "ThroughAPFinish" =>
                //println("Communication through AP from B.")
                if(msgCount == 10){
                  sender ! "ThroughAPFinishACK"
                  //println("---------------------")
                  //println("STATION B FINISH EXITS")
                  //println("---------------------")
                  exit                  
                }else{
                  sender ! "ThroughAPMsgLossACK"
                  //println("---------------------")
                  //println("STATION B LOSS EXITS")
                  //println("---------------------")
                  exit                  
                }                
            }
          }
        }//Station B Ends
        
      }//Normal Station Ends
    }
    theStationActor
  }  
  
}

//The computation class - it 
//Generates a random number
//And computes distance between two points in the Cartesian 2D space
class Computation{

  //Generates Random number
  def generateRandom(seed: Int): Int = {    
    return new Random().nextInt(seed)
  }
  
  //Computes distance between two points in the Cartesian 2D space
  def computeDistance(x1: Int,y1: Int,x2: Int,y2: Int): Double = {
    return Math.sqrt( (x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1) );
  }
}