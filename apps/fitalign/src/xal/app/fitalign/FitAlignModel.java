/*
 * FitAlignModel.java
 *
 * Created on January 23, 2014, 1:32 PM
 */

package xal.app.fitalign;

import java.util.*;

import xal.ca.*;
import xal.smf.*;
import xal.smf.impl.*;
import xal.model.probe.*;
import xal.model.alg.*;
import xal.model.elem.*;
import xal.sim.scenario.*;
import xal.tools.beam.calc.*;


/**
 * FitAlignModel is the main model for the alignment fitting application.
 * @author  t6p
 */
public class FitAlignModel {
	/** accelerator sequence */
	private AcceleratorSeq _sequence;

	/** measured beam orbit */
	private BeamOrbit _beamOrbit;

	/** misaligned quadrupole */
	private Quadrupole _misalignedQuad;


    /** Create a new model */
    public FitAlignModel() {
		_sequence = null;
		_beamOrbit = null;
		_misalignedQuad = null;
    }


	/** get the accelerator sequence */
	public AcceleratorSeq getSequence() {
		return _sequence;
	}


	/** set the accelerator sequence */
	public void setSequence( final AcceleratorSeq sequence ) {
		_sequence = sequence;
		_misalignedQuad = null;

		if ( _sequence != null ) {
			measureBeamOrbit();
		}
		else {
			_beamOrbit = null;
		}
	}


	/** get the available quadrupoles */
	public List<Quadrupole> getAvailableQuadrupoles() {
		// grab all the sequence's quadrupoles that have good status
		final AcceleratorSeq sequence = _sequence;
		return sequence != null ? sequence.<Quadrupole>getNodesOfType( Quadrupole.s_strType, true ) : null;
	}


	/** get the misaligned quad */
	public Quadrupole getMisalignedQuad() {
		return _misalignedQuad;
	}


	/** set the misaligned quad */
	public void setMisalignedQuad( final Quadrupole misalignedQuad ) {
		_misalignedQuad = misalignedQuad;
	}


	/** get the last measured beam orbit */
	public BeamOrbit getBeamOrbit() {
		return _beamOrbit;
	}


	/** measure the orbit */
	public BeamOrbit measureBeamOrbit() {
		_beamOrbit = BeamOrbit.captureOrbit( _sequence );
		return _beamOrbit;
	}


	/** calculate and return the trial orbit given the horizontal and vertical misalignments */
	public BeamOrbit getTrialOrbit( final double xMisalignment, final double yMisalignment ) {
		final AlignmentFitEvaluator evaluator = new AlignmentFitEvaluator( _misalignedQuad, _beamOrbit );
		return evaluator.getTrialOrbit( xMisalignment, yMisalignment );
	}
}