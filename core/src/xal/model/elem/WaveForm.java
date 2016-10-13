/**
 * 
 */
package xal.model.elem;

/**
 * @author kritha 20160514
 *
 */
public abstract class WaveForm {
	//private fields
	//0 for initialize from formula, 1 for initialize from file
	protected int InitFlag=0;
	protected String WaveFormType;
	//public methods
	//constructor
	public WaveForm() {
		// TODO Auto-generated constructor stub
	}
	
	public WaveForm(String type) {
		WaveFormType=type;
	}
	
//	//initialize function which must be override
//	public abstract void initialize();
	
	//calculate parameters at a specified time
	public abstract void cacl(double time);
	
	//set parameters of the waveforms
	public abstract void setParams(double[] params);
	
	//get parameters of the waveforms
	public abstract double[] getParams();

	public String getWaveFormType() {
		return WaveFormType;
	}

	public void setWaveFormType(String waveFormType) {
		WaveFormType = waveFormType;
	}

	public int getInitFlag() {
		return InitFlag;
	}

	public void setInitFlag(int initFlag) {
		InitFlag = initFlag;
	}
	
}