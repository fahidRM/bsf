package CustomXMPP;import jason.asSyntax.*;import jason.environment.*;import jason.asSemantics.*;import jason.RevisionFailedException;import org.jivesoftware.smack.XMPPException;import edu.bath.sensorframework.DataReading;import edu.bath.sensorframework.Visualisation;import edu.bath.sensorframework.client.*;import edu.bath.sensorframework.JsonReading;import static jason.asSyntax.ASSyntax.*;import java.util.logging.*;import java.util.*;import java.io.BufferedReader;import java.io.FileReader;public class XMPPWorld extends Environment {	class XMPPTimeCheck	{		String lastUpdater="";		long lastUpdateTime=0;	}		class AVehicle	{		String vehicleName="";		double x,y,z,orientation;		long updatedTime=0L;	}	private boolean firstNorm = true;    	private Logger logger = Logger.getLogger("vehicleScenarioOne.mas2j."+XMPPWorld.class.getName());	private ArrayList<String> receivedData;	private ArrayList<AVehicle> knownVehicles = new ArrayList<AVehicle>();	private static String jasonSensorVehicles = "jasonSensorVehicles";	private static String jasonSensorVehiclesCmds = "jasonSensorVehiclesCmds";	private SensorClient mySensorClient, instSensorClient;	private static String XMPPServer = "127.0.0.1";	private XMPPTimeCheck xmppTimeChecker;	private InstSensClient myInstClient;	private static String aoiNodeName = "aoiSensor";	private boolean firstObl=true;	private long startupTime =0L;	private long startupDelay =1000L;	private static boolean useXMPP=false;	private static boolean useMQTT=false;    /** Called before the MAS execution with the args informed in .mas2j */    @Override    public void init(String[] args) {		super.init(args);			try		{			BufferedReader br = new BufferedReader(new FileReader("config.txt"));			String line;			while((line = br.readLine()) != null) 			{				if (line.contains("OPENFIRE"))				{					String[] configArray = line.split("=");					XMPPServer = configArray[1];					//System.out.println("Using config declared IP address of openfire server as: " + XMPPServer);				}				if (line.contains("COMMUNICATION"))				{					String[] configArray = line.split("=");					if(configArray[1].equals("MQTT"))					{						useMQTT=true;					}					else if(configArray[1].equals("XMPP"))					{						useXMPP=true;					}					//System.out.println("Using config declared IP address of openfire server as: " + XMPPServer);				}			}			if (!useMQTT && !useXMPP)			{				System.out.println("no COMMUNICATION value found in config.txt, should be = MQTT or XMPP");				System.exit(1);			}					}		catch (Exception e) {			System.out.println("error getting config file..");			e.printStackTrace();		}	if (useXMPP)	{		System.out.println("XMPP Connection");		while(mySensorClient == null) 		{			try {				mySensorClient = new SensorXMPPClient(XMPPServer, "XMPPWorld", "jasonpassword");			} catch (XMPPException e1) {				System.out.println("Exception in establishing client for XMPPWorld");				e1.printStackTrace();			}		}			while(instSensorClient == null) 		{			try {				instSensorClient = new SensorXMPPClient(XMPPServer, "XMPPWorldInstClient", "jasonpassword");				//List<String> clearData = instSensorClient.getPendingData("NODE_NORM");								//System.out.println("XMPP World institution connected up OK, cleared " + clearData.size());			} catch (Exception e1) {				System.out.println("Exception in establishing inst client for XMPPWorldInstClient");				e1.printStackTrace();			}		}	}	else if (useMQTT)	{		System.out.println("MQTT subscription");		try {			mySensorClient = new SensorMQTTClient(XMPPServer, "XMPPWorld");		} catch (Exception e1) {			System.out.println("Exception in establishing client for XMPPWorld");					e1.printStackTrace();		}		try {			instSensorClient = new SensorMQTTClient(XMPPServer, "XMPPWorldInstClient");			//List<String> clearData = instSensorClient.getPendingData("NODE_NORM");							//System.out.println("XMPP World institution connected up OK, cleared " + clearData.size());		} catch (Exception e1) {			System.out.println("Exception in establishing inst client for XMPPWorldInstClient");			e1.printStackTrace();		}	}	startupTime = System.currentTimeMillis(); 		// for norms	instSensorClient.addHandler("NODE_NORM", new ReadingHandler() {		public void handleIncomingReading(String node, String rdf) {			if ((startupTime + startupDelay) < System.currentTimeMillis())			{			try				{				JsonReading jr = new JsonReading();				jr.fromJSON(rdf);				//if xmpp server isn't caching then dont need this i think				firstNorm=false;				if (firstNorm)				{					System.out.println("WARNING: Assuming caching, so discarding first norm reading: " + rdf);					firstNorm=false;				}				else				{								JsonReading.Value val = jr.findValue("COUNT");				long i = (long) val.m_object;				//System.out.println("got COUNT of " + i);				val = jr.findValue("CONTENT");				if (val != null)				{					String mainVal = val.m_object.toString();					for (int idx = 0; idx < i; idx++)					{						String str = mainVal + idx;						//System.out.println("trying to findValue on " + str);						val = jr.findValue(str);						if (val != null)						{							//System.out.println("obligation: " + val.m_object.toString());							addData(val.m_object.toString());						}						else						{							System.out.println("searching for " + str + " failed");						}					}				}				else				{					System.out.println("null val found for CONTENT");				}				//InstSensJSONConv instConverter = new InstSensJSONConv();				//String jsonVal = instConverter.findValue("STATE", rdf);				//addData(jsonVal);				}			} catch (Exception e) {				System.out.println("error in instSensorClient.addHandler..");				e.printStackTrace();			}			}			else			{				System.out.println("ignoring inst readings as only just started");			} 		}	});			try {			instSensorClient.subscribe("NODE_NORM");	} catch (Exception xe) {		System.out.println("failed to subscribe: " + "NODE_NORM");		xe.printStackTrace();	}			mySensorClient.addHandler(aoiNodeName, new ReadingHandler() 		{ 			@Override			public void handleIncomingReading(String node, String rdf) 			{				if ((startupTime + startupDelay) < System.currentTimeMillis())				{				try 				{					DataReading dr = DataReading.fromRDF(rdf);					String takenBy = dr.getTakenBy();					DataReading.Value aoiLight = dr.findFirstValue(null, "http://127.0.0.1/AOISensors/upcomingLight", null);					if(aoiLight != null)					{						String tempAOIval = (String)aoiLight.object;						//System.out.println("told about an AOI light: " + tempAOIval);						addData("upcomingTrafficLight,"+tempAOIval+","+takenBy);					}					DataReading.Value aoiVehNames = dr.findFirstValue(null, "http://127.0.0.1/AOISensors/vehicleNames", null);					DataReading.Value aoiVehXs = dr.findFirstValue(null, "http://127.0.0.1/AOISensors/vehicleX", null);					DataReading.Value aoiVehYs = dr.findFirstValue(null, "http://127.0.0.1/AOISensors/vehicleY", null);					DataReading.Value aoiVehSameLane = dr.findFirstValue(null, "http://127.0.0.1/AOISensors/vehicleSameLane", null);					if((aoiVehNames != null) && (aoiVehXs != null) && (aoiVehYs != null))					{						if (aoiVehSameLane != null)						{							//System.out.println("was told lane check values too");						}						//System.out.println("wow was told about some vehicles in my AOI");						String nameString = (String)aoiVehNames.object;						String xString = (String)aoiVehXs.object;						String yString = (String)aoiVehYs.object;						String lanesString = (String)aoiVehSameLane.object;						String[] allNames = nameString.split(",");						String[] allXs = xString.split(",");						String[] allYs = yString.split(",");						String[] allLanes = lanesString.split(",");						if ((allNames.length == allXs.length) && (allXs.length == allYs.length))						{							for (int i=0; i<allNames.length; i++)							{								addData("aoiVehicleDetection,"+allNames[i]+","+allXs[i]+","+allYs[i]+","+allLanes[i]+","+takenBy);							}						}						else						{							System.out.println("mismatch on argument number of aoi vehicle detections");						}												//System.out.println(aoiVehNames.object);					}				}				catch (Exception e)				{					System.out.println("Error adding new message to queue..");					e.printStackTrace();				}				}				else				{					System.out.println("ignoring aoiNodeName readings as just started up");				}			}		});		try {			mySensorClient.subscribe(aoiNodeName);		} catch (Exception e1) {			System.out.println("Exception while subscribing to " + aoiNodeName);			e1.printStackTrace();		}		xmppTimeChecker =  new XMPPTimeCheck();	mySensorClient.addHandler(jasonSensorVehicles, new ReadingHandler() {	@Override	public void handleIncomingReading(String node, String rdf) {		if ((startupTime + startupDelay) < System.currentTimeMillis())		{		try 		{			DataReading dr = DataReading.fromRDF(rdf);			String takenBy = dr.getTakenBy();			if (takenBy.contains("http://127.0.0.1/vehicles/"))			{						//used for spatial: position and orientation				DataReading.Value spatialVal = dr.findFirstValue(null, "http://127.0.0.1/sensors/types#spatial", null);				DataReading.Value speedVal = dr.findFirstValue(null, "http://127.0.0.1/sensors/types#vehicleSpeed", null);				if(spatialVal != null) {					Double vSpeed = 0d;					if (speedVal != null)					{						vSpeed= (Double)speedVal.object;						//System.out.println("received speed of " + vSpeed + "!!");					}					String msgString = (String)spatialVal.object;					if (xmppTimeChecker.lastUpdater.equals(takenBy))					{						if (dr.getTimestamp() < xmppTimeChecker.lastUpdateTime)						{							System.out.println("XXX: Received DataReading out of order!!!! " + takenBy + " new msg time: " + dr.getTimestamp() + " and previous timestamp was: " + xmppTimeChecker.lastUpdateTime);						}					}					xmppTimeChecker.lastUpdateTime = dr.getTimestamp();					xmppTimeChecker.lastUpdater = takenBy;					msgString = msgString+","+vSpeed;					addData("spatial,"+msgString+","+takenBy);					//System.out.println("spatial update from: " + takenBy + " saying " + msgString);				}                            	DataReading.Value vehHealthVal = dr.findFirstValue(null, "http://127.0.0.1/sensors/types#healthState", null);                            	if(vehHealthVal != null)                             	{                                	String tempHealthReading = (String)vehHealthVal.object; 					addData("health,"+tempHealthReading+","+takenBy);                            	}			}			else if (takenBy.contains("http://127.0.0.1/sim/sumo"))			{				DataReading.Value timeVal = dr.findFirstValue(null, "http://127.0.0.1/sensors/types#simTime", null);				if (timeVal != null)				{					int timeValue = (int)timeVal.object;										//only interested in whole second updates, anything quicker is too fast really					//VB not sure thats true actually...					//if(timeValue % 1000 == 0)					//{						//System.out.println("told sim time of " + timeValue);							removePerceptsByUnif(Literal.parseLiteral("time(_)"));						addPercept(Literal.parseLiteral("time("+timeValue+")"));					//}				}			}			else			{				//System.out.println(takenBy + " not relevant to me");			}		}		catch(Exception e) 		{			System.out.println("error in mySensorClient.addHandler for some incoming reading..");			e.printStackTrace();		}		}		else		{			System.out.println("ignoring jasonSensorVehicles readings as just started up");		}	}	});			try {		mySensorClient.subscribe(jasonSensorVehicles);	} catch (Exception e1) {		System.out.println("Exception while subscribing to sensor.");		e1.printStackTrace();	}		    }		public void addData(String newItem)	{		String[] newData = newItem.split(",");		if (newData[0].equals("spatial"))		{			String[] agentArrayText= newData[6].split("/");			String agNameSend = agentArrayText[4];								//update locally held list of vehicles which are known about within this environment.. 			boolean updatedVehicle = false;			for (AVehicle tempVeh : knownVehicles) 			{				if (tempVeh.vehicleName.equals(agNameSend))				{					tempVeh.x=Double.parseDouble(newData[1]);					tempVeh.y=Double.parseDouble(newData[2]);					tempVeh.z=Double.parseDouble(newData[3]);					tempVeh.orientation=Double.parseDouble(newData[4]);					tempVeh.updatedTime=System.currentTimeMillis();					updatedVehicle = true;					//System.out.println("updated existing vehicle " + agNameSend);				}			}			if (!updatedVehicle)			{				AVehicle newVehicle = new AVehicle();				newVehicle.x=Double.parseDouble(newData[1]);				newVehicle.y=Double.parseDouble(newData[2]);				newVehicle.z=Double.parseDouble(newData[3]);				newVehicle.orientation=Double.parseDouble(newData[4]);				newVehicle.vehicleName = agNameSend;				newVehicle.updatedTime=System.currentTimeMillis();				knownVehicles.add(newVehicle);;			}						//Make sure only creating percept updates 			//System.out.println("Now have " + knownVehicles.size() + " vehicles in XMPPWorld");			if(!getEnvironmentInfraTier().getRuntimeServices().getAgentsNames().contains(agNameSend))			{				//System.out.println(agNameSend + " is not a Jason agent, not updating");			}			else			{				Literal newLit = Literal.parseLiteral("info("+newData[1]+","+newData[2]+","+newData[3]+",0,"+newData[5]+","+newData[4]+",0)");				Literal clearLit = Literal.parseLiteral("info(_,_,_,_,_,_,_)");							//I had thought this dropped the update rate by 50%, but actually, I think  it's only dropping				//on the cases where the vehicle position hasn't moved.. which isn't so bad				//remove the old percept, new one will be added next time..				if (containsPercept(agNameSend, newLit))				{					///System.out.println("agent already contains this percept!!");					int numCleared2 = removePerceptsByUnif(agNameSend,clearLit);					///System.out.println("deleted " + numCleared2 + " info percepts from " + agNameSend);				}				//remove the old position percept and add the new one				else				{					int numCleared2 = removePerceptsByUnif(agNameSend,clearLit);					addPercept(agNameSend, newLit);				}			}			///informAgsEnvironmentChanged(agNameSend);					}		else if (newData[0].startsWith("obl("))		{				String[] oblData = newItem.split("\\(");			if (oblData.length == 4 )			{				String oblTarget=oblData[2].split("\\)")[0];				String oblType=oblData[1];				System.out.println("oblTarget: " + oblTarget + " and type " + oblType);							Literal oblLit = createLiteral(oblType); 				if (oblType.equals("slowDown"))				{					String speedMod = oblData[2].split("\\)")[0].split(",")[1];					oblTarget= oblData[2].split("\\)")[0].split(",")[0];					oblLit = createLiteral(oblType, createAtom(speedMod)); 					//System.out.println("now oblTarget: " + oblTarget + " and type " + oblType + " atom " + speedMod);				}				else if (oblType.equals("merge"))				{					String mergeTarget=oblTarget.split(",")[1];					oblTarget=oblTarget.split(",")[0];					oblLit = createLiteral(oblType, createAtom(mergeTarget)); 					AVehicle myVeh=null;					AVehicle mergeVeh=null;					for (AVehicle tempVeh : knownVehicles) 					{						if (tempVeh.vehicleName.equals(mergeTarget))						{							mergeVeh=tempVeh;							System.out.println("found merge target");						}						if (tempVeh.vehicleName.equals(oblTarget))						{							myVeh=tempVeh;							System.out.println("found my veh");						}					}						if (myVeh != null && mergeVeh != null)					{						Double distToMerge = Math.abs(mergeVeh.x-myVeh.x) + Math.abs(mergeVeh.y-myVeh.y) + Math.abs(mergeVeh.z-myVeh.z);						System.out.println("distance to merge veh is " + distToMerge);						oblLit = createLiteral(oblType, createAtom(mergeTarget), createNumber(distToMerge)); 					}					else					{						System.out.println("couldn't find required vehicle info for merge");					}				}								//TODO: messy hack.. :( BETTER!				/*if ((oblType.equals("changeLane")) && (oblTarget.equals("centralMember1")) && !firstObl)				{					if (containsPercept(oblTarget, oblLit))					{						System.out.println(oblTarget + " already contains obligation " + oblLit + " !!");						addPercept(oblTarget, oblLit);					}					else					{						System.out.println("added percept obligation " + oblLit + " to " + oblTarget);						addPercept(oblTarget, oblLit);					}				}				else if ((oblType.equals("changeLane")) && (oblTarget.equals("centralMember1")) && firstObl)				{					System.out.println("ignoring first obligation to change lane, cached value..");					firstObl=false;				}				else if ((oblType.equals("reduceSpeed")) && (oblTarget.equals("centralMember1")) && firstObl)				{					System.out.println("ignoring first obligation to reduceSpeed, cached value..");					firstObl=false;				}				else if ((oblType.equals("slowDown")) && firstObl)				{					System.out.println("ignoring first obligation to slowDown, cached value..");					firstObl=false;				}				else				{*/					if (containsPercept(oblTarget, oblLit))					{						System.out.println(oblTarget + " already contains obligation " + oblLit + " !!");						addPercept(oblTarget, oblLit);					}					else					{						System.out.println("added percept obligation " + oblLit + " to " + oblTarget);						addPercept(oblTarget, oblLit);					}				//}			}			else			{				System.out.println("thought that was an obligation, but its " + oblData.length + " not what expected..");			}		}		else if (newData[0].equals("health"))		{			System.out.println("health msg: " + newItem + " and trying to split " + newData[2]);			String crashedInto = newData[2];			String agFullName = newData[3];			String[] agentArrayText= agFullName.split("/");			String agNameSend = agentArrayText[4];			String healthMsg = newData[1];			Literal healthLit = createLiteral(healthMsg, createAtom(crashedInto));			System.out.println("telling " + agNameSend + " updated health of " + healthLit);			addPercept(agNameSend, healthLit);		}		else if (newData[0].equals("upcomingTrafficLight"))		{			String agFullName = newData[3];			String[] agentArrayText= agFullName.split("/");			String agNameSend = agentArrayText[4];			//String lightMsg = newData[0] + "(" + newData[1] + "," + newData[2] + ")";			//Literal lightLit = createLiteral(lightMsg);			//System.out.println("trying to create literal: " + lightMsg);			Double distVal = Double.parseDouble(newData[1]);  			Literal lightLit = createLiteral(newData[0], createAtom(newData[2]), createNumber(distVal)); 			//Literal lightLit = Literal.parseLiteral(lightMsg);			//System.out.println("telling " + agNameSend + " updated light state of " + lightMsg);			addPercept(agNameSend, lightLit);		}		else if (newData[0].equals("aoiVehicleDetection"))		{	//("aoiVehicleDetection,"+allNames[i]+","+allXs[i]+","+allYs[i]+","+takenBy);			//System.out.println("a" + newItem);			String agFullName = newData[5];			String[] agentArrayText= agFullName.split("/");			String agNameSend = agentArrayText[4];			Double locX = Double.parseDouble(newData[2]);			Double locY = Double.parseDouble(newData[3]);			String sameLane = newData[4];			if (sameLane.equals("y"))			{  				Literal detectionLit = createLiteral(newData[0], createAtom(newData[1]), createNumber(locX), createNumber(locY)); 				addPercept(agNameSend, detectionLit);			}			else			{				//System.out.println("received AOI detection but its not in same lane as me, so ignoring..");			}		}		else		{			System.out.println("env class told " + newItem + " but not handling this yet!");		}	}		private void updatePercepts()	{		//so, we need to clear down agents we've received new percepts for, and add in the new values	}	@Override    public boolean executeAction(String agName, Structure act) {       // System.out.println("UMMM The action  is not implemented in the default environment.");	return true;    }	    /** Called before the end of MAS execution */    @Override    public void stop() {        super.stop();    }}