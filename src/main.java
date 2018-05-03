import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.util.Delay;
import lejos.util.Stopwatch;

class writer {
	void fw(indi obj, String filename) {
		FileOutputStream fos = null;
		File inform = new File(filename + ".txt");
		
	    try {
	        fos = new FileOutputStream(inform, true);
	    } catch(IOException ex) {
	      	System.err.println("Failed to create output stream");
	      	Button.waitForAnyPress();
	      	System.exit(1);
	    }
	    
	    DataOutputStream dOut = new DataOutputStream(fos);
	    
	    try {
	    	dOut.writeChars("KP:" + obj.coef.get("kp").toString() + "\n");
	    	dOut.writeChars("KD:" + obj.coef.get("kd").toString() + "\n");
	    	dOut.writeChars("KI:" + obj.coef.get("ki").toString() + "\n");
	    	dOut.writeChars("C:" + obj.coef.get("c").toString() + "\n");
	    	dOut.writeChars("REASON:" + obj.reason + "\n");
	    	dOut.writeChars("ID:" +obj.id + "\n");
	    	dOut.writeChars("POKOLENIE:" + obj.pok + "\n");
	    	dOut.writeChars("TIME:" + obj.time + "\n");
	    	fos.close();
	    } catch(IOException ex) {
	    	System.err.println("Failed to write line into the file");
	    	Button.waitForAnyPress();
	    	System.exit(1);
	    }
	}
	
	public void message(String msg) {
		FileOutputStream fs = null;
		File log = new File("mylog.txt");
		
		try {
			fs = new FileOutputStream(log, true);
			DataOutputStream out = new DataOutputStream(fs);
			out.writeChars(msg + "\n");
			out.close();
		} catch(IOException ex) {
			
		}
	}
}

class indi{
	LightSensor RLight = new LightSensor(SensorPort.S1);
	LightSensor LLight = new LightSensor(SensorPort.S4);
	NXTMotor RMotor = new NXTMotor(MotorPort.A);
	NXTMotor LMotor = new NXTMotor(MotorPort.B);
	
	@SuppressWarnings("deprecation")
	Map<String, Double> coef = new HashMap<String, Double>();
	
	int time;
	boolean isParent = false;
	boolean reason;
	int cross = 0;
	int ePr = 0;
	public boolean flag = false;
	int pok;
	int id = 0;
	static int cnt = 0;
	private void pid() {
		double i = 0;
		double d = 0;
		double pwrR = 0;
		double pwrL = 0;
		double err = RLight.readValue() - LLight.readValue();
		i += err;
		d = ePr-err;
		pwrL = coef.get("c") + coef.get("kp")*err - coef.get("kd")*d + coef.get("ki")*i;
		pwrR = coef.get("c") - coef.get("kp")*err + coef.get("kd")*d - coef.get("ki")*i;
		Motor.A.suspendRegulation();
		Motor.B.suspendRegulation();
		RMotor.setPower((int) pwrR);
		LMotor.setPower((int) pwrL);
		if(RLight.readValue()<40 && LLight.readValue()<40) {
			 cross+=1;
			 Sound.systemSound(true, 0);
				switch(cross) {
				case 1:
					RMotor.setPower(50);
					LMotor.setPower(50);
					RMotor.forward();
					LMotor.forward();
					Delay.msDelay(500);
				
				}
		}
		
	}	
	public void run() {
		writer w = new writer();
		w.message(id + " " + pok + " " + cross + " " + "run start");
		Delay.msDelay(1000);
		Stopwatch sw = new Stopwatch();
		reason = true;
		while(cross<2) {
			pid();
			if(Button.ENTER.isPressed()) {
				w.message(id + " " + pok + " " + cross + " " + "passed into buttonPress");
				reason = false;
				break;
			}
		}
		time = sw.elapsed();
		RMotor.stop();
		LMotor.stop();
		w.message(id + " " + pok + " " + cross + " " + "exitting run()");
	}
	public indi(double op, double od, double oi, double oc, int opok) {
		id = cnt;
		coef.put("kp", op);
		coef.put("kd", od);
		coef.put("ki", oi);
		coef.put("c", oc);
		pok = opok;
		cnt++;
	}
}

class experiment{
	String[] coefNames = {"kp", "kd", "ki", "c"};
	int maxtime = 100000;
	int n;
	String fPath;
	ArrayList<indi> curGen = new ArrayList<>();
	
