/*
 * FitAlignDocument.java
 *
 * Created on January 23, 2014, 1:32 PM
 */

package xal.app.fitalign;

import java.net.*;
import java.io.*;
import java.awt.Color;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.*;
import java.text.*;

import xal.tools.dispatch.*;
import xal.smf.*;
import xal.smf.impl.*;
import xal.extension.application.*;
import xal.extension.application.smf.*;
import xal.extension.bricks.WindowReference;
import xal.extension.widgets.plot.*;
import xal.extension.widgets.swing.*;

/**
 * FitAlignDocument is a custom document for this application.
 * @author  t6p
 */
public class FitAlignDocument extends AcceleratorDocument {
	/** format for output */
	final static private DecimalFormat OUTPUT_FORMAT;

	/** main model */
	final private FitAlignModel MODEL;

	/** plot of the beam orbit */
	private FunctionGraphsJPanel _orbitPlot;

	/** reference to the what-if dialog */
	private WindowReference _whatifDialogReference;


	// static initializer
	static {
		OUTPUT_FORMAT = new DecimalFormat( "0.000" );
	}


    /** Create a new empty document */
    public FitAlignDocument() {
        this( null );
    }
    
    
    /** 
     * Create a new document loaded from the URL file 
     * @param url The URL of the file to load into the new document.
     */
    public FitAlignDocument( final URL url ) {
		MODEL = new FitAlignModel();

        setSource( url );
    }


