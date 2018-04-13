import lejos.nxt.*;
import lejos.util.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;



class indi{
	LightSensor RLight = new LightSensor(SensorPort.S1);
	LightSensor LLight = new LightSensor(SensorPort.S4);
	NXTMotor RMotor = new NXTMotor(MotorPort.A);
	NXTMotor LMotor = new NXTMotor(MotorPort.B);
	double kp;
	double kd;
	double ki;
	double c;
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
	
	
	public boolean run() {
		while(cross<2) {
			pid();
			if(flag) {
				return(false);
			}
		}
		return(true);
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

public class main {
	
	public static void main(String[] args) {
		indi i1 = new indi(1.7, 3.6, 0.003, 30, 1);
		i1.run();
	}

}