	experiment(ArrayList<indi> ar, String path, int numgen) {
		curGen = ar;
		fPath = path;
		n = numgen;
	}		  	 
	ArrayList<indi> selection() {
		Sound.systemSound(true, 1);
		writer w = new writer();
		ArrayList<indi> tempGen = new ArrayList<>();
		for(int i = 0; i < curGen.size(); i++) {
			if(isAlive(curGen.get(i))) {
				tempGen.add(curGen.get(i));
				w.message("IsAlive = TRUE");
			} else {
				w.message("IsAlive = FALSE");
			}
		}
		w.message("LengthAfterSelection:" + tempGen.size());
		return tempGen;
	}
	
	ArrayList<indi> cross(ArrayList<indi> gen) {
		Sound.systemSound(true, 4);
		ArrayList<indi> tempGen = new ArrayList<>();
		writer w = new writer();
		int countInd = 0;
		indi i1 = null;
		indi i2 = null;
		for(indi ind: gen) {
			if(!ind.isParent) {
				if(countInd == 0) {
					i1 = ind;
					ind.isParent = true;
					countInd++;
				} else if(countInd == 1) {
					i2 = ind;
					ind.isParent = true;
					tempGen.add(dichC(i1,i2));
					countInd = 0;
				}
			}
		}
		w.message("LengthAfterCross:" + tempGen.size());
		return tempGen;
	}
	
	indi dichC(indi i1, indi i2) {
		indi result = new indi(0,0,0,0,i1.pok+1);
		writer w = new writer();
		Random rnd = new Random();
		for(String tcoef : coefNames) {
			w.message(tcoef);
			if(rnd.nextInt(2)==0) {
				result.coef.remove(tcoef);
				result.coef.put(tcoef, i1.coef.get(tcoef)); 
			} else if(rnd.nextInt(2) == 1) {
				result.coef.remove(tcoef);
				result.coef.put(tcoef, i2.coef.get(tcoef)); 
			}
		}
		return result;
	}
	
	void run() {
		writer w = new writer();
		for(int j = 0; j<=n-1; j++) {
			for(int i = 0; i<curGen.size(); i++) {
				curGen.get(i).run();
				
				w.fw(curGen.get(i), fPath);
				Button.waitForAnyPress();
			}
			w.message("Generating new gen");
			Sound.systemSound(true, 3);
			curGen = selection();
			w.message("CurGenLength:" + curGen.size());
			curGen = cross(curGen);
			Sound.systemSound(true, 2);
			w.message("new gen completed");
			
		}
	}
	
	boolean isAlive(indi ind) {
		writer w = new writer();
		w.message("IA TIME:" + ind.time + " REASON:" + ind.reason + " MAXTIME:" + maxtime);
		if(ind.time < maxtime && ind.reason) {
			Sound.playTone(1200, 2000);
			return true;
		} else {
			Sound.playTone(500, 500);
			Sound.playTone(400, 500);
			Sound.playTone(300, 500);
			Sound.playTone(200, 500);
			return false;
		}
	}
	
}

public class main {

	public static void main(String[] args) {
		Button.ESCAPE.addButtonListener(new ButtonListener() {

			@Override
			public void buttonPressed(Button b) {
				System.exit(1);
				
			}

			@Override
			public void buttonReleased(Button b) {
				
			}});
		
		ArrayList<indi> pok1 = new ArrayList<>();
		pok1.add(new indi(1.7, 3.8, 0.003, 40, 1));
		pok1.add(new indi(1.6, 3.9, 0.003, 45, 1));
		pok1.add(new indi(1.3, 3.6, 0.004, 70, 1));
		pok1.add(new indi(0.9, 4.0, 0.001, 41, 1));
		pok1.add(new indi(1.8, 3.7, 0.002, 40, 1));
		pok1.add(new indi(1.5, 3.5, 0.005, 60, 1));
		pok1.add(new indi(1.2, 2, 0.003, 42, 1));
		pok1.add(new indi(1.1, 3.1, 0.006, 55, 1));
		pok1.add(new indi(1.3, 3.6, 0.004, 38, 1));
		pok1.add(new indi(1.6, 3.9, 0.03, 44, 1));
		Button.waitForAnyPress();
		experiment e = new experiment(pok1, "result", 10);
		e.run();
	}

} 