    /**
     * Make the main window. This method is called by the superclass to generate the content for this document's main window.
     */
    public void makeMainWindow() {
		final WindowReference windowReference = getDefaultWindowReference( "MainWindow", this );
        mainWindow = (AcceleratorWindow)windowReference.getWindow();

		// store a reference to the whatif dialog so it can be reused
		_whatifDialogReference = getDefaultWindowReference( "WhatIfDialog", mainWindow );

		final JButton whatIfButton = (JButton)windowReference.getView( "WhatIfButton" );
		whatIfButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				if ( getSelectedSequence() != null ) {
					showWhatifDialog();
				}
				else {
					displayError( "No Sequence Selected", "You must first select a sequence before performing a what-if evaluation." );
				}
			}
		});

		final JButton measureOrbitButton = (JButton)windowReference.getView( "MeasureOrbitButton" );
		measureOrbitButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				if ( getSelectedSequence() != null ) {
					MODEL.measureBeamOrbit();
					clearOrbitPlot();
					plotMeasuredOrbit();
				}
				else {
					displayError( "No Sequence Selected", "You must first select a sequence before measuring the orbit." );
				}
			}
		});

		final JButton misalignedQuadSelectionButton = (JButton)windowReference.getView( "MisalignedQuadSelectionButton" );
		misalignedQuadSelectionButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				final List<Quadrupole> quadrupoles = MODEL.getAvailableQuadrupoles();

				if ( quadrupoles != null ) {
					//final KeyValueRecordSelector<Quadrupole> quadrupoleSelector = KeyValueRecordSelector.getInstance( quadrupoles, mainWindow, "SelectMisalignedQuadrupole", "id" );
					//final Quadrupole misalignedQuad = quadrupoleSelector.showSingleSelectionDialog();
					for (Quadrupole quad : quadrupoles){
						System.out.println(quad.getId());
					}
					final Quadrupole misalignedQuad = quadrupoles.get(1);
					if ( misalignedQuad != null ) {
						misalignedQuadSelectionButton.setText( misalignedQuad.getId() );
					}
					else {
						misalignedQuadSelectionButton.setText( "Select Quad..." );
					}
					MODEL.setMisalignedQuad( misalignedQuad );
				}
				else {
					displayError( "No quads to select", "There are no quadrupoles from which to select. Be sure to select a sequence first." );
				}
			}
		});

		_orbitPlot = (FunctionGraphsJPanel)windowReference.getView( "OrbitPlot" );
		_orbitPlot.setAxisNameX( "Position from sequence start (m)" );
		_orbitPlot.setAxisNameY( "Beam displacement (mm)" );
		_orbitPlot.setNumberFormatX( new DecimalFormat( "0.00E0" ) );
		_orbitPlot.setNumberFormatY( new DecimalFormat( "0.00E0" ) );

		plotMeasuredOrbit();
	}


	/** Display the whatif dialog */
	private void showWhatifDialog() {
		final JDialog whatIfDialog = (JDialog)_whatifDialogReference.getWindow();

		final JButton cancelButton = (JButton)_whatifDialogReference.getView( "CancelButton" );
		cancelButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				whatIfDialog.setVisible( false );
			}
		});

		final JButton okayButton = (JButton)_whatifDialogReference.getView( "OkayButton" );
		okayButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				whatIfDialog.setVisible( false );
				
				final JTextField xMisalignmentField = (JTextField)_whatifDialogReference.getView( "XMisalignmentField" );
				final JTextField yMisalignmentField = (JTextField)_whatifDialogReference.getView( "YMisalignmentField" );

				try {
					// get the misalignment coordinates in meters (converting from millimeters)
					final double xMisalignment = Double.parseDouble( xMisalignmentField.getText() ) / 1000.0;
					final double yMisalignment = Double.parseDouble( yMisalignmentField.getText() ) / 1000.0;

					final BeamOrbit trialOrbit = MODEL.getTrialOrbit( xMisalignment, yMisalignment );//浣跨敤灏濊瘯鐨勫姙娉曡缃洓绾ч搧璇樊浣跨敤鍦ㄧ嚎妯″瀷娴嬮噺寰楀埌鐨勮建閬撹宸�
					MODEL.measureBeamOrbit();
					final BeamOrbit measuredOrbit = MODEL.getBeamOrbit();//杩欎釜beam orbit鏄疊PM娴嬮噺寰楀埌鐨勮建閬撴暟鎹�

					// update the plot
					plotMeasuredAndTrialOrbits( measuredOrbit, trialOrbit );
				}
				catch( Exception exception ) {
					exception.printStackTrace();
					displayError( "What-If failed", "Exception: " + exception.getMessage() );
				}
			}
		});

		// show the what-if dialog
		if ( getSelectedSequence() == null ) {
			displayError( "What-If failed", "No sequence selected. Please select a sequence and a misaligned quad before performing a what-if." );
		}
		else if ( MODEL.getMisalignedQuad() == null	) {
			displayError( "What-If failed", "No misaligned quad selected. Please select a sequence and a misaligned quad before performing a what-if." );
		}
		else {
			whatIfDialog.setLocationRelativeTo( mainWindow );
			whatIfDialog.setVisible( true );
		}
	}


    /**
     * Hook for handling the accelerator change event.  Subclasses should override
     * this method to provide custom handling.  The default handler does nothing.
     */
    public void acceleratorChanged() {
		if ( MODEL != null ) {
			MODEL.setSequence( null );
			clearOrbitPlot();
		}
    }


    /**
     * Hook for handling the selected sequence change event.  Subclasses should override
     * this method to provide custom handling.  The default handler does nothing.
     */
    public void selectedSequenceChanged() {
		if ( MODEL != null ) {
			clearOrbitPlot();
			MODEL.setSequence( getSelectedSequence() );
			plotMeasuredOrbit();
		}
    }

    
    /**
     * Save the document to the specified URL.
     * @param url The URL to which the document should be saved.
     */
    public void saveDocumentAs( final URL url ) {
    }


	/** plot the current measured orbit */
	private void plotMeasuredOrbit() {
		final BeamOrbit measuredOrbit = MODEL.getBeamOrbit();
		plotMeasuredOrbit( measuredOrbit );
	}


	/** plot the specified measured orbit */
	private void plotMeasuredOrbit( final BeamOrbit orbit ) {
		plotOrbit( orbit, "Measured", Color.BLUE );
	}


	/** plot the specified trial orbit */
	private void plotTrialOrbit( final BeamOrbit orbit ) {
		plotOrbit( orbit, "Trial", Color.GREEN );
	}


	/** plot the difference between the two orbits */
	private void plotDifferenceOrbit( final BeamOrbit trialOrbit, final BeamOrbit referenceOrbit ) {
		if ( trialOrbit != null && referenceOrbit != null ) {
			// TODO: Plot the difference orbit (trialOrbit - referenceOrbit)
			BeamOrbit diffOrbit=trialOrbit.minus(referenceOrbit);
			plotOrbit(diffOrbit, "difference", Color.RED);
		}
	}


	/** plot the trial and measured orbits */
	private void plotMeasuredAndTrialOrbits( final BeamOrbit measuredOrbit, final BeamOrbit trialOrbit ) {
		// update the plot
		clearOrbitPlot();
		plotMeasuredOrbit( measuredOrbit );
		plotTrialOrbit( trialOrbit );
		plotDifferenceOrbit( trialOrbit, measuredOrbit );
	}


	/** plot the specified orbit */
	private void plotOrbit( final BeamOrbit orbit, final String baseLabel, final Color baseColor ) {
		if ( _orbitPlot != null ) {
			if ( orbit != null ) {
				final List<BPM> bpms = orbit.getBeamPositionMonitors();
				final BasicGraphData xOrbitSeries = new BasicGraphData();
				xOrbitSeries.setGraphProperty( _orbitPlot.getLegendKeyString(), baseLabel + " X Orbit" );
				xOrbitSeries.setGraphColor( baseColor.darker() );
				xOrbitSeries.setLineDashPattern( 5, 5 );
				final BasicGraphData yOrbitSeries = new BasicGraphData();
				yOrbitSeries.setGraphProperty( _orbitPlot.getLegendKeyString(), baseLabel + " Y Orbit" );
				yOrbitSeries.setGraphColor( baseColor.brighter() );
				yOrbitSeries.setLineDashPattern( 2, 2 );
				for ( final BPM bpm : bpms ) {
					final double x = orbit.getBeamPositionX( bpm );
					final double y = orbit.getBeamPositionY( bpm );
					final double z = orbit.getBeamPositionZ( bpm );
//					if ( !Double.isNaN( x ) && !Double.isNaN( y ) && !Double.isNaN( z ) ) {
//						xOrbitSeries.addPoint( z, x );
//						yOrbitSeries.addPoint( z, y );
//					}
					xOrbitSeries.addPoint( z, x );
					yOrbitSeries.addPoint( z, y );
				}
				_orbitPlot.addGraphData( xOrbitSeries );
				_orbitPlot.addGraphData( yOrbitSeries );
			}
		}
	}


	/** clear the orbit plot */
	private void clearOrbitPlot() {
		if ( _orbitPlot != null ) {
			_orbitPlot.removeAllGraphData();
		}
	}
}