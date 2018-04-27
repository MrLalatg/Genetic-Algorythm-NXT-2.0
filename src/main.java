import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.*;
import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.MotorPort;
import lejos.nxt.NXTMotor;
import lejos.nxt.SensorPort;

class writer {
	void fw(indi obj) {
		FileOutputStream fos = null;
		File inform = new File("inf.txt");
		
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
	    	dOut.writeChars("ID:" + obj.id + "\n");
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
	
	double time;
	boolean isParent = false;
	String reason;
	int cross = 0;
	int ePr = 0;
	boolean flag = false;
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
		if(RLight.readValue()<50 && LLight.readValue()<50) {
			 cross+=1;
		}
	}
	
	
	public void run() {
		while(cross<2) {
			pid();
			if(flag) {
				reason = "Fail";
			}
		}
		reason = "Success";
	}
	public indi(double op, double od, double oi, double oc, int opok) {
		Button.ENTER.addButtonListener(new ButtonListener() {
			public void buttonPressed(Button b){
				LCD.drawString("PROGRAM INTERRUPTED", 31, 3);
			}
			
			public void buttonReleased(Button b) {
				LCD.clear();
				flag = true;
			}
		});
		id = cnt;
		coef.put("kp", op);
		coef.put("kd", od);
		coef.put("ki", oi);
		coef.put("c", oc);
		cnt++;
	}
}

class experiment{
	String[] coefNames = {"kp", "kd", "ki", "c"};
	double maxtime;
	indi curGen[] = {new indi(1.7, 3.8, 0.003, 30, 1),
			  	 new indi(1.6, 3.9, 0.003, 35, 1),
			  	 new indi(1.3, 3.6, 0.004, 28, 1),
			  	 new indi(1.4, 4.0, 0.001, 31, 1),
			  	 new indi(1.8, 3.7, 0.002, 40, 1),
			  	 new indi(1.5, 3.5, 0.005, 29, 1),
			  	 new indi(1.2, 3.5, 0.003, 32, 1),
			  	 new indi(1.1, 3.1, 0.006, 45, 1),
			  	 new indi(1.3, 3.6, 0.004, 28, 1),
			  	 new indi(1.6, 3.9, 0.003, 34, 1)};
	indi[] selection() {
		indi tempGen[] = {};
		for(int i = 0; i<= curGen.length; i++) {
			curGen[i].run();
			if(isAlive(curGen[i])) {
				if(tempGen.length == 0) {
					tempGen[0] = curGen[i];
				} else if(tempGen.length == 1) {
					tempGen[1] = curGen[i];
				} else {
					tempGen[tempGen.length-1] = curGen[i];
				}
			}
		}
		
		return tempGen;
	}
	
	indi[] cross(indi gen[]) {
		indi tempGen[] = {};
		int flag = 0;
		indi i1 = null;
		indi i2 = null;
		for(indi ind: gen) {
			if(!ind.isParent) {
				if(flag == 0) {
					i1 = ind;
				} else if(flag == 1) {
					i2 = ind;
					if(tempGen.length == 0) {
						tempGen[0] = dichC(i1, i2);
					} else if(tempGen.length == 1) {
						tempGen[1] = dichC(i1, i2);
					} else {
						tempGen[tempGen.length-1] = dichC(i1, i2);
					}
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
		
		return result;
	}
	
	void run() {
		writer w = new writer();
		for(int j = 0; j<=9; j++) {
			for(int i = 0; i<=curGen.length; i++) {
				curGen[i].run();
				
				w.fw(curGen[i]);
				LCD.drawString("To run next individual\n press any button", 31, 4);
				Button.waitForAnyPress();
			}
			curGen = selection();
			curGen = cross(curGen);
			
		}
	}
	
	boolean isAlive(indi ind) {
		if(ind.time < maxtime && ind.reason == "Success") {
			return true;
		} else {
			return false;
		}
	}
	
}

public class main {
	
	public static void main(String[] args) {
//		experiment e = new experiment();
//		e.run();
		Button.waitForAnyPress();
		indi test = new indi(1.7, 3.8, 0.003, 30, 1);
		test.run();
	}

}
