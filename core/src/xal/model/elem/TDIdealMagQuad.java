/**
 * 
 */
package xal.model.elem;

/**
 * @author kritha 20160514
 *
 */
public class TDIdealMagQuad extends IdealMagQuad {
	public QuadWaveForm quadWaveForm;
	public static final String s_strType = "TDIdealMagQuad";

	public TDIdealMagQuad() {
		// TODO Auto-generated constructor stub
		super();
		quadWaveForm=new QuadWaveForm();
	}
	
	public TDIdealMagQuad(String strId,
	        int enmOrient,
	        double dblFld,
	        double dblLen) {
		// TODO Auto-generated constructor stub
		super(strId,enmOrient,dblFld,dblLen);
		quadWaveForm=new QuadWaveForm();
	}
	
	public void updateParams(double time) {
		quadWaveForm.cacl(time);
		setMagField(quadWaveForm.getStrength());
	}
	
	public boolean isTimeDependent(){
		return true;
	}
	
	public QuadWaveForm getWaveForm() {
		return quadWaveForm;
	}

	public void setWaveForm(QuadWaveForm quadWaveForm) {
		this.quadWaveForm = quadWaveForm;
	}
	
}
