/**
 * 
 */
package xal.model.elem;

/**
 * @author kritha 20160517
 *
 */
public class DipoleWaveForm extends WaveFormFormula{
	private double strength;
//	private final double tr=0.49;//ramping time
//	private final double tt=0.5;//total time
//	private final double tc=0.01;//capture time
//	private final double Bi=0.251023978256972;//initial B
//	private final double Bf=1.521694559177570;//final B
	public DipoleWaveForm(double strength) {
		// TODO Auto-generated constructor stub
		super("magnet waveform");
		this.strength=strength;
	}
	
	public DipoleWaveForm() {
		// TODO Auto-generated constructor stub
		this(0.251023978256972);
	}
	
//	public void initialize() {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void cacl(double time) {
		// TODO Auto-generated method stub
		//following formula includes capture
//		final double tt=initParams[0];
//		final double tr=initParams[1];
//		final double tc=initParams[2];
//		final double Bi=initParams[3];
//		final double Bf=initParams[4];
//		final double t1=0.8*tr+tc;
//		if (time>=0 && time<tc) {
//			strength=Bi;
//		} else if (time>=tc && time <t1) {
//			strength=Bi+(Bf-Bi)/0.8*Math.pow((time-tc)/tr, 2);
//		} else if(time>=t1 && time<=tt){
//			strength=Bf-(Bf-Bi)/0.2*Math.pow(1-(time-tc)/tr,2);
//		}else {
//			strength=Bf;
//		}
		
		//following formula inclues only accelerating
		//initPrams数组仍然不变
		//final double tt=initParams[0];
		final double tr=initParams[1];
		final double Bi=initParams[3];
		final double Bf=initParams[4];
		final double t1=0.8*tr;
		if (time>=0 && time <t1) {
			strength=Bi+(Bf-Bi)/0.8*Math.pow(time/tr, 2);
		} else if(time>=t1 && time<=tr){
			strength=Bf-(Bf-Bi)/0.2*Math.pow(1-time/tr,2);
		}else {
			strength=Bf;
		}
	}

	@Override
	public void setParams(double[] params) {
		// TODO Auto-generated method stub
		this.strength=params[0];
	}

	@Override
	public double[] getParams() {
		// TODO Auto-generated method stub
		double[] params = new double[1];
		params[0]=this.strength;
		return params;
	}

	public double getStrength() {
		return this.strength;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}
	
}
