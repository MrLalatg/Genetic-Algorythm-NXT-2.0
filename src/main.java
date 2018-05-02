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
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.comm.RConsole;
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
	    	fos.close();
	    } catch(IOException ex) {
	    	System.err.println("Failed to write line into the file");
	    	Button.waitForAnyPress();
	    	System.exit(1);
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
	String reason;
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
		if(RLight.readValue()<35 && LLight.readValue()<35) {
			 cross+=1;
		}
		
		switch(cross) {
		case 1:
			RMotor.setPower(50);
			LMotor.setPower(50);
			RMotor.forward();
			LMotor.forward();
			Delay.msDelay(500);
		
		}
	}
	public void message(String msg) {
		FileOutputStream fs = null;
		File log = new File("mylog.txt");
		
		try {
			fs = new FileOutputStream(log, true);
			DataOutputStream out = new DataOutputStream(fs);
			out.writeChars(id + " " + pok + " " + cross + " " + msg + "\n");
			out.close();
		} catch(IOException ex) {
			
		}
	}
	
	public void run() {
		message("run start");
		Stopwatch sw = new Stopwatch();
		reason = "Success";
		while(cross<2) {
			message("while bf pid()");
			pid();
			message("while af pid()");
			if(Button.ENTER.isPressed()) {
				message("Pass into button press");
				reason = "Fail";
				break;
			}
		}
		time = sw.elapsed();
		RMotor.stop();
		LMotor.stop();
		message("Exitting run()");
		return;
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
	double maxtime;
	String fPath;
	ArrayList<indi> curGen = new ArrayList<>();
	
	experiment(ArrayList<indi> ar, String path) {
		curGen = ar;
		fPath = path;
	}		  	 
	ArrayList<indi> selection() {
		ArrayList<indi> tempGen = new ArrayList<>();
		for(int i = 0; i < curGen.size(); i++) {
			curGen.get(i).run();
			if(isAlive(curGen.get(i))) {
				tempGen.add(curGen.get(i));
			}
		}
		return tempGen;
	}
	
	ArrayList<indi> cross(ArrayList<indi> gen) {
		ArrayList<indi> tempGen = new ArrayList<>();
		int flag = 0;
		indi i1 = null;
		indi i2 = null;
		for(indi ind: gen) {
			if(!ind.isParent) {
				if(flag == 0) {
					i1 = ind;
				} else if(flag == 1) {
					i2 = ind;
					tempGen.add(dichC(i1,i2));
				}
			}
		}
		
		return tempGen;
	}
	
	indi dichC(indi i1, indi i2) {
		indi result = new indi(0,0,0,0,i1.pok+1);
		Random rnd = new Random();
		for(String tcoef : coefNames) {
			if(rnd.nextInt(2)==0) {
				result.coef.remove(tcoef);
				result.coef.put(tcoef, i1.coef.get(tcoef)); 
			} else if(rnd.nextInt(2) == 1) {
				result.coef.remove(tcoef);
				result.coef.put(tcoef, i2.coef.get(tcoef)); 
			}
		}
		result.pok++;
		return result;
	}
	
	void run() {
		writer w = new writer();
		for(int j = 0; j<=9; j++) {
			for(int i = 0; i<curGen.size(); i++) {
				curGen.get(i).run();
				
				w.fw(curGen.get(i), fPath);
				LCD.drawString("To run next", 31, 1);
				LCD.drawString("Individual", 31, 2);
				LCD.drawString("Press any button", 31, 3);
				Button.waitForAnyPress();
			}
			curGen = selection();
			curGen = cross(curGen);
			
		}
	}
	
	boolean isAlive(indi ind) {
		if(/*ind.time < maxtime &&*/ ind.reason == "Success") {
			return true;
		} else {
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
		pok1.add(new indi(1.7, 3.8, 0.003, 30, 1));
		pok1.add(new indi(1.6, 3.9, 0.003, 35, 1));
		pok1.add(new indi(1.3, 3.6, 0.004, 28, 1));
		pok1.add(new indi(1.4, 4.0, 0.001, 31, 1));
		pok1.add(new indi(1.8, 3.7, 0.002, 40, 1));
		pok1.add(new indi(1.5, 3.5, 0.005, 29, 1));
		pok1.add(new indi(1.2, 3.5, 0.003, 32, 1));
		pok1.add(new indi(1.1, 3.1, 0.006, 45, 1));
		pok1.add(new indi(1.3, 3.6, 0.004, 28, 1));
		pok1.add(new indi(1.6, 3.9, 0.003, 34, 1));
		Button.waitForAnyPress();
		experiment e = new experiment(pok1, "test");
		e.run();
	}

} 
