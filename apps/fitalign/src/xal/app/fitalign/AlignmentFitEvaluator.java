/*
 * AlignmentFitEvaluator.java
 *
 * Created on January 23, 2014, 1:32 PM
 */

package xal.app.fitalign;

import java.util.*;

import xal.ca.*;
import xal.smf.*;
import xal.smf.impl.*;
import xal.model.*;
import xal.model.probe.*;
import xal.model.probe.traj.*;
import xal.model.alg.*;
import xal.model.elem.*;
import xal.sim.scenario.*;
import xal.tools.beam.calc.*;
import xal.tools.beam.PhaseVector;


/**
 * AlignmentFitEvaluator is the evaluator used by the solver to evaluate the alignment fit for a given trial.
 * @author  t6p
 */
public class AlignmentFitEvaluator {
	/** misaligned quadrupole used in fitting to the measured orbit */
	final private Quadrupole MISALIGNED_QUADRUPOLE;

	/** measured orbit to which to fit */
	final private BeamOrbit MEASURED_ORBIT;

	/** entrance probe */
	final private Probe ENTRANCE_PROBE;

	/** model scenario */
	final private Scenario SCENARIO;


	/** Constructor */
	public AlignmentFitEvaluator( final Quadrupole misalignedQuadrupole, final BeamOrbit measuredOrbit ) {
		MISALIGNED_QUADRUPOLE = misalignedQuadrupole;
		MEASURED_ORBIT = measuredOrbit;

		final AcceleratorSeq sequence = measuredOrbit.getSequence();

		ENTRANCE_PROBE = getProbe( sequence );

		SCENARIO = createScenario( sequence );
	}


	/** create a new scenario */
	static private Scenario createScenario( final AcceleratorSeq sequence ) {
		try {
			final Scenario scenario = Scenario.newScenarioFor( sequence );
			scenario.setSynchronizationMode( Scenario.SYNC_MODE_DESIGN );
			scenario.resync();
			return scenario;
		}
		catch ( Exception exception ) {
			exception.printStackTrace();
			throw new RuntimeException( "Exception instantiating a new model scenario.", exception );
		}
	}


	/** get the probe for the specified sequence */
	static private Probe getProbe( final AcceleratorSeq sequence ) {
		Probe probe=null;
		try {
			if ( sequence.isLinear() ) {
				// TODO: Instantiate the algorithm (EnvTrackerAdapt) and probe (EnvelopeProbe) for the linear part of the accelerator using the AlgorithmFactory and ProbeFactory				
				EnvTrackerAdapt etracker=AlgorithmFactory.createEnvTrackerAdapt(sequence);
				probe=ProbeFactory.getEnvelopeProbe(sequence, etracker);
				return probe;
			}
			else {
				// TODO: Instantiate the algorithm (TransferMapTracker) and probe (TransferMapProbe) for the ring using the AlgorithmFactory and ProbeFactory
				TransferMapTracker etracker=AlgorithmFactory.createTransferMapTracker(sequence);
				probe=ProbeFactory.getTransferMapProbe(sequence, etracker);
				return probe;
			}
		}
		catch( Exception exception ) {
			exception.printStackTrace();
			throw new RuntimeException( "Exception creating new probe.", exception );
		}
	}


	/** copy the entrance probe */
	private Probe copyEntranceProbe() {
		final Probe probe = ENTRANCE_PROBE.copy();
		probe.initialize();
		return probe;
	}


	/** calculate and return the trial orbit given the horizontal and vertical misalignments */
	public BeamOrbit getTrialOrbit( final double xMisalignment, final double yMisalignment ) {
		final Probe probe = copyEntranceProbe();
		SCENARIO.setProbe( probe );

		// collect all the quad elements corresponding to the misaligned quadrupole
		final String misalignedQuadID = MISALIGNED_QUADRUPOLE.getId();
		final List<Element> misalignedQuadElements = new ArrayList<>();
		final Lattice lattice = SCENARIO.getLattice();
		final Iterator<IComponent> componentIter = lattice.globalIterator();
		while( componentIter.hasNext() ) {
			final IComponent component = componentIter.next();
			// TODO: Find all elements corresponding to the misaligned quadrupole by equating the unique misalignedQuadID and element ID and assign the elements to the misalignedQuadElements
			if(component instanceof IdealMagQuad){
				final IdealMagQuad magQuad=(IdealMagQuad)component;
				final String elementId=magQuad.getId();
				if(elementId.equals(misalignedQuadID)){
					misalignedQuadElements.add(magQuad);
				}
			}
		}

		// apply the misaligments to the misaligned quadrupole elements
		// TODO: Set the x and y alignment for the misaligned quad elements
		for (Element element : misalignedQuadElements){
			element.setAlignX(xMisalignment);
			element.setAlignY(yMisalignment);
		}

		// run the model with the trial misalignments
		try {
			SCENARIO.resyncFromCache();
			SCENARIO.run();
			final Trajectory trajectory = probe.getTrajectory();

			final SimResultsAdaptor simulationAdaptor = new SimpleSimResultsAdaptor( trajectory );

			// calculate the trial orbit
			final BeamOrbit trialOrbit = new BeamOrbit( MEASURED_ORBIT.getSequence() );
			final List<BPM> bpms = MEASURED_ORBIT.getBeamPositionMonitors();
			for ( final BPM bpm : bpms ) {
				// TODO: Get the trajectory state for the BPM and use the simulationAdaptor to calculate the fixed orbit at the BPM and convert the coordinates to millimeters assigning them to variables named x and y.
				final ProbeState state = trajectory.stateForElement( bpm.getId() );
				final PhaseVector coordinates = simulationAdaptor.computeFixedOrbit( state );//由probe state对象得到该元件处的平均束流位置
				// get the beam position in millimeters
				final double x = 1000 * coordinates.getx();
				final double y = 1000 * coordinates.gety();
				trialOrbit.setBeamPosition( bpm, x, y );
			}

			return trialOrbit;
		}
		catch ( Exception exception ) {
			throw new RuntimeException( "Exception running the model.", exception );
		}
	}
}
