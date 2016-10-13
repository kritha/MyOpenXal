/**
 * 
 */
package xal.model.elem;

/**
 * @author kritha 20160514
 *
 */
public class TDIdealMagWedgeDipole2 extends IdealMagWedgeDipole2 {
	public DipoleWaveForm dipoleWaveForm;
	public static final String s_strType = "TDIdealMagWedgeDipole";

	public TDIdealMagWedgeDipole2() {
		// TODO Auto-generated constructor stub
		super(null);
		dipoleWaveForm=new DipoleWaveForm();
	}
	
	public TDIdealMagWedgeDipole2(String strId) {
		// TODO Auto-generated constructor stub
		super(strId);
		dipoleWaveForm=new DipoleWaveForm();
	}
	
	public void updateParams(double time) {
		dipoleWaveForm.cacl(time);
		setMagField(dipoleWaveForm.getStrength());
	}
	
	public boolean isTimeDependent(){
		return true;
	}
	
	public DipoleWaveForm getWaveForm() {
		return dipoleWaveForm;
	}

	public void setWaveForm(DipoleWaveForm dipoleWaveForm) {
		this.dipoleWaveForm = dipoleWaveForm;
	}
}
