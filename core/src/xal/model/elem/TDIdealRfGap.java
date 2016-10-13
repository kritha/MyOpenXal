/**
 * 
 */
package xal.model.elem;

/**
 * @author kritha
 *
 */
public class TDIdealRfGap extends IdealRfGap {
	public GapWaveForm gapWaveForm;
	public final static String s_strType = "TDIdealRfGap";

	public TDIdealRfGap() {
		// TODO Auto-generated constructor stub
		super();
		gapWaveForm=new GapWaveForm();
	}
	
	public TDIdealRfGap(String strId, double dblETL, double dblPhase, double dblFreq) {
		// TODO Auto-generated constructor stub
		super(strId,dblETL,dblPhase,dblFreq);
		gapWaveForm=new GapWaveForm();
	}
	
	public void updateParams(double time) {
		gapWaveForm.cacl(time);
//		setE0(gapWaveForm.getVoltage()/1.0E6/getLength());
//		setETL(gapWaveForm.getVoltage()/1.0E6);
//		setPhase(gapWaveForm.getPhase());
//		setFrequency(gapWaveForm.getF());
//		System.out.println(getLength());
		setE0(gapWaveForm.getVoltage()/getCellLength());
		setETL(gapWaveForm.getVoltage());
		setPhase(gapWaveForm.getPhase());
		setFrequency(gapWaveForm.getF());
	}
	
	public boolean isTimeDependent(){
		return true;
	}
	
	public GapWaveForm getWaveForm() {
		return gapWaveForm;
	}

	public void setWaveForm(GapWaveForm gapWaveForm) {
		this.gapWaveForm = gapWaveForm;
	}
}