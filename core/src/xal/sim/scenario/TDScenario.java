/**
 * 
 */
package xal.sim.scenario;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.net.aso.i;
import xal.model.IAlgorithm;
import xal.model.IComponent;
import xal.model.IElement;
import xal.model.Lattice;
import xal.model.ModelException;
import xal.sim.scenario.Scenario;
import xal.sim.sync.SynchronizationManager;
import xal.smf.Accelerator;
import xal.smf.AcceleratorNode;
import xal.smf.AcceleratorSeq;

/**
 * @author kritha 20160514
 *
 */
public class TDScenario extends Scenario {

	//定义两种同步模式，1是一圈同步一次，0是逐个元件同步
	private int TimeSyncMode=0;
//	private Map<AcceleratorNode,List<IElement>> timeDependentElems = new HashMap<AcceleratorNode,List<IElement>>();
	//改成放在SynchronizationManager中
	protected TDScenario(AcceleratorSeq smfSeq, Lattice mdlLattice, SynchronizationManager mgrSync) {
		super(smfSeq, mdlLattice, mgrSync);
		// TODO Auto-generated constructor stub
	}
	
	/**
     * Creates a new Scenario for the supplied accelerator sequence.
     * 
     * @param smfSeq    the accelerator sequence to build a scenario for
     * @return          a new Scenario for the supplied accelerator sequence
     * 
     * @throws          ModelException error building Scenario
     */
    public static TDScenario newScenarioFor( final AcceleratorSeq smfSeq ) throws ModelException {

        // We have a linear accelerator/transport line - process as such
        Accelerator         smfAccel      = smfSeq.getAccelerator();
        ElementMapping      mapNodeToElem = smfAccel.getElementMapping();
        ScenarioGenerator   mdlGenScnr    = new ScenarioGenerator(mapNodeToElem);

        return mdlGenScnr.generateTDScenario(smfSeq);
    }

	public int getTimeSyncMode() {
		return TimeSyncMode;
	}

	public void setTimeSyncMode(int timeSyncMode) {
		TimeSyncMode = timeSyncMode;
	}

	public Map<AcceleratorNode,List<IComponent>> getTimeDependentElems() {
		return mgrSync.getTimeDependentElems();
	}

	public void setTimeDependentElems(Map<AcceleratorNode,List<IComponent>> timeDependentElems) {
		mgrSync.setTimeDependentElems(timeDependentElems);
	}
	
	
	//注意该方法适用于同步模式1，最好在经过加速腔后调用
	public void setTimeDependentParams() {
        if (lattice == null)
            throw new IllegalStateException(
                "must initialize lattice before running model");
        if (probe == null)
            throw new IllegalStateException(
                "must initialize probe before running model");
        double t=probe.getTime();
        final Collection<AcceleratorNode> nodes = mgrSync.getTimeDependentElems().keySet();
		//System.out.println(nodes);
		
		for ( final AcceleratorNode node : nodes ) {
			//如果需要排除加速腔，需要在这里添加判断，此外该方法最好在经过加速腔后调用
			for ( final IComponent elem : mgrSync.getTimeDependentElems().get( node ) ) {
				elem.updateParams(t);
			}
		} 
	}
	
	//在环里面运行n圈
	public void runTurns(int turns) throws ModelException {
        if (lattice == null)
            throw new IllegalStateException(
                "must initialize lattice before running model");
        if (probe == null)
            throw new IllegalStateException(
                "must initialize probe before running model");
        
        // Set the starting and stopping elements
        IAlgorithm  alg = probe.getAlgorithm();
        
        //下面的代码用于定义algorithm中的起始和结束元件ID和是否使用最后一个element
        if (this.getStartElementId() != null)
            alg.setStartElementId( this.getStartElementId() );
        else
            alg.unsetStartElementId();
            
        if (this.getStopElementId() != null) {
            alg.setStopElementId( this.getStopElementId() );
            alg.setIncludeStopElement(this.isInclStopElem());
        } else
            alg.unsetStopElementId();
        
        // Propagate probe
        //probe.initialize();
        //System.out.println(probe.getInitialState());
        probe.update();
 
		
		//for sync parameters every turn
		if (TimeSyncMode==1) {
			for (int i = 0; i < turns; i++) {
				//更新参数
				//setTimeDependentParams();
//				if (i%1000==0) {
//					//probe.update();
//					System.out.println(i+" turns has been finished, and beam energy is: "+probe.getKineticEnergy());
//				}
				lattice.propagate_everyturn(probe,mgrSync);
			}
		} else {
			for (int i = 0; i < turns; i++) {
				if (i%1000==0) {
					probe.update();
					System.out.println(i+" turns has been finished and beam energy is: "+probe.getKineticEnergy());
				}
				lattice.propagate_everynode(probe);
			}
		}
	}
	
}
