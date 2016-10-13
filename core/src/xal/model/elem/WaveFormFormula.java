/**
 * 
 */
package xal.model.elem;

/**
 * @author kritha 20160514
 *
 */
public abstract class WaveFormFormula extends WaveForm {
	protected double[] initParams;
	
	public WaveFormFormula(String type) {
		// TODO Auto-generated constructor stub
		super(type);
	}

	public double[] getInitParams() {
		return initParams;
	}

	public void setInitParams(double[] initParams) {
		this.initParams = initParams;
	}

}
