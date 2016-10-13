/*
 * MachineSimulatorDocument.java
 *
 * Created by Tom Pelaia on 9/19/11
 */

package xal.app.machinesimulator;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.text.DecimalFormat;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

//import xal.app.virtualaccelerator.DiagPlot;
import xal.ca.ConnectionException;
import xal.ca.GetException;
import xal.extension.application.*;
import xal.extension.application.smf.*;
import xal.smf.*;
import xal.tools.apputils.*;
import xal.tools.xml.XmlDataAdaptor;
import xal.smf.data.XMLDataManager;
import xal.tools.data.*;
import xal.extension.bricks.WindowReference;
import xal.extension.widgets.plot.BasicGraphData;
import xal.extension.widgets.plot.FunctionGraphsJPanel;
import xal.extension.widgets.swing.*;
import xal.model.probe.traj.ProbeState;


/**
 * MachineSimulatorDocument represents the document for the Machine Simulator application.
 * @author  t6p
 */
public class MachineSimulatorDocument extends AcceleratorDocument implements DataListener {
 	/** the data adaptor label used for reading and writing this document */
	static public final String DATA_LABEL = "MachineSimulatorDocument";
	
	/** main window reference */
	final WindowReference WINDOW_REFERENCE;
    
    /** main model */
    final MachineModel MODEL;
    
    /** simulated states table model */
    final KeyValueFilteredTableModel<MachineSimulationRecord> STATES_TABLE_MODEL;
    
