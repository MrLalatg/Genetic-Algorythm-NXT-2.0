import java.util.ArrayList;
import java.util.Arrays;
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

class indi{
	LightSensor RLight = new LightSensor(SensorPort.S1);
	LightSensor LLight = new LightSensor(SensorPort.S4);
	NXTMotor RMotor = new NXTMotor(MotorPort.A);
	NXTMotor LMotor = new NXTMotor(MotorPort.B);
	double kp;
	double kd;
	double ki;
	double c;
	
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
		pwrL = c + kp*err - kd*d + ki*i;
		pwrR = c - kp*err + kd*d - ki*i;
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
		kp = op;
		kd = od;
		ki = oi;
		c = oc;
		cnt++;
	}
}

class experiment{
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
	indi tempGen[] = {};
	indi[] selection() {
		for(int i = 0; i<= curGen.length; i++) {
			curGen[i].run();
			if(curGen[i].reason == "Success") {
				if(tempGen.length==0) {
					tempGen[1] = curGen[i];
				} else {
					tempGen[tempGen.length-1] = curGen[i];
				}
			}
		}
		
		return tempGen;
	}
	
	indi[] cross() {
		
	}
	
	indi dichC(indi i1, indi i2) {
		indi result;
		
	}
	
//	int[][] choose(indi[] gen) {
//		int result[][];
//		if(curGen.length%2 == 0) {
//			for(int i = 0; i<curGen.length; i++) {
//				
//			}
//		}
//	}
}

public class main {
	
	public static void main(String[] args) {
		
	}

}
