/*
 * FitAlignModel.java
 *
 * Created on January 23, 2014, 1:32 PM
 */

package xal.app.fitalign;

import java.util.*;

import javax.xml.soap.Node;

import xal.ca.*;
import xal.smf.*;
import xal.smf.impl.*;


/**
 * Beam Orbit at beam position monitors.
 * @author  t6p
 */
public class BeamOrbit {
	/** map of beam positions keyed by BPM */
	final Map<String,PlanePoint> BEAM_POSITIONS;

	/** sequence for which the orbit is measured */
	final AcceleratorSeq SEQUENCE;

	/** beam position monitors */
	final List<BPM> BEAM_POSITION_MONITORS;


    /** Create a new model */
    public BeamOrbit( final AcceleratorSeq sequence ) {
		SEQUENCE = sequence;
		BEAM_POSITIONS = new HashMap<>();

		// grab all the sequence's beam position monitors that have good status
		// TODO: Fetch the beam position monitors with good status from the sequence and assign it to BEAM_POSITION_MONITORS.
		//BEAM_POSITION_MONITORS=sequence.getNodesOfType("BPM",true);//BPM.s_strType="BPM"
		BEAM_POSITION_MONITORS=sequence.getNodesOfType(BPM.s_strType,true);
    }


	/** capture and return the beam orbit */
	static public BeamOrbit captureOrbit( final AcceleratorSeq sequence ) {
		final BeamOrbit orbit = new BeamOrbit( sequence );
		orbit.measure();
		return orbit;
	}


	/** calculate the difference between this orbit and the specified orbit */
	public BeamOrbit minus( final BeamOrbit orbit ) {
		final List<BPM> bpms = getBeamPositionMonitors();
		final BeamOrbit orbitDiff = new BeamOrbit( SEQUENCE );

		for ( final BPM bpm : bpms ) {
			final double x1 = getBeamPositionX( bpm );
			final double x2 = orbit.getBeamPositionX( bpm );
			final double dx = x1 - x2;

			final double y1 = getBeamPositionY( bpm );
			final double y2 = orbit.getBeamPositionY( bpm );
			final double dy = y1 - y2;

			if ( !Double.isNaN( dx ) && !Double.isNaN( dy ) ) {
				orbitDiff.setBeamPosition( bpm, dx, dy );
			}
		}

		return orbitDiff;
	}


	/** get the sequence */
	public AcceleratorSeq getSequence() {
		return SEQUENCE;
	}


	/** set the beam position for the specified BPM  */
	public void setBeamPosition( final BPM bpm, final double x, final double y ) {
		final PlanePoint point = new PlanePoint( x, y );
		synchronized( BEAM_POSITIONS ) {
			BEAM_POSITIONS.put( bpm.getId(), point );
		}
	}


	/** get the beam position monitors */
	public List<BPM> getBeamPositionMonitors() {
		return new ArrayList<BPM>( BEAM_POSITION_MONITORS );
	}


	/** get the position along the beamline of the beam position monitor relative to the sequence's entrance */
	public double getBeamPositionZ( final BPM bpm ) {
		return SEQUENCE.getPosition( bpm );
	}


	/** get the horizontal beam positon at the specified BPM */
	public double getBeamPositionX( final BPM bpm ) {
		return getBeamPositionX( bpm.getId() );
	}


	/** get the horizontal beam positon at the specified BPM */
	public double getBeamPositionX( final String bpmID ) {
		final PlanePoint point = getBeamPosition( bpmID );
		return point != null ? point.getX() : Double.NaN;
	}


	/** get the vertical beam positon at the specified BPM */
	public double getBeamPositionY( final BPM bpm ) {
		return getBeamPositionY( bpm.getId() );
	}


	/** get the vertical beam positon at the specified BPM */
	public double getBeamPositionY( final String bpmID ) {
		final PlanePoint point = getBeamPosition( bpmID );
		return point != null ? point.getY() : Double.NaN;
	}


	/** get the beam position for the specified BPM */
	private PlanePoint getBeamPosition( final String bpmID ) {
		synchronized( BEAM_POSITIONS ) {
			return BEAM_POSITIONS.get( bpmID );
		}
	}


	/** begin measuring the beam orbit */
	public void measure() {
		// clear the current beam positions
		synchronized( BEAM_POSITIONS ) {
			BEAM_POSITIONS.clear();
		}

		// collect the X and Y average position channels from the beam position monitors
		final Set<Channel> channels = new HashSet<>( 2 * BEAM_POSITION_MONITORS.size() );
		for ( final BPM bpm : BEAM_POSITION_MONITORS ) {
			final Channel xAvgChannel = bpm.findChannel(BPM.X_AVG_HANDLE);	// TODO: Get the channel from the BPM which corresponds to the X_AVG signal and assign it to xAvgChannel
			if ( xAvgChannel != null ) {
				channels.add( xAvgChannel );
			}

			final Channel yAvgChannel = bpm.findChannel(BPM.Y_AVG_HANDLE);	// TODO: Get the channel from the BPM which corresponds to the X_AVG signal and assign it to yAvgChannel
			if ( yAvgChannel != null ) {
				channels.add( yAvgChannel );
			}
		}


		// submit a batch request to capture all the channel data in parallel
		final BatchGetValueRequest batchRequest = new BatchGetValueRequest( channels );
		System.out.println( "Submit batch request and wait..." );
		batchRequest.submitAndWait( 5.0 );	// wait at most 5 seconds

		// gather all the measurements
		final Map<String,PlanePoint> measurements = new HashMap<>();
		for ( final BPM bpm : BEAM_POSITION_MONITORS ) {
			double x = Double.NaN;
			final Channel xAvgChannel = bpm.findChannel( BPM.X_AVG_HANDLE );
			if ( xAvgChannel != null ) {
				final ChannelRecord xAvgRecord = batchRequest.getRecord( xAvgChannel );
				if ( xAvgRecord != null ) {
					x = xAvgRecord.doubleValue();
				}
			}

			// TODO: Get the measured vertical position from the batch request and assign it to a new variable named "y"
			double y = Double.NaN;
			final Channel yAvgChannel = bpm.findChannel( BPM.Y_AVG_HANDLE );
			if ( yAvgChannel != null ) {
				final ChannelRecord yAvgRecord = batchRequest.getRecord( yAvgChannel );
				if ( yAvgRecord != null ) {
					y = yAvgRecord.doubleValue();
				}
			}
			
			final PlanePoint measurement = new PlanePoint( x, y );
			measurements.put( bpm.getId(), measurement );
		}

		synchronized( BEAM_POSITIONS ) {
			BEAM_POSITIONS.putAll( measurements );
			System.out.println( "Captured Orbit: " + BEAM_POSITIONS );
		}
	}
}



/** represents a point on a 2D plane */
class PlanePoint {
	/** horizontal coordinate */
	final private double X;

	/** vertical coordinate */
	final private double Y;


	/** Constructor */
	public PlanePoint( final double x, final double y ) {
		X = x;
		Y = y;
	}


	/** get the horizontal coordinate */
	public double getX() {
		return X;
	}


	/** get the vertical coordinate */
	public double getY() {
		return Y;
	}


	/** string representation of the point */
	public String toString() {
		return "(" + X + ", " + Y+ ")";
	}
}
