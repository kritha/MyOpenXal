package xal.model.elem;
import java.lang.Math;

import com.cosylab.gui.components.wheelswitch.StaticDigit;
/**
 * @author kritha in 20160514
 *
 */
public class GapWaveForm extends WaveFormFormula{
	//private parameters of Rf Gap
	private double voltage;//Mv/m
	private double phase;//degree,cos form
	private double f;//MHz
	private static double phase0=-Math.PI/2; 
	public GapWaveForm(double vol, double phase, double f) {
		// TODO Auto-generated constructor stub
		super("gap waveform");
		this.voltage=vol;
		this.phase=phase;
		this.f=f;
	}
	
	public GapWaveForm() {
		// TODO Auto-generated constructor stub
		this(50,-90,1.178534574838021);
	}
	
//	public void initialize() {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void cacl(double time) {
		// TODO Auto-generated method stub
		//final double m=1.672621637e-27;
		//final double e=1.6e-19;
//		final double xx=9.821930609140285;//m^2c^2/e^2
//		final double c=299792458;
//		final double tt=initParams[0];
//		final double tr=initParams[1];
//		final double tc=initParams[2];
//		final double Vini=initParams[3];
//		final double Vfin=initParams[4];
//		final double Brhoi=initParams[5];
//		final double Brhof=initParams[6];
//		final double fi=initParams[7];
//		final double ff=initParams[8];
//		final double L=initParams[9];
//		final double t1=0.8*tr+tc;
//		//double phase0=-Math.PI/2;
//		//double T1=initParams[2];
//		if (time>=0 && time<tc) {
//			voltage=Vini+(Vfin-Vini)*(3*Math.pow(time, 2)/Math.pow(tc, 2)-2*Math.pow(time, 3)/Math.pow(tc, 3));
//			//phase0=phase*Math.PI/180;//将上一次的phase保存下来
//			phase=-90;
//			f=fi;
//		}else if (time>=tc && time <t1) {
//			voltage=Vfin;
//			double a=(Brhof-Brhoi)/0.4*(time-tc)/Math.pow(tr, 2)*L/voltage;
//			double phase_rad=-1*Math.acos(a);
//			double deltap=phase_rad-phase0;
//			double Brho=Brhoi+(Brhof-Brhoi)/0.8*Math.pow((time-tc)/tr, 2);
//			double v=Brho*c/Math.sqrt(xx+Brho*Brho);
//			phase0=phase_rad;
//			phase=180/Math.PI*phase_rad;
//			f=v/L*(1+deltap/2/Math.PI)/1000000;
//		}else if(time>=t1 && time<=tt){
//			voltage=Vfin;
//			double a=(Brhof-Brhoi)/0.1/tr*(1-(time-tc)/tr)*L/voltage;
//			double phase_rad=-1*Math.acos(a);
//			double deltap=phase_rad-phase0;
//			double Brho=Brhof-(Brhof-Brhoi)/0.2*Math.pow(1-(time-tc)/tr,2);
//			double v=Brho*c/Math.sqrt(xx+Brho*Brho);
//			phase0=phase_rad;
//			phase=180/Math.PI*phase_rad;
//			f=v/L*(1+deltap/2/Math.PI)/1000000;
//		}else {
//			voltage=Vfin;
//			phase=-90;
//			f=ff;
//		}
		
		
		//following formula inclues only accelerating
		//initPrams数组仍然不变
		final double xx=9.795253096655147;//m^2c^2/e^2
		final double c=299792458;
		//final double tt=initParams[0];
		final double tr=initParams[1];
		//final double tc=initParams[2];
		//final double Vini=initParams[3];
		final double Vfin=initParams[4];
		final double Brhoi=initParams[5];
		final double Brhof=initParams[6];
		//final double fi=initParams[7];
		final double ff=initParams[8];
		final double L=initParams[9];
		final double t1=0.8*tr;
		//double phase0=-Math.PI/2;
		//double T1=initParams[2];
		if (time>=0 && time <t1) {
			voltage=Vfin;
			double Brho=Brhoi+(Brhof-Brhoi)/0.8*Math.pow(time/tr, 2);
			double beta=Brho/Math.sqrt(xx+Brho*Brho);
//			double gamma=1/Math.sqrt(1-beta*beta);
//			double a=(Brhof-Brhoi)/0.4*time/Math.pow(tr, 2)*L*(gamma-1)/gamma/voltage;
			double a=(Brhof-Brhoi)/0.4*time/Math.pow(tr, 2)*L/voltage;
			double phase_rad=-1*Math.acos(a);
			double deltap=phase_rad-phase0;
			phase0=phase_rad;
//			phase=180/Math.PI*phase_rad;
			phase=phase_rad;
//			f=beta*c/L*(1+deltap/2/Math.PI)/1000000;
			f=beta*c/L*(1+deltap/(2*Math.PI));
		}else if(time>=t1 && time<=tr){
			voltage=Vfin;
			double Brho=Brhof-(Brhof-Brhoi)/0.2*Math.pow(1-time/tr,2);
			double beta=Brho/Math.sqrt(xx+Brho*Brho);
//			double gamma=1/Math.sqrt(1-beta*beta);
//			double a=(Brhof-Brhoi)/0.1/tr*(1-time/tr)*L*(gamma-1)/gamma/voltage;
			double a=(Brhof-Brhoi)/0.1/tr*(1-time/tr)*L/voltage;
			double phase_rad=-1*Math.acos(a);
			double deltap=phase_rad-phase0;
			phase0=phase_rad;
//			phase=180/Math.PI*phase_rad;
			phase=phase_rad;
//			f=beta*c/L*(1+deltap/2/Math.PI)/1000000;
			f=beta*c/L*(1+deltap/(2*Math.PI));
		}else {
			voltage=Vfin;
//			phase=-90;
			phase=-Math.PI/2;
			f=ff;
		}
	}
		

	@Override
	public void setParams(double[] params) {
		// TODO Auto-generated method stub
		this.voltage=params[0];
		this.phase=params[1];
		this.f=params[2];
	}

	@Override
	public double[] getParams() {
		// TODO Auto-generated method stub
		double[] params=new double[3];
		params[0]=this.voltage;
		params[1]=this.phase;
		params[2]=this.f;
		return params;
	}

	public double getVoltage() {
		return voltage;
	}

	public void setVoltage(double vol) {
		this.voltage = vol;
	}

	public double getPhase() {
		return phase;
	}

	public void setPhase(double phase) {
		this.phase = phase;
	}

	public double getF() {
		return f;
	}

	public void setF(double f) {
		this.f = f;
	}

}