	/** diagplot object*/
    public DiagPlot _diagplot;
    /** Empty Constructor */
    public MachineSimulatorDocument() {
        this( null );
    }
    
    
    /** 
     * Primary constructor 
     * @param url The URL of the file to load into the new document.
     */
    public MachineSimulatorDocument( final java.net.URL url ) {
        setSource( url );
		
		WINDOW_REFERENCE = getDefaultWindowReference( "MainWindow", this );
        
//        STATES_TABLE_MODEL = new KeyValueFilteredTableModel<IPhaseState>();
        STATES_TABLE_MODEL = new KeyValueFilteredTableModel<MachineSimulationRecord>();
        
        // initialize the model here
        MODEL = new MachineModel();
		
		if ( url != null ) {
            System.out.println( "Opening document: " + url.toString() );
            final DataAdaptor documentAdaptor = XmlDataAdaptor.adaptorForUrl( url, false );
            update( documentAdaptor.childAdaptor( dataLabel() ) );
        }		
		
		configureWindow( WINDOW_REFERENCE );//主界面显示		
		
		setHasChanges( false );
    }
    
    
    /** Make and configure the main window. */
    public void makeMainWindow() {
        mainWindow = (XalWindow)WINDOW_REFERENCE.getWindow();
        final FunctionGraphsJPanel twissplot = (FunctionGraphsJPanel) WINDOW_REFERENCE.getView("Twiss Plot");
		final FunctionGraphsJPanel sigamplot = (FunctionGraphsJPanel) WINDOW_REFERENCE.getView("Sigma Plot");			
		_diagplot = new DiagPlot(twissplot, sigamplot);
		setHasChanges( false );
    }
    
    
    /** configure the main window */
    private void configureWindow( final WindowReference windowReference ) {
        STATES_TABLE_MODEL.setColumnClassForKeyPaths( Double.class, "position", "probeState.kineticEnergy" );

        STATES_TABLE_MODEL.setColumnName( "elementID", "Element" );
        STATES_TABLE_MODEL.setColumnName( "probeState.kineticEnergy", "Kinetic Energy" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.0.beta", "<html>&beta;<sub>x</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.0.alpha", "<html>&alpha;<sub>x</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.0.gamma", "<html>&gamma;<sub>x</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.0.emittance", "<html>&epsilon;<sub>x</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.0.envelopeRadius", "<html>&sigma;<sub>x</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "betatronPhase.toArray.0", "<html>&phi;<sub>x</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.1.beta", "<html>&beta;<sub>y</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.1.alpha", "<html>&alpha;<sub>y</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.1.gamma", "<html>&gamma;<sub>y</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.1.emittance", "<html>&epsilon;<sub>y</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.1.envelopeRadius", "<html>&sigma;<sub>y</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "betatronPhase.toArray.1", "<html>&phi;<sub>y</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.2.beta", "<html>&beta;<sub>z</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.2.alpha", "<html>&alpha;<sub>z</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.2.gamma", "<html>&gamma;<sub>z</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.2.emittance", "<html>&epsilon;<sub>z</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "twissParameters.2.envelopeRadius", "<html>&sigma;<sub>z</sub></html>" );
        STATES_TABLE_MODEL.setColumnName( "betatronPhase.toArray.2", "<html>&phi;<sub>z</sub></html>" );
        
        final JTable statesTable = (JTable)windowReference.getView( "States Table" );
        statesTable.setModel( STATES_TABLE_MODEL );
        
        final JTextField statesTableFilterField = (JTextField)windowReference.getView( "States Table Filter Field" );
        STATES_TABLE_MODEL.setInputFilterComponent( statesTableFilterField );
        STATES_TABLE_MODEL.setMatchingKeyPaths( "elementID" );
        
        
        // handle the parameter selections
        final JCheckBox kineticEnergyCheckbox = (JCheckBox)windowReference.getView( "Kinetic Energy Checkbox" );
        
        final JCheckBox xSelectionCheckbox = (JCheckBox)windowReference.getView( "X Selection Checkbox" );
        final JCheckBox ySelectionCheckbox = (JCheckBox)windowReference.getView( "Y Selection Checkbox" );
        final JCheckBox zSelectionCheckbox = (JCheckBox)windowReference.getView( "Z Selection Checkbox" );
        
        final JCheckBox betaCheckbox = (JCheckBox)windowReference.getView( "Beta Checkbox" );
        final JCheckBox alphaCheckbox = (JCheckBox)windowReference.getView( "Alpha Checkbox" );
        final JCheckBox gammaCheckbox = (JCheckBox)windowReference.getView( "Gamma Checkbox" );
        final JCheckBox emittanceCheckbox = (JCheckBox)windowReference.getView( "Emittance Checkbox" );
        final JCheckBox beamSizeCheckbox = (JCheckBox)windowReference.getView( "Beam Size Checkbox" );
        final JCheckBox betatronPhaseCheckbox = (JCheckBox)windowReference.getView( "Betatron Phase Checkbox" );
                
        final ActionListener PARAMETER_HANDLER = new ActionListener() {
            public void actionPerformed( final ActionEvent event ) {                
                // array of standard parameters to display
                final String[] standardParameterKeys = new String[] { "elementID", "position" };
                
                // array of optional scalar parameters to display
                final List<String> scalarParameterNames = new ArrayList<String>();
                if ( kineticEnergyCheckbox.isSelected() )  scalarParameterNames.add( "probeState.kineticEnergy" );
                final String[] scalarParameterKeys = new String[ scalarParameterNames.size() ];
                int scalarParameterIndex = 0;
                for ( final String scalarParameterName : scalarParameterNames ) {
                    scalarParameterKeys[ scalarParameterIndex++ ] = scalarParameterName;
                }
                STATES_TABLE_MODEL.setColumnClassForKeyPaths( Double.class, scalarParameterKeys );
                
                // Add each selected plan to the list of planes to display and associate each plane with its corresponding twiss array index
                final List<String> planes = new ArrayList<String>(3);
                if ( xSelectionCheckbox.isSelected() )  planes.add( "0" );
                if ( ySelectionCheckbox.isSelected() )  planes.add( "1" );
                if ( zSelectionCheckbox.isSelected() )  planes.add( "2" );
                
                // Add each selected twiss parameter name to the list of parameters to display
                final List<String> twissParameterNames = new ArrayList<String>();
                if ( betaCheckbox.isSelected() )  twissParameterNames.add( "beta" );
                if ( alphaCheckbox.isSelected() )  twissParameterNames.add( "alpha" );
                if ( gammaCheckbox.isSelected() )  twissParameterNames.add( "gamma" );
                if ( emittanceCheckbox.isSelected() )  twissParameterNames.add( "emittance" );
                if ( beamSizeCheckbox.isSelected() )  twissParameterNames.add( "envelopeRadius" );
                
                int vectorParameterBaseCount = twissParameterNames.size();
                if ( betatronPhaseCheckbox.isSelected() )  vectorParameterBaseCount++;
                
                // construct the full vector parameter keys from each pair of selected planes and vector parameter names
                final String[] vectorParameterKeys = new String[ planes.size() * vectorParameterBaseCount ];
                int vectorParameterIndex = 0;
                for ( final String plane : planes ) {
                    for ( final String twissParameter : twissParameterNames ) {
//                        vectorParameterKeys[ vectorParameterIndex++ ] = "twiss." + plane + "." + twissParameter;
                        vectorParameterKeys[ vectorParameterIndex++ ] = "twissParameters." + plane + "." + twissParameter;
                    }
                    
                    if ( betatronPhaseCheckbox.isSelected() ) {
                        vectorParameterKeys[ vectorParameterIndex++ ] = "betatronPhase.toArray." + plane;
                    }
                }
                STATES_TABLE_MODEL.setColumnClassForKeyPaths( Double.class, vectorParameterKeys );
                
                final String[] parameterKeys = new String[standardParameterKeys.length + scalarParameterKeys.length + vectorParameterKeys.length];
                // add standard parameters at the start
                System.arraycopy( standardParameterKeys, 0, parameterKeys, 0, standardParameterKeys.length );
                // append optional scalar parameters after standard parameters
                System.arraycopy( scalarParameterKeys, 0, parameterKeys, standardParameterKeys.length, scalarParameterKeys.length );
                // append vector parameters after scalar parameters
                System.arraycopy( vectorParameterKeys, 0, parameterKeys, scalarParameterKeys.length + standardParameterKeys.length, vectorParameterKeys.length );
                
                STATES_TABLE_MODEL.setKeyPaths( parameterKeys );
            }
        };
        
        kineticEnergyCheckbox.addActionListener( PARAMETER_HANDLER );
        
        xSelectionCheckbox.addActionListener( PARAMETER_HANDLER );
        ySelectionCheckbox.addActionListener( PARAMETER_HANDLER );
        zSelectionCheckbox.addActionListener( PARAMETER_HANDLER );
        
        betaCheckbox.addActionListener( PARAMETER_HANDLER );
        alphaCheckbox.addActionListener( PARAMETER_HANDLER );
        gammaCheckbox.addActionListener( PARAMETER_HANDLER );
        emittanceCheckbox.addActionListener( PARAMETER_HANDLER );
        beamSizeCheckbox.addActionListener( PARAMETER_HANDLER );
        
        betatronPhaseCheckbox.addActionListener( PARAMETER_HANDLER );
        
        // perform the initial parameter display configuration
        PARAMETER_HANDLER.actionPerformed( null );
        
        
        // configure the run button
        final JButton runButton = (JButton)windowReference.getView( "Run Button" );
        runButton.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent event ) {
                System.out.println( "running the model..." );
                final MachineSimulation simulation = MODEL.runSimulation();
                /** update graph*/
                putDiagPVs(simulation);
                STATES_TABLE_MODEL.setRecords( simulation.getSimulationRecords() );
            }
        });

		
		final JCheckBox phaseSlipCheckbox = (JCheckBox)windowReference.getView( "Phase Slip Checkbox" );
		phaseSlipCheckbox.setSelected( MODEL.getSimulator().getUseRFGapPhaseSlipCalculation() );
		phaseSlipCheckbox.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				MODEL.getSimulator().setUseRFGapPhaseSlipCalculation( phaseSlipCheckbox.isSelected() );
			}
		});
    }
    
    
    // Generate the twiss parameter key from the base twiss parameter name and the plane
    static private String toTwissParameterKey( final String twissParameterName, final int plane ) {
        return "twiss." + plane + "." + twissParameterName;
    }
    
    
    /**
     * Save the document to the specified URL.
     * @param url The URL to which the document should be saved.
     */
    public void saveDocumentAs( final URL url ) {
        writeDataTo( this, url );
    }
    
    
    /** Handle the accelerator changed event by displaying the elements of the accelerator in the main window. */
    public void acceleratorChanged() {
        try {
            MODEL.setSequence( null );
            setHasChanges( true );
        }
        catch ( Exception exception ) {
            exception.printStackTrace();
            displayError( "Error Setting Accelerator", "Simulator Configuration Exception", exception );
        }
	}
    
    
    /** Handle the selected sequence changed event by displaying the elements of the selected sequence in the main window. */
    public void selectedSequenceChanged() {
        try {
            MODEL.setSequence( getSelectedSequence() );
            setHasChanges( true );
        }
        catch ( Exception exception ) {
            exception.printStackTrace();
            displayError( "Error Setting Sequence", "Simulator Configuration Exception", exception );
        }
	}
	
    
    /** provides the name used to identify the class in an external data source. */
    public String dataLabel() {
        return DATA_LABEL;
    }
    
    
    /** Instructs the receiver to update its data based on the given adaptor. */
    public void update( final DataAdaptor adaptor ) {
		if ( adaptor.hasAttribute( "acceleratorPath" ) ) {
			final String acceleratorPath = adaptor.stringValue( "acceleratorPath" );
			final Accelerator accelerator = applySelectedAcceleratorWithDefaultPath( acceleratorPath );
			
			if ( accelerator != null && adaptor.hasAttribute( "sequence" ) ) {
				final String sequenceID = adaptor.stringValue( "sequence" );
				setSelectedSequence( getAccelerator().findSequence( sequenceID ) );
			}
		}
		
        final DataAdaptor modelAdaptor = adaptor.childAdaptor( MachineModel.DATA_LABEL );
        if ( modelAdaptor != null )  MODEL.update( modelAdaptor );
    }
    
    
    /** Instructs the receiver to write its data to the adaptor for external storage. */
    public void write( final DataAdaptor adaptor ) {
        adaptor.setValue( "version", "1.0.0" );
        adaptor.setValue( "date", new java.util.Date().toString() );
		
        adaptor.writeNode( MODEL );
		
		if ( getAccelerator() != null ) {
			adaptor.setValue( "acceleratorPath", getAcceleratorFilePath() );
			
			final AcceleratorSeq sequence = getSelectedSequence();
			if ( sequence != null ) {
				adaptor.setValue( "sequence", sequence.getId() );
			}
		}
    }
    
    
    /** update the graph data after the run*/
    protected void putDiagPVs(MachineSimulation res) {
    	List<MachineSimulationRecord> records=res.getSimulationRecords();
		/**temporary list data for getting the array bpm and ws datas*/
		List<Double> tempalphax = new ArrayList<Double>();
		List<Double> tempalphay = new ArrayList<Double>();
		List<Double> tempbetax = new ArrayList<Double>();
		List<Double> tempbetay = new ArrayList<Double>();
		List<Double> tempsigmax = new ArrayList<Double>();
		List<Double> tempsigmay = new ArrayList<Double>();
		List<Double> tempbeampos = new ArrayList<Double>();	
		List<Double> tempsigmaz = new ArrayList<Double>();
		
		for(MachineSimulationRecord record:records){
			tempbeampos.add(record.getPosition());
			tempalphax.add(record.getTwissParameters()[0].getAlpha());
			tempalphay.add(record.getTwissParameters()[1].getAlpha());
			tempbetax.add(record.getTwissParameters()[0].getBeta());
			tempbetay.add(record.getTwissParameters()[1].getBeta());
			tempsigmax.add(record.getTwissParameters()[0].getEnvelopeRadius()*1000);
			tempsigmay.add(record.getTwissParameters()[1].getEnvelopeRadius()*1000);
			tempsigmaz.add(record.getTwissParameters()[2].getEnvelopeRadius()*1000);
		}
		int size=records.size();
//		double[] alphax=new double[tempalphax.size()];
//		double[] alphay=new double[tempalphay.size()];
//		double[] betax=new double[tempbetax.size()];
//		double[] betay=new double[tempbetay.size()];
//		double[] posi=new double[tempbeampos.size()];
//		double[] sigmax=new double[tempsigmax.size()];
//		double[] sigmay=new double[tempsigmay.size()];
//		double[] sigmaz=new double[tempsigmaz.size()];
		
		double[] alphax=new double[size];
		double[] alphay=new double[size];
		double[] betax=new double[size];
		double[] betay=new double[size];
		double[] posi=new double[size];
		double[] sigmax=new double[size];
		double[] sigmay=new double[size];
		double[] sigmaz=new double[size];
		int i=0;
		for (i = 0; i < size; i++) {
			posi[i] = tempbeampos.get(i);
			alphax[i] = tempalphax.get(i);
			alphay[i] = tempalphay.get(i);
			betax[i]=tempbetax.get(i);			
			betay[i]=tempbetay.get(i);
			sigmax[i]=tempsigmax.get(i);
			sigmay[i]=tempsigmay.get(i);
			sigmaz[i]=tempsigmaz.get(i);
		}
		try {
			_diagplot.showalphaplot(posi, alphax, alphay);
			_diagplot.showbetaplot(posi, betax, betay);
			_diagplot.showsigmaplot(posi, sigmax, sigmay, sigmaz);
		} catch (ConnectionException | GetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}


/**show bpm and ws plots*/
class DiagPlot {
	
	protected FunctionGraphsJPanel _twissplot;
	protected FunctionGraphsJPanel _sigamplot;
	protected BasicGraphData DataAlphax;
	protected BasicGraphData DataAlphay;
	protected BasicGraphData DataBetax;
	protected BasicGraphData DataBetay;
	protected BasicGraphData Datasigmax;
	protected BasicGraphData Datasigmay;
	protected BasicGraphData Datasigmaz;
	
	public DiagPlot(FunctionGraphsJPanel twissplot, FunctionGraphsJPanel sigamplot) {
		_twissplot=twissplot;
		_sigamplot=sigamplot;
		setupPlot(twissplot,sigamplot);	 
	}

	public void showalphaplot(double[] p,double[] x, double[] y) throws ConnectionException, GetException {	
		DataAlphax.updateValues(p, x);
		DataAlphay.updateValues(p, y);			
	}
	
	public void showbetaplot(double[] p,double[] x, double[] y) throws ConnectionException, GetException {		
		DataBetax.updateValues(p, x);
		DataBetay.updateValues(p, y);	    
	}
	
	
	public void showsigmaplot(double[] p,double[] sigmax,double[] sigmay,double[] sigmaz) throws ConnectionException, GetException {
		Datasigmax.updateValues(p, sigmax);	
		Datasigmay.updateValues(p, sigmay);	
		Datasigmaz.updateValues(p, sigmaz);	
	}
	
//	public void showsigmaplot(double[] wsp,double[] wsx, double[] wsy) throws ConnectionException, GetException {		
//		DataWSx.updateValues(wsp, wsx);
//		DataWSy.updateValues(wsp, wsy);
//	}
	
	
	public void setupPlot(FunctionGraphsJPanel twissplot,FunctionGraphsJPanel sigamplot) {
		/** setup twissplot*/
		// labels
		twissplot.setName( "Twiss_PLOT" );
		twissplot.setAxisNameX("Position(m)");
		twissplot.setAxisNameY("Twiss Params");

		twissplot.setNumberFormatX( new DecimalFormat( "###.###" ) );
		twissplot.setNumberFormatY( new DecimalFormat( "###.###" ) );

		// add legend support
		twissplot.setLegendPosition( FunctionGraphsJPanel.LEGEND_POSITION_ARBITRARY );
		twissplot.setLegendKeyString( "Legend" );
		twissplot.setLegendBackground( Color.lightGray );
		twissplot.setLegendColor( Color.black );
		twissplot.setLegendVisible( true );		
		
		/** setup sigamplot*/
		// labels
		sigamplot.setName( "Sigma_PLOT" );
		sigamplot.setAxisNameX("Position(m)");
		sigamplot.setAxisNameY("Beam Envelope(mm)");

		sigamplot.setNumberFormatX( new DecimalFormat( "###.###" ) );
		sigamplot.setNumberFormatY( new DecimalFormat( "###.###" ) );

		// add legend support
		sigamplot.setLegendPosition( FunctionGraphsJPanel.LEGEND_POSITION_ARBITRARY );
		sigamplot.setLegendKeyString( "Legend" );
		sigamplot.setLegendBackground( Color.lightGray );
		sigamplot.setLegendColor( Color.black );
		sigamplot.setLegendVisible( true );
		
		
		DataAlphax=new BasicGraphData();
		DataAlphay=new BasicGraphData();	
		DataBetax=new BasicGraphData();	 
		DataBetay=new BasicGraphData();	
		Datasigmax=new BasicGraphData();
		Datasigmay=new BasicGraphData();
		Datasigmaz=new BasicGraphData();
		
		DataAlphax.setGraphProperty(_twissplot.getLegendKeyString(), "Alphax");
		DataAlphay.setGraphProperty(_twissplot.getLegendKeyString(), "Alphay");		    
		DataBetax.setGraphProperty(_twissplot.getLegendKeyString(), "Betax");
		DataBetay.setGraphProperty(_twissplot.getLegendKeyString(), "Betay");	    
		Datasigmax.setGraphProperty(_sigamplot.getLegendKeyString(), "sigmax");		
		Datasigmay.setGraphProperty(_sigamplot.getLegendKeyString(), "sigmay");
		Datasigmaz.setGraphProperty(_sigamplot.getLegendKeyString(), "sigmaz");
		
		DataAlphax.setGraphColor(Color.blue);
		DataAlphay.setGraphColor(Color.orange);    
		DataBetax.setGraphColor(Color.RED);
		DataBetay.setGraphColor(Color.BLACK);		    
		Datasigmax.setGraphColor(Color.RED);
		Datasigmay.setGraphColor(Color.BLACK);
	    Datasigmaz.setGraphColor(Color.blue);
	    
	   _twissplot.addGraphData(DataAlphax);
	   _twissplot.addGraphData(DataAlphay);		
	   _twissplot.addGraphData(DataBetax);
	   _twissplot.addGraphData(DataBetay);				
	   _sigamplot.addGraphData(Datasigmax);
	   _sigamplot.addGraphData(Datasigmay);			
	   _sigamplot.addGraphData(Datasigmaz);
	}
	
}