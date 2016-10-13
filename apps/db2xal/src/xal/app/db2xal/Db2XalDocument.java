/*
 * @(#)Db2XalDocument.java          2.5 02/13/2004
 *
 * Copyright (c) 2001-2004 Oak Ridge National Laboratory
 * Oak Ridge, Tenessee 37831, U.S.A.
 * All rights reserved.
 *
 */
package xal.app.db2xal;

//import java.sql.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.net.URL;
import java.util.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.omg.CORBA.PRIVATE_MEMBER;

import javax.swing.JToggleButton.ToggleButtonModel;

import xal.extension.application.smf.*;
import xal.tools.xml.*;
import xal.tools.data.*;
import xal.extension.application.*;
import xal.tools.apputils.NonConsecutiveSeqSelector.MyTableModel;
import xal.tools.database.ConnectionDictionary;
//import gov.sns.tools.database.*;


/**
 * Application for generating XAL XML file from SNS global database
 * 
 * @version 2.5 13 Feb 2004
 * @author Paul C. Chu
 * @author K.Danilova
 */

public class Db2XalDocument extends AcceleratorDocument implements DataListener {
	/** name of the production server in the configruation file */
	final private static String PRODUCTION_SERVER = "Product";//production
	
	/** name of the development server in the configruation file */
	final private static String DEVELOPMENT_SERVER = "XiPAF_Dev";//development
	
	private static final String OUTPUT_FILENAME = "XiPAF.xdxf";
	
	protected JFileChooser _exportFileChooser;

	/**
	 * The document for the text pane in the main window.
	 */
	protected PlainDocument textDocument;

	protected String databaseServer = DEVELOPMENT_SERVER;

	protected ArrayList<Object> seqList;
	
	private String wholeText="";//whole text 用于存储生成的整个光学文件

	// private Db2XalDocument myDoc;

	/** Create a new empty document */
	public Db2XalDocument() {
		this(null);
		// myDoc = this;
	}

	/**
	 * Create a new document loaded from the URL file
	 * 
	 * @param url
	 *            The URL of the file to load into the new document.
	 */
	public Db2XalDocument(java.net.URL url) {
		setSource(url);
		makeTextDocument();

		if (url == null)
			return;
	}

	public void customizeCommands(Commander commander) {

		// action for popping up sequence chooser
		Action showSeqsAction = new AbstractAction() {
			static final long serialVersionUID = 0;

			public void actionPerformed(ActionEvent event) {
				// NonConsecutiveSequenceSelector seqSel = new
				// SequenceSelector(myDoc);
//				NonConsecutiveSeqSelector seqSel = new NonConsecutiveSeqSelector();
//				seqSel.selectSequence();
				MyTableModel myModel = ((Db2XalWindow)getMainWindow()).getSeqSelector().geTableModel();
				seqList = new java.util.ArrayList<Object>();
				for (int i = 0; i < myModel.data.length; i++) {
					if (((Boolean) myModel.data[i][1]).booleanValue())
						seqList.add(myModel.data[i][0]);
				}
				if (seqList.size() != 0){
					setHasChanges(true);
					JOptionPane.showMessageDialog( getMainWindow(), "已选择的加速器序列为："+seqList.toString(), "Message!", JOptionPane.PLAIN_MESSAGE );
				}else {
					JOptionPane.showMessageDialog( getMainWindow(), "还没有选择加速器序列!  请先选择一个或多个加速器序列", "Warning!", JOptionPane.WARNING_MESSAGE);
				}
			}
		};
		showSeqsAction.putValue(Action.NAME, "showSeqs");
		commander.registerAction(showSeqsAction);
		
		/*
		 * 20160817增加工具栏按钮，用于保存xdxf文档
		 */
		// action for save whole text in text area as a xdxf file
				Action SaveAction = new AbstractAction() {
					static final long serialVersionUID = 0;

					public void actionPerformed(ActionEvent event) {
						JTextArea textView=((Db2XalWindow)getMainWindow()).getTextView();
						if ( _exportFileChooser == null ) {
							_exportFileChooser = new JFileChooser("E:/xal/config");
//							new JFileChooser("E:/xal/config");
							_exportFileChooser.setSelectedFile( new File( OUTPUT_FILENAME ) );
						}
						
						int status = _exportFileChooser.showSaveDialog( getMainWindow() );
						switch( status ) {
							case JFileChooser.APPROVE_OPTION:
								break;
							case JFileChooser.CANCEL_OPTION:
								return;
							case JFileChooser.ERROR_OPTION:
								return;
							default:
								return;
						}
						//判断选择的文件是否存储，并选择是否覆盖，20160817添加
//						System.out.println(_exportFileChooser.getSelectedFile().getName());
						File selFile=_exportFileChooser.getSelectedFile();
						while (selFile.exists()) {
							int overwrite=JOptionPane.showConfirmDialog(getMainWindow(), "文件"+selFile.getName()+"已经存在，是否覆盖？","是否覆盖",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
							if (overwrite!=JOptionPane.YES_OPTION) {
//								return;
								status = _exportFileChooser.showSaveDialog( getMainWindow() );
								switch( status ) {
									case JFileChooser.APPROVE_OPTION:
										break;
									case JFileChooser.CANCEL_OPTION:
										return;
									case JFileChooser.ERROR_OPTION:
										return;
									default:
										return;
								}
							}else {
								break;
							}
							selFile=_exportFileChooser.getSelectedFile();
						}
						if (!textView.getText().equals("")) {
							OutputStream fout;
							byte buf0[] = textView.getText().getBytes();
							try {
								fout = new FileOutputStream( _exportFileChooser.getSelectedFile() );
								fout.write(buf0);
								fout.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							JOptionPane.showMessageDialog( getMainWindow(), "文件已更新，输出文件名为： "+_exportFileChooser.getSelectedFile().getName()+".", "Message!", JOptionPane.PLAIN_MESSAGE );
						}else {
							JOptionPane.showMessageDialog( getMainWindow(), "文本框没有内容，请先生成XDXF文件！", "Message!", JOptionPane.PLAIN_MESSAGE );
						}
					}
				};
				SaveAction.putValue(Action.NAME, "save-xal");
				commander.registerAction(SaveAction);

		ToggleButtonModel useProdModel;
		ToggleButtonModel useDevlModel;

		// action for query production database
		useProdModel = new ToggleButtonModel();
		useProdModel.setSelected(false);
		useProdModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				databaseServer = PRODUCTION_SERVER;
			}
		});
		commander.registerModel("use-prod", useProdModel);

		// action for query development database
		useDevlModel = new ToggleButtonModel();
//		useDevlModel.setSelected(true);
		useDevlModel.setSelected(true);
		useDevlModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				databaseServer = DEVELOPMENT_SERVER;
			}
		});
		commander.registerModel("use-devl", useDevlModel);

		// action for generating XAL XML file
		Action export1Action = new AbstractAction() {
			static final long serialVersionUID = 0;

			public void actionPerformed(ActionEvent event) {
				if (seqList == null) {
					JOptionPane.showMessageDialog( getMainWindow(), "还没有选择加速器序列!  请先选择一个或多个加速器序列", "Warning!", JOptionPane.WARNING_MESSAGE);
				} 
				else {
					try {
						writeXal();
					} 
					catch (IOException exception) {
						exception.printStackTrace();
					}

					/*
					 * 20160816增加textarea显示所有的光学文件内容
					 */
					JTextArea textView=((Db2XalWindow)getMainWindow()).getTextView();
					textView.setFont(new Font("Serif",Font.PLAIN,17));
					textView.setForeground(Color.red);
					textView.setText(wholeText);
					if (!wholeText.equals("")) {
						JOptionPane.showMessageDialog( getMainWindow(), "文件已更新，输出文件名为： "+_exportFileChooser.getSelectedFile().getName()+".", "Message!", JOptionPane.PLAIN_MESSAGE );
						//在textview更新显示之后，清空wholeText对象
						wholeText="";
					}else {
						JOptionPane.showMessageDialog( getMainWindow(), "生成的XDXF字符串为空，没有写入XDXF文件，请检查程序配置之后再尝试 ", "Warning!", JOptionPane.WARNING_MESSAGE );
					}
//					JOptionPane.showMessageDialog( getMainWindow(), "文件已更新，输出文件名为： "+_exportFileChooser.getSelectedFile().getName()+".", "Message!", JOptionPane.PLAIN_MESSAGE );
				}
			}

		};
		export1Action.putValue(Action.NAME, "export-xal");
		commander.registerAction(export1Action);

	}
	
	
	/** generate a channel entry for a given signal */
	@SuppressWarnings("unused")
	static private String channelEntry( final String handle, final String signalName, final Map<String,String> signalTable, final boolean settable ) {
		return "          <channel handle=\"" + handle + "\"" + " signal=\"" + signalTable.get( signalName ) + "\"" + " settable=\"" + settable + "\"/>\n";
	}
	
	/** generate a channel entry for a given signal */
	static private String channelEntry( final Map<String,List<String>> signalTable) {
		final int channel_num=signalTable.get("signal").size();
		String str="";
		for (int i = 0; i < channel_num; i++) {
			str=str+"          <channel handle=\"" + signalTable.get("handle").get(i) + "\"" + " signal=\"" + signalTable.get( "signal" ).get(i) + "\"" + " settable=\"" + signalTable.get("settable").get(i) + "\"/>\n";
		}
		return str;
	}
	
	
	/** fetch the signals for the specified device */
	@SuppressWarnings("unused")
	static private Map<String,String> fetchSignals( final PreparedStatement fetchStatement, final String deviceID ) throws java.sql.SQLException {
		fetchStatement.setString( 1, deviceID );
		final ResultSet signalSet = fetchStatement.executeQuery();
		final Map<String,String> signalTable = new HashMap<String,String>();
		while ( signalSet.next() ) {
			final String signalID = signalSet.getString( 1 );
			final String signalName = signalSet.getString( 2 );
			signalTable.put( signalName, signalID );
			//System.out.println( "name: " + signalName + ", signal: " + signalID );
		}
		return signalTable;
	}
	
	/** fetch the signals for the specified device */
	/*
	 * 结合XiPAF数据库的特点从channel表中获取所需要的通道信息
	 */
	static private Map<String, List<String>> fetchChannels( final PreparedStatement fetchStatement, final String deviceID ) throws java.sql.SQLException {
		fetchStatement.setString( 1, deviceID );
		final ResultSet signalSet = fetchStatement.executeQuery();
		final Map<String, List<String>> signalTable = new HashMap<String, List<String>>();
		final List<String> signalList = new ArrayList<String>();
		final List<String> hanleList = new ArrayList<String>();
		final List<String> settableList = new ArrayList<String>();
		while ( signalSet.next() ) {
			signalList.add(deviceID+":"+signalSet.getString( "SIGNAL" ));//channel中只包含signal id的后半部分，前面还需要加上dvc_id
			hanleList.add(signalSet.getString("HANDLE"));
			settableList.add(signalSet.getString("SETTABLE"));
			//System.out.println( "name: " + signalName + ", signal: " + signalID );
		}
		signalTable.put( "signal", signalList );
		signalTable.put("handle", hanleList);
		signalTable.put("settable", settableList);
		return signalTable;
	}
	
	
	/** generate and write the optics to a file */
	@SuppressWarnings("unused")
	public void writeXal() throws IOException {
		if ( _exportFileChooser == null ) {
			_exportFileChooser = new JFileChooser("E:/xal/config");
			_exportFileChooser.setSelectedFile( new File( OUTPUT_FILENAME ) );
		}
		
		int status = _exportFileChooser.showSaveDialog( getMainWindow() );
		switch( status ) {
			case JFileChooser.APPROVE_OPTION:
				break;
			case JFileChooser.CANCEL_OPTION:
				return;
			case JFileChooser.ERROR_OPTION:
				return;
			default:
				return;
		}
		
		File selFile=_exportFileChooser.getSelectedFile();
		while (selFile.exists()) {
			int overwrite=JOptionPane.showConfirmDialog(getMainWindow(), "文件"+selFile.getName()+"已经存在，是否覆盖？","是否覆盖",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
			if (overwrite!=JOptionPane.YES_OPTION) {
//				return;
				status = _exportFileChooser.showSaveDialog( getMainWindow() );
				switch( status ) {
					case JFileChooser.APPROVE_OPTION:
						break;
					case JFileChooser.CANCEL_OPTION:
						return;
					case JFileChooser.ERROR_OPTION:
						return;
					default:
						return;
				}
			}else {
				break;
			}
			selFile=_exportFileChooser.getSelectedFile();
		}
		
		OutputStream fout;
		fout = new FileOutputStream( _exportFileChooser.getSelectedFile() );
		Connection conn;
		ResultSet rset, rsetDTLs;
		String str;

		// get the current time and date and insert in the XML file
		java.util.Date today = new java.util.Date();

		str = "<?xml version = '1.0' encoding = 'UTF-8'?>\n"
				+ "<!DOCTYPE xdxf SYSTEM \"xdxf.dtd\">\n"
				+ "<xdxf system=\"XiPAF\" ver=\"2.0.0\" date=\""
				+ today.toString() + "\">\n";

		int dtlCounter = 1;
//		int cclCounter = 1;
		wholeText+=str;
		byte buf0[] = str.getBytes();
		fout.write(buf0);

		try {
			final ConnectionDictionary connectionDictionary = ConnectionDictionary.getInstance( "mysql_dev", databaseServer );
			conn = connectionDictionary.getDatabaseAdaptor().getConnection( connectionDictionary );

			// Create a Statement
			Statement stmt = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
			Statement stmt1 = conn.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY );

			PreparedStatement stmt0;

			String[] sequences = new String[seqList.size()];
			for (int i = 0; i < seqList.size(); i++) {
				sequences[i] = (String) seqList.get(i);
			}

			if (sequences[0].length()>3 && sequences[0].substring(0, 4).equals("Ring")) {
				str = "  <comboseq id=\"Ring\">\n"
						+ "	  <sequence id=\"RING1\"/>\n"
						+ "	  <sequence id=\"RING2\"/>\n" + "  </comboseq>\n";
			} else {
				str = "  <comboseq id=\"DTL-MEBT\">\n"
						+ "	  <sequence id=\"DTL\"/>\n"
						+ "	  <sequence id=\"MEBT\"/>\n" + "  </comboseq>\n"
//						+ "  <comboseq id=\"DTL-LDmp\">\n"
//						+ "	  <sequence id=\"DTL\"/>\n"
//						+ "	  <sequence id=\"LDmp\"/>\n" + "  </comboseq>\n"
//						+ "  <comboseq id=\"MEBT-HDmp\">\n"
//						+ "	  <sequence id=\"MEBT\"/>\n"
//						+ "	  <sequence id=\"RING1\"/>\n"
//						+ "	  <sequence id=\"HDmp\"/>\n" + "  </comboseq>\n"
//						+ "  <comboseq id=\"RING1-HDmp\">\n"
//						+ "	  <sequence id=\"RING1\"/>\n"
//						+ "	  <sequence id=\"HDmp\"/>\n" + "  </comboseq>\n"
						+ "  <comboseq id=\"RING1-HEBT\">\n"
						+ "	  <sequence id=\"RING1\"/>\n"
						+ "	  <sequence id=\"HEBT\"/>\n" + "  </comboseq>\n"
						+ "  <comboseq id=\"HEBT-T2\">\n"
						+ "	  <sequence id=\"HEBT\"/>\n"
						+ "	  <sequence id=\"T2\"/>\n" + "  </comboseq>\n"
						+ "  <comboseq id=\"HEBT-T1\">\n"
						+ "	  <sequence id=\"HEBT\"/>\n"
						+ "	  <sequence id=\"T1\"/>\n" + "  </comboseq>\n"
						+ "  <comboseq id=\"RING\">\n"
						+ "	  <sequence id=\"RING1\"/>\n"
						+ "	  <sequence id=\"RING2\"/>\n" + "  </comboseq>\n";
			}
			buf0 = str.getBytes();
			wholeText+=str;
			fout.write(buf0);
			
			final PreparedStatement SEQUENCE_FETCH = conn.prepareStatement( "SELECT * FROM EPICS.DVC_SEQ where SEQ_NM = ?" );

			// loop through all the sequences
			for (int k = 0; k < sequences.length; k++) {

				String tmpID = null;
				boolean seqAttTag = true;
				
				final String theSequence = sequences[k];
				SEQUENCE_FETCH.setString( 1, theSequence );
				final ResultSet rsetSeq = SEQUENCE_FETCH.executeQuery();

				// produce <sequence> and its attributes
				while (rsetSeq.next()) {
					str = "  <sequence id=\"" + sequences[k] + "\"" + " len=\"" + rsetSeq.getString("SEQ_LENGTH") + "\"";
					if (sequences[k].equals("DTL"))
						// add DTL type to the <sequence>
						str = str.concat(" type=\"DTLTank\"");//只有DTL作为sequence有type属性

					str = str.concat(">\n");

					// start sequence attributes
					str = str.concat("   <attributes>\n");
					seqAttTag = true;

					if (!rsetSeq.getString("PREV_SEQ_NM").isEmpty()) {//使用isEmpty而不是==null来判断，因为数据库中存放的是空值
						if (rsetSeq.getString("ALT_SEQ_NM").isEmpty())
							str = str.concat("      <sequence predecessors=\""
									+ rsetSeq.getString("PREV_SEQ_NM")
									+ "\"/>\n");
						else
							str = str.concat("      <sequence predecessors=\""
									+ rsetSeq.getString("PREV_SEQ_NM") + ","
									+ rsetSeq.getString("ALT_SEQ_NM")
									+ "\"/>\n");
					} else {
						str = str.concat("      <sequence predecessors=\"null\"/>\n");
					}
				}

				System.out.println(sequences[k]);
				
				//增加加速腔序列额外的rfcavity属性，其余的序列没有改属性
				str = queryDTL_CCL_cavs(str, stmt, sequences, k);

				// insert a begin of sequence flag (as a virtual node)
				str = str + "    <node type=\"marker" + "\""
						+ " id=\"Begin_Of_" + sequences[k] + "\""
						+ " pos=\"0\" len=\"0\"/>\n";

				// get all the magnet nodes within this sequence
				//查询所有的磁铁元件
				stmt0 = conn.prepareStatement("SELECT EPICS.DVC.DVC_ID, "
								+ "EPICS.DVC.SUBSYS_ID, "
								+ "EPICS.DVC.DVC_TYPE_ID, "
								+ "EPICS.DVC.PARENT_DVC_ID, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_X, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Y, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Z, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_PITCH, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_YAW, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_ROLL, "
								+ "EPICS.BEAM_LINE_DVC.DIST_FROM_START, "
								+ "EPICS.BEAM_LINE_DVC.PHYS_LENGTH, "
								+ "EPICS.BEAM_LINE_DVC.DSGN_USAGE_IND, "
								+ "EPICS.BEAM_LINE_DVC.SEQ_NM, "
								+ "EPICS.MAG_DVC.POLARITY, "
								+ "EPICS.MAG_DVC.PM_QUAD_STRENGTH, "
								+ "EPICS.MAG_DVC.MAG_LENGTH, "
								+ "EPICS.MAG_DVC.APERTURE_H, "
								+ "EPICS.MAG_DVC.DIPOLE_BEND_ANGLE, "
								+ "EPICS.MAG_DVC.DIPOLE_ENTR_ROTATION_ANGLE, "
								+ "EPICS.MAG_DVC.DIPOLE_EXIT_ROTATION_ANGLE, "
								+ "EPICS.MAG_DVC.PATH_LENGTH, "
								+ "EPICS.MAG_DVC.DIPOLE_QUAD_TERM, "
								+ "EPICS.DVC_SET.SETTING_ID, "
								+ "EPICS.DVC_SET.SETTING_VALUE "
								+ "FROM EPICS.DVC, "
								+ "EPICS.BEAM_LINE_DVC, "
								+ "EPICS.MAG_DVC, "
								+ "EPICS.DVC_SET "
								+ "where (EPICS.DVC.DVC_TYPE_ID IN "
								+ "( 'QH', 'QV', 'PMQH', 'PMQV', 'DCH', 'DCV', 'DH', 'DV', "
								+ "'QTH', 'QTV', 'QSC', 'EKick', 'InjSptm', 'ExjSptm', 'Sptm', 'SH', 'SV', 'SSH', 'SSV', 'InjChicane', 'InjBump', 'SFM' )) "//新增InjChicane(kicker),InjBump(kicker),SFM(quad)
								+ "and EPICS.BEAM_LINE_DVC.SEQ_NM = '"
								+ sequences[k]
								+ "' "
								+ "and EPICS.DVC.Dvc_id = EPICS.BEAM_LINE_DVC.Dvc_id "
								+ "and EPICS.DVC.Dvc_id = EPICS.MAG_DVC.Dvc_id "
								+ "and EPICS.DVC.Dvc_id = EPICS.DVC_SET.Dvc_id "
								+ "order by "
								+ "EPICS.BEAM_LINE_DVC.Dist_From_Start");

				rset = stmt0.executeQuery();

				// generating the nodes
				int tmpCounter = 0;
				boolean chSuiteTag = true;

				// Iterate through the result and print the specified tag name
				while (rset.next()) {
					final String deviceID = rset.getString( "DVC_ID" );
					final String deviceType = rset.getString( "DVC_TYPE_ID" );
					System.out.println(deviceID);
					// check if the previous device has a closed </node>
					//应该是会查出多条DVC_ID相同的记录，所以需要和前面一条记录的DVC_ID比较，
					//若不同，则需要加上node的结尾</node>
					if ( !(deviceID.equals(tmpID)) && !(tmpID == null) ) {
						if (chSuiteTag) {
							str = str.concat("       </channelsuite>\n");
						}
						str = str.concat("    </node>\n");
					}

					if (!(deviceID.equals(tmpID))) {
						final boolean ringLike = sequences[k].startsWith("RING") || sequences[k].startsWith("T") || sequences[k].equals("HDmp") || sequences[k].equals("HEBT");
						//将切割磁铁类型改成DH
						if ( deviceType.equals("InjSptm") || deviceType.equals( "ExjSptm" ) || deviceType.equals("Sptm")) {
							str = str.concat("    <node type=\"DH" + "\" id=\"" + deviceID + "\"" + " pos=\"" + rset.getString("DIST_FROM_START") + "\""
									+ " len=\"" + getNumericString( rset.getString("PHYS_LENGTH") )
									+ "\"");
							if (rset.getString("DSGN_USAGE_IND").equals("Y"))
								str = str.concat(" status=\"true\">\n");
							else
								str = str.concat(" status=\"false\">\n");
						}
						// for Ring QTH and QTV, we replace the device type with
						// "QH" and "QV"，其中T是Trim的含义，即含有辅助绕组
//						else if ( deviceType.equals("QTH") && ringLike ) {
//							str = str.concat("    <node type=\"QH" + "\" id=\"" + deviceID + "\" pos=\""
//									+ rset.getString("DIST_FROM_START") + "\" len=\"" + getNumericString( rset.getString("PHYS_LENGTH") ) + "\"");
//							if (rset.getString("DSGN_USAGE_IND").equals("Y"))
//								str = str.concat(" status=\"true\">\n");
//							else
//								str = str.concat(" status=\"" + "false\">\n");
//
//						} else if (rset.getString("DVC_TYPE_ID").equals("QTV")
//								&& (sequences[k].equals("Ring1") || sequences[k].equals("Ring2") || sequences[k].equals("Ring3")
//										|| sequences[k].equals("Ring4") || sequences[k].equals("Ring5") || sequences[k].equals("RTBT1") || sequences[k].equals("RTBT2"))) {
//							str = str.concat("    <node type=\"QV" + "\""
//									+ " id=\"" + rset.getString("DVC_ID")
//									+ "\"" + " pos=\""
//									+ rset.getString("DIST_FROM_START") + "\""
//									+ " len=\"" + getNumericString( rset.getString("PHYS_LENGTH") )
//									+ "\"");
//							if (rset.getString("DSGN_USAGE_IND").equals("Y"))
//								str = str.concat(" status=\"true\">\n");
//							else
//								str = str.concat(" status=\"false\">\n");
//						}

						else {
							str = str.concat("    <node type=\"" + deviceType + "\"" + " id=\"" + deviceID + "\" pos=\"" + rset.getString("DIST_FROM_START") + "\""
									+ " len=\"" + getNumericString( rset.getString("PHYS_LENGTH") ) + "\"");
							if (rset.getString("DSGN_USAGE_IND").equals("Y"))
								str = str.concat(" status=\"true\">\n");
							else
								str = str.concat(" status=\"false\">\n");
						}
						str = str.concat("       <attributes>\n");

						/*
						 * 下面设置磁铁特有的属性，包括磁铁的长度，极性，默认场强等等
						 */
						// converting the polarity 'A', 'B' to '1', '-1' we need to handle the proton part of the machine and the H- part of the machine differently
						//将数据库中A和B转化成+1和-1两种表示方式
						if (rset.getString("POLARITY").equals("A")) {
							if ( ringLike && ( deviceType.equals("DCH") || deviceType.equals("DCV") || deviceType.equals("EKick") || deviceType.equals("InjChicane") || deviceType.equals("InjBump") ) )
								str = str.concat("          <magnet len=\"" + getNumericString( rset.getString("MAG_LENGTH") ) + "\" polarity=\"+1\"");
							else
								str = str.concat("          <magnet len=\"" + getNumericString( rset.getString("MAG_LENGTH") ) + "\" polarity=\"-1\"");
						} 
						else if (rset.getString("POLARITY").equals("B")) {
							if ( ringLike && ( deviceType.equals("DCH") || deviceType.equals("DCV") || deviceType.equals("EKick") || deviceType.equals("InjChicane") || deviceType.equals("InjBump") ) )
								str = str.concat("          <magnet len=\"" + getNumericString( rset.getString("MAG_LENGTH") ) + "\" polarity=\"-1\"");
							else
								str = str.concat("          <magnet len=\"" + getNumericString( rset.getString("MAG_LENGTH") ) + "\" polarity=\"+1\"");
						} 
						else
							str = str.concat("          <magnet len=\"" + getNumericString( rset.getString("MAG_LENGTH") ) + "\" polarity=\"0\"");

						/*
						 * 下面设置磁铁的默认场强，区分永磁铁和电磁铁
						 */
						//判断永磁铁和电磁铁，因为永磁铁只存在于DTL中，所以对对sequence进行判断是否为DTL
						//永磁四级铁的强度保存磁铁的表格当中，是固定值
						//电磁铁的强度保存在DVC_set表格当中，有一个默认的设计值
						//对于XiPAF来说，DTL中没有校正铁，不需要对校正铁进行操作
						if ( sequences[k].startsWith("DTL") ) {
							//其实DTL中只有PMQ，所以不需要额外的判断
//							if ( !deviceType.equals("DCH") && !deviceType.equals("DCV") ) {
//								// add field sign for PMQ, will be removed once the polarity key is populated
//								if ( deviceType.equals("PMQH") ) {
//									str = str.concat(" dfltMagFld=\"" + getNumericString( rset.getString("PM_QUAD_STRENGTH") ) + "\"/>\n");
//								} 
//								else if ( deviceType.equals("PMQV") ) {
//									str = str.concat(" dfltMagFld=\"" + getNumericString( rset.getString("PM_QUAD_STRENGTH") ) + "\"/>\n");
//								}
//							} else {
//								str = str.concat(" dfltMagFld=\"" + getNumericString( rset.getString("SETTING_VALUE") ) + "\"/>\n");
//							}
							str = str.concat(" dfltMagFld=\"" + getNumericString( rset.getString("PM_QUAD_STRENGTH") ) + "\"/>\n");
						} else {
							/*
							 * 添加二极铁特有的属性，包括pathlength，四级分量，各种角度等等。
							 */
							// for bending dipoles, we need to add extra attributes
							if (rset.getString("DVC_TYPE_ID").equals("DH")
									|| rset.getString("DVC_TYPE_ID").equals("DV")
									|| rset.getString("DVC_TYPE_ID").equals("InjSptm")
									|| rset.getString("DVC_TYPE_ID").equals("Sptm")
									|| rset.getString("DVC_TYPE_ID").equals("ExjSptm")) {
								str = str.concat(" pathLength=\"" + rset.getString("PATH_LENGTH") + "\" dipoleQuadComponent=\""+ getNumericString( rset.getString("DIPOLE_QUAD_TERM") ) + "\"");
								// we need to reverse bend angle, etc. for Chicane in HEBT2 and IDmp-
								//应该这两个磁铁的电荷状态发生了改变？这一块用不到，先注释掉
//								if ( (sequences[k].equals("HEBT2") && deviceID.equals( "Ring_Mag:DH_A11" ) ) || (sequences[k].equals("IDmp-") && deviceID.equals("Ring_Mag:DH_A12")) )
//									str = str.concat(" bendAngle=\""
//										+ -1.0 * Double.parseDouble(rset.getString("DIPOLE_BEND_ANGLE"))
//										+ "\""
//										+ " dipoleEntrRotAngle=\""
//										+ -1.0 * Double.parseDouble(rset.getString("DIPOLE_ENTR_ROTATION_ANGLE"))
//										+ "\""
//										+ " dipoleExitRotAngle=\""
//										+ -1.0 * Double.parseDouble(rset.getString("DIPOLE_EXIT_ROTATION_ANGLE"))
//										+ "\"");
								// other than the 2 middle Chicane dipoles
//								else
								str = str.concat(" bendAngle=\"" + rset.getString("DIPOLE_BEND_ANGLE")
									+ "\""
									+ " dipoleEntrRotAngle=\""
									+ getNumericString( rset.getString("DIPOLE_ENTR_ROTATION_ANGLE") )
									+ "\""
									+ " dipoleExitRotAngle=\""
									+ getNumericString( rset.getString("DIPOLE_EXIT_ROTATION_ANGLE") )
									+ "\"");
							}
							str = str.concat(" dfltMagFld=\"" + getNumericString( rset.getString("SETTING_VALUE") ) + "\"/>\n");
						}

						/*
						 * 下面设置alignment属性，即束线元件的准直误差属性
						 */
						// we use "zero" error for alignment for now
						str = str
								.concat("          <align x=\""
										+ ( Double.parseDouble( rset.getString("GLBL_COORD_X") ) - Double.parseDouble( rset.getString("GLBL_COORD_X") ) )
										+ "\" y=\""
										+ ( Double.parseDouble( rset.getString("GLBL_COORD_Y") ) - Double.parseDouble( rset.getString("GLBL_COORD_Y") ) )
										+ "\" z=\""
										+ (Double.parseDouble(rset.getString("GLBL_COORD_Z")) - Double.parseDouble(rset.getString("GLBL_COORD_Z")))
										+ "\""
										+ " pitch=\"" + rset.getString("GLBL_COORD_PITCH")
										+ "\""
										+ " yaw=\"" + rset.getString("GLBL_COORD_YAW")
										+ "\""
										+ " roll=\"" + rset.getString("GLBL_COORD_ROLL")
										+ "\"/>\n");
						/*
						 * 增加磁铁孔径
						 */
						// for magnet aperture
						str = str.concat("          <aperture shape=\"0\" x=\"" + getNumericString( rset.getString("APERTURE_H") ) + "\"/>\n       </attributes>\n");
						// dealing with the <channelsuite> tag for PMQs (no channelsuite)
						if ( sequences[k].startsWith("DTL") ) {
							// for non-PMQs 非PQM，即所有的电磁铁增加电源ps属性和channel suite属性
							if (!(rset.getString("DVC_TYPE_ID").equals("PMQH")) && !(rset.getString("DVC_TYPE_ID").equals("PMQV"))) {
								// for power supply
								str = str.concat("       <ps main=\"" + rset.getString("PARENT_DVC_ID") + "\"");
								// has trim winding or shunt
//								final String relatedDevice = rset.getString( "RELATED_DVC" );
//								final String relatedDeviceRole = rset.getString( "FUNC_DVC_GRP_NM" );
//								if ( relatedDevice != null && relatedDeviceRole != null && ( relatedDeviceRole.equals("shunt") || relatedDeviceRole.equals("trim") ) ) {
//									str = str.concat(" trim=\"" + relatedDevice + "\"");
//								}
								str = str.concat("/>\n");
								// for read back PV
								str += "       <channelsuite name=\"magnetsuite\">\n          <channel handle=\"fieldRB\"" + " signal=\"" + deviceID + ":B\" settable=\"false\"/>\n";
								chSuiteTag = true;
							} else {
								chSuiteTag = false;
							}
							// for non-DTL sequences
						} 
						//所有DTL之外的电磁铁，设置PS和channel suite
						else {
							// for non-PMQ power supply
							str = str.concat("       <ps main=\"" + rset.getString("PARENT_DVC_ID") + "\"");
							// has trim winding
//							final String relatedDevice = rset.getString( "RELATED_DVC" );
//							final String relatedDeviceRole = rset.getString( "FUNC_DVC_GRP_NM" );
//							if ( relatedDevice != null && relatedDeviceRole != null && ( relatedDeviceRole.equals("shunt") || relatedDeviceRole.equals("trim") ) ) {
//								str = str.concat(" trim=\"" + relatedDevice + "\"");
//							}

							str = str.concat("/>\n");
							// for read back PV
							str = str.concat("       <channelsuite name=\"" + "magnetsuite\">\n");
							str = str.concat("          <channel handle=\"" + "fieldRB" + "\"");
							// inside the following "if" bracket is for
							// temporary ring B readback
							
							//先处理环上的QTH，对于XiPAF这一部分没有
							//后续考虑将所有channel相关的信息，如handle，signal，settable从数据库channel表中获取
//							if ( ringLike ) {
//								if ( deviceID.indexOf("QTH") > 0) {
//									str = str.concat(" signal=\"" + deviceID.replaceAll("QTH", "QH") + ":B" + "\"" + " settable=\"false\"/>\n");
//								} 
//								else if ( deviceID.indexOf( "QTV" ) > 0 ) {
//									str = str.concat(" signal=\"" + deviceID.replaceAll("QTV", "QV") + ":B" + "\"" + " settable=\"false\"/>\n");
//								} 
//								else
//									str = str.concat(" signal=\"" + deviceID + ":B" + "\"" + " settable=\"false\"/>\n");
//							} 
//							else
							str = str.concat(" signal=\"" + rset.getString("DVC_ID") + ":B" + "\"" + " settable=\"false\"/>\n");
						}
						tmpID = deviceID;
					}

					tmpCounter++;
				}

				rset.close();
				stmt0.close();

				str = queryRFQ(str, stmt, sequences, k);

//				final PreparedStatement SIGNAL_FETCH = conn.prepareStatement( "select sgnl_id, sgnl_nm from epics.sgnl_rec where dvc_id = ? order by sgnl_nm" );
				/*
				 * 从channel表格中获取元件所有PV signal信息的查询语句，针对XiPAF数据结构有效
				 */
				final PreparedStatement SIGNAL_FETCH = conn.prepareStatement( "select epics.dvc.dvc_id, "
								+ "epics.channel.* "
								+"from epics.dvc, "
								+ "epics.channel "
								+ "where epics.dvc.dvc_id = ? "
								+ "and epics.dvc.dvc_type_id= epics.channel.dvc_type_id "
								+ "order by epics.channel.signal" );
				/*
				 * 添加所有的束测信息，如BPM，BCM，BLM，WS等等
				 */
				// put diagnostic devices, vacuum windows, laser strippers and foil (they all act like markers) here
				final String diagDeviceQuery = "SELECT EPICS.DVC.DVC_ID, "
								+ "EPICS.DVC.SUBSYS_ID, "
//								+ "EPICS.DVC.ACT_DVC_IND, "//这一列不知道什么含义，先注释掉
								+ "EPICS.DVC.DVC_TYPE_ID, "
//								+ "EPICS.DVC_DVC_TYPE_SFTW_ASSC.SFTW_ID, "//软件版本，后续再加上这个信息
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_X, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Y, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Z, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_PITCH, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_YAW, "
								+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_ROLL, "
								+ "EPICS.BEAM_LINE_DVC.DIST_FROM_START, "
								+ "EPICS.BEAM_LINE_DVC.PHYS_LENGTH, "
								+ "EPICS.BEAM_LINE_DVC.SEQ_NM, "
								+ "EPICS.BEAM_LINE_DVC.DSGN_USAGE_IND, "
								+ "EPICS.BPM_DVC.FREQ, "
								+ "EPICS.BPM_DVC.ELCTD_LENGTH, "
								+ "EPICS.BPM_DVC.ORIENT_IND "
								+ "FROM EPICS.DVC "
//								+ "EPICS.DVC_DVC_TYPE_SFTW_ASSC, "
								+ "INNER JOIN EPICS.BEAM_LINE_DVC ON EPICS.DVC.Dvc_id = EPICS.BEAM_LINE_DVC.Dvc_id "//这里用INNER JOIN比较好些
								+ "LEFT JOIN EPICS.BPM_DVC ON EPICS.DVC.Dvc_id = EPICS.BPM_DVC.Dvc_id "
								+ "where (EPICS.DVC.DVC_TYPE_ID IN "
								+ "( 'BPM','BCM', 'BLM', 'BSM', 'ChMPS', 'EMS', 'LW', 'WS', 'ND', 'Harp', 'Foil', 'LStrp', 'VIW', 'Tgt', 'BPRM', 'IC', 'DEG', 'Slit' )) "//增加类型：BPRM(PIC)，IC，DEG(marker)，Slit(marker)
//								+ "and EPICS.DVC.ACT_DVC_IND = 'Y' "
								+ "and EPICS.BEAM_LINE_DVC.SEQ_NM = '" + sequences[k] + "' "
//								+ "and EPICS.DVC.Dvc_id = EPICS.BEAM_LINE_DVC.Dvc_id(+) "//这里(+)在等号右边表示是左连接
//								+ "and EPICS.DVC.Dvc_id = EPICS.BPM_DVC.Dvc_id(+) "//MySQL不支持左连接
								//这里使用左连接的目的就是为了列出所有的诊断设备，而不仅仅是BPM元件，因为其他的束测元件只有node的一般信息
								//没有单独的表格存放信息
								//元件的软件版本信息后续再加上
//								+ "and EPICS.DVC.Dvc_id = EPICS.DVC_DVC_TYPE_SFTW_ASSC.DVC_ID(+) "
								+ "order by "
								+ "EPICS.BEAM_LINE_DVC.Dist_From_Start, EPICS.DVC.DVC_ID";
				
				System.out.println( "Diag Device Query: " + diagDeviceQuery );
				rset = stmt.executeQuery( diagDeviceQuery );
				while ( rset.next() ) {
					final String deviceID = rset.getString( "DVC_ID" );
//					final String softType = rset.getString( "SFTW_ID" );
					final String deviceType = rset.getString( "DVC_TYPE_ID" );
					System.out.println( deviceID );
//					if ( softType != null ) {
//						System.out.println( "************************ Soft Type:  >>>" + softType + "<<< ****************************" );
//					}

					// check if the previous device has a closed </node>
					if (!(deviceID.equals(tmpID)) && !(tmpID == null)) {
						if (chSuiteTag) {
							str = str.concat("       </channelsuite>\n");
						}
						str = str.concat("    </node>\n");
					}

					if ( !deviceID.equals( tmpID ) ) {
						final String devicePosition = rset.getString( "DIST_FROM_START" );
						final String deviceLength = getNumericString( rset.getString("PHYS_LENGTH") );
						final boolean deviceStatus = rset.getString("DSGN_USAGE_IND").equals("Y");
						final boolean ringLike = sequences[k].equals("RING1") || sequences[k].equals("RING2") || sequences[k].startsWith("T") || sequences[k].equals("HDmp") || sequences[k].equals("HEBT");
						
						if ( ringLike && deviceType.equals("BPM") ) {//环上的BPM类型叫“RBPM”
							str += "    <node type=\"R" + deviceType + "\" id=\"" + deviceID + "\"" + " pos=\"" + devicePosition + "\"" + " len=\"" + deviceLength + "\"";
							str += " status=\"" + deviceStatus + "\">\n";
						} 
						else {
							str += "    <node type=\"" + deviceType + "\"";
//							if ( softType != null ) {
//								str += " softType=\"" + softType + "\"";
//							}
							if (deviceType.equals("WS")){//加上WS的版本号
								str += " softType=\"" + "Version 2.0.0" + "\"";
							}
							str += " id=\"" + deviceID + "\" pos=\"" + devicePosition + "\"";
							str += " len=\"" + deviceLength + "\" status=\"" + deviceStatus + "\">\n";
						}
						
						//添加束测元件的attribute属性，这里只有准直误差信息
						str = str.concat("       <attributes>\n");
						// we use "zero" error for alignment for now
						str = str.concat("          <align x=\"" + (Double.parseDouble(rset.getString("GLBL_COORD_X")) - Double.parseDouble(rset.getString("GLBL_COORD_X"))) + "\""
							+ " y=\"" + (Double.parseDouble(rset.getString("GLBL_COORD_Y")) - Double.parseDouble(rset.getString("GLBL_COORD_Y"))) + "\""
							+ " z=\"" + (Double.parseDouble(rset.getString("GLBL_COORD_Z")) - Double.parseDouble(rset.getString("GLBL_COORD_Z"))) + "\""
							+ " pitch=\"" + rset.getString("GLBL_COORD_PITCH") + "\""
							+ " yaw=\"" + rset.getString("GLBL_COORD_YAW") + "\""
							+ " roll=\"" + rset.getString("GLBL_COORD_ROLL") + "\"/>\n");
						//添加BPM额外的信息，BPM bracket
						if ( deviceType.equals("BPM") ) {
							str = str.concat("          <bpm frequency=\"" + Double.parseDouble(rset.getString("FREQ")) + "\"\n"
											+ "               length=\"" + Double.parseDouble(rset.getString("ELCTD_LENGTH")) + "\"\n");
							if (rset.getString("ORIENT_IND").equals("U"))
								str = str.concat("               orientation=\"" + "-1\"/>\n");
							else if (rset.getString("ORIENT_IND").equals("D"))
								str = str.concat("               orientation=\"" + "1\"/>\n");
							else
								str = str.concat("/>\n");
						}
						str = str + "       </attributes>\n";
						
						
						
						//下面添加channelsuite属性，分别对不同类型的束测元件逐一添加信息
						//可以考虑增加RBPM类型，以区分环上BPM和输运线BPM，因为这两种类型的channelsuite还不一样
						if (deviceType.equals("BPM") || deviceType.equals("RBPM")) {
							str = str + "       <channelsuite name=\"bpmsuite\">\n";
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
							str +=channelEntry(signalTable);
							chSuiteTag = true;
						}else if (deviceType.equals("BCM")) {
							str = str + "       <channelsuite name=\"bcmsuite\">\n";
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
							str +=channelEntry(signalTable);
							chSuiteTag = true;
						}else if (deviceType.equals("BLM")) {
							str = str + "       <channelsuite name=\"blmsuite\">\n";
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
							str +=channelEntry(signalTable);
							chSuiteTag = true;
						}else if (deviceType.equals("WS")) {
							str = str + "       <channelsuite name=\"wssuite\">\n";
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
							str +=channelEntry(signalTable);
							chSuiteTag = true;
						}else if (deviceType.equals("BPRM")) {
							str = str + "       <channelsuite name=\"bprmsuite\">\n";
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
							str +=channelEntry(signalTable);
							chSuiteTag = true;
						}else if (deviceType.equals("IC")) {
							str = str + "       <channelsuite name=\"icsuite\">\n";
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
							str +=channelEntry(signalTable);
							chSuiteTag = true;
						}else {
							//其他类型的诊断设备没有channel suite，不添加该节点
							chSuiteTag = false;
						}
						

						tmpCounter++;
					}

					tmpID = deviceID;
				}

				// add closing tags for the last device
				// System.out.println(tmpCounter);
				if (tmpCounter != 0) {
					if (chSuiteTag) {
						str = str.concat("       </channelsuite>\n");
					}
					str = str.concat("    </node>\n");
				}

				/*
				 * 添加完束测元件之后添加RF gap的信息
				 */
				// put Rf gaps here
				
				String dtlCounter_str = String.valueOf(dtlCounter);//DTL tank的个数
//				String cclCounter_str = String.valueOf(cclCounter);
				
				//下面只是对DTL和CCL有效，对于其他的加速腔如debuncher和MA cavity需要再单独考虑，因为他们加速间隙少，寄生在其他sequence当中
				if (sequences[k].equals("DTL") || sequences[k].equals("DTL1")//考虑到CPHS需要用到DTL1和DTL2
						|| sequences[k].equals("DTL2")) {

					String queryString = "";
					// get all the rf gaps within this sequence
					/*
					 * 在XAL中加速腔都是作为一个sequence存在的，每一个gap都是一个node，也就是一个DVC，DVC_ID是针对RF gap的。
					 * 我想应该是这样的：DTL和CCL单独作为一个sequence，存放于DVC_SEQ和rf_dvc表当中，其gap位于DVC和beam_line_dvc当中，
					 * 因为gap作为一个node具有两张表的信息
					 * 同时RF gap中也存储了gap的额外信息，需要放在RF gap表当中。
					 * 而其他的如聚束腔，整体表现为node的性质，感觉晕了。。。
					 * XiPAF方案：只有散束器和磁合金腔加入DVC和beam_line_dvc，其余加速腔放在rf_dvc，加速间隙放在rf_gap
					 */
					//DTL tank内gap的查询语句
					if (sequences[k].substring(0, 3).equals("DTL"))
						queryString = "SELECT * " + "FROM EPICS.DVC a, "
								+ "EPICS.RF_GAP d " + "where a.subsys_id = 'DTL' "//DTL gap的 subsys_ID是DTL,sys_id是linac
								+ "and a.Dvc_id = d.Dvc_id "//DVC表中还需要加上L:DTL:CAV01，即DTL加速腔
								+ "and d.Dvc_id like '"
								+ "L:"+sequences[k].substring(0, 3) + ":Cav0"
								+ dtlCounter_str + "%'"
								+ "order by d.Dist_From_Start";
					//CCL tank内gap的查询语句
//					else
//						queryString = "SELECT * " + "FROM EPICS.DVC a, "
//								+ "EPICS.RF_GAP d " + "where a.sys_id = 'CCL' "
//								+ "and a.Dvc_id = d.Dvc_id "
//								+ "and d.Dvc_id like '"
//								+ sequences[k].substring(0, 3) + "_RF:Cav0"
//								+ cclCounter_str + "%'"
//								+ "order by d.Dist_From_Start";

					ResultSet rsetRfGaps = stmt.executeQuery(queryString);
					//RF_GAP_ID和DVC_ID的关系是？
					//从下面的代码来看RF_GAP_ID才是GAP的ID
					while (rsetRfGaps.next()) {
						System.out.println(rsetRfGaps.getString("RF_GAP_ID"));
						str = str.concat("    <node type=\"RG\" id=\""
								+ rsetRfGaps.getString("RF_GAP_ID") + "\""
								+ " pos=\""
								+ rsetRfGaps.getString("DIST_FROM_START")
								+ "\">\n" + "       <attributes>\n"
								+ "          <rfgap length=\""
								+ rsetRfGaps.getString("GAP_LENGTH") + "\""
								+ " phaseFactor=\""
								+ rsetRfGaps.getString("PHASE_OFFSET") + "\""
								+ " ampFactor=\""
								+ rsetRfGaps.getString("AMP_TILT") + "\""
								+ " TTF=\"" + rsetRfGaps.getString("TTF")
								+ "\"");
						if (rsetRfGaps.getString("END_CELL_IND").equals("1"))//20160913，在我们的数据库中使用1和0来表示状态，1为end cell，0非end cell，原来是Y和N
							str = str.concat(" endCell=\"1\"");
						else if (rsetRfGaps.getString("END_CELL_IND").equals(
								"0"))
							str = str.concat(" endCell=\"0\"");
						str = str.concat(" gapOffset=\""
								+ getNumericString( rsetRfGaps.getString("ELEC_CNTR_OFF") )
								+ "\"/>\n" + "       </attributes>\n"
								+ "    </node>\n");
					}
					rsetRfGaps.close();
					if (sequences[k].substring(0, 3).equals("DTL"))
						dtlCounter++;
//					else if (sequences[k].substring(0, 3).equals("CCL"))
//						cclCounter++;
					// for all other sequences with rf cavities in them.
				}
				
				//下面考虑其他加速腔，如散束器，磁合金腔
				//对于XiPAF来说，只要再考虑MEBT和Ring2，下面的查询语句需要结合自己的数据库设计进行。
				else if (sequences[k].equals("MEBT") || sequences[k].equals("RING2")) {

					// 查询所有的散束器、磁合金腔
					//20160914出现一个问题，检索不到磁合金腔
					ResultSet rsetRb;
					rsetRb = stmt.executeQuery("SELECT EPICS.DVC.DVC_ID, "
									+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_X, "
									+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Y, "
									+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Z, "
									+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_PITCH, "
									+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_YAW, "
									+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_ROLL, "
									+ "EPICS.BEAM_LINE_DVC.DIST_FROM_START, "
									+ "EPICS.BEAM_LINE_DVC.PHYS_LENGTH, "
									+ "EPICS.BEAM_LINE_DVC.DSGN_USAGE_IND, "
									+ "EPICS.DVC_SET.SETTING_ID, "
									+ "EPICS.DVC_SET.SETTING_VALUE, "
									+ "EPICS.RF_DVC.* "
									+ "FROM EPICS.DVC, "
									+ "EPICS.RF_DVC, "
									+ "EPICS.BEAM_LINE_DVC, "
									+ "EPICS.DVC_SET "
									+ "where EPICS.DVC.Dvc_id = EPICS.RF_DVC.Dvc_id "
									+ "and EPICS.DVC.Dvc_id = EPICS.DVC_SET.Dvc_id "
//									+ "and EPICS.BEAM_LINE_DVC.DVC_id like '"
//									+ sequences[k].substring(0, 3)//就是以sequence name开头的所有名称，要匹配的字符串用单引号括起来
//									+ "%' "
									+ "and EPICS.DVC.Dvc_id = EPICS.BEAM_LINE_DVC.DVC_id "
									+ "and EPICS.BEAM_LINE_DVC.SEQ_NM='"
									+ sequences[k]
									+ "' "
//									+ "and EPICS.DVC.Dvc_id = EPICS.BEAM_LINE_DVC.Dvc_id "
									+ "order by EPICS.BEAM_LINE_DVC.Dist_From_Start, EPICS.DVC.DVC_ID, EPICS.DVC_SET.SETTING_ID");
					
					// get all the rf gaps within this sequence
					ResultSet rsetRfGaps = stmt1.executeQuery("SELECT * FROM EPICS.RF_GAP ");

					String rbID = null;
					boolean rbAttTag;

					while (rsetRb.next()) {
						final String rbName = rsetRb.getString("DVC_ID");

						if ( !rbName.equals( rbID ) ) {
							rbAttTag = false;
							System.out.println( rbName );
							if ( sequences[k].startsWith( "Ring" ) )//环上的为磁合金腔
								str = str.concat("    <sequence type=\"MACavity\" id=\"" + rbName + "\"");
							else
								str = str.concat("    <sequence type=\"DeBnch\" id=\""+ rbName + "\"");//类型改为DeBnch，原来为Bnch
							//这些子序列放在大的序列外面，也具有node的一个bracket属性
							str = str.concat(" pos=\"" + rsetRb.getString("DIST_FROM_START") + "\" len=\"" + getNumericString( rsetRb.getString("PHYS_LENGTH") ) + "\"");
							if ( rsetRb.getString("DSGN_USAGE_IND").equals("Y") )
								str = str.concat(" status=\"true\">\n");
							else
								str = str.concat(" status=\"false\">\n");
							str = str.concat("       <attributes>\n");

							// we use "zero" error for alignment for now
							str = str.concat("          <align" 
								+ " x=\"0.0\" y=\"0.0\" z=\"0.0\""
								+ " pitch=\"" + rsetRb.getString("GLBL_COORD_PITCH") + "\""
								+ " yaw=\""+ rsetRb.getString("GLBL_COORD_YAW") + "\""
								+ " roll=\"" + rsetRb.getString("GLBL_COORD_ROLL") + "\"/>\n");
							str = str.concat("          <rfcavity");
							if (rsetRb.getString("SETTING_ID").equals("Amp"))//由Mv/m改成Amp，deg改成Phase，含义更明显
								str = str.concat("                 amp=\"" + rsetRb.getString("SETTING_VALUE") + "\"\n");
							if (rsetRb.getString("SETTING_ID").equals("Phase"))
								str = str.concat("                 phase=\"" + rsetRb.getString("SETTING_VALUE") + "\"\n");
							str = str.concat("                 TTFCoefs=\"" + rsetRb.getString("T0_COEF") + ", " + rsetRb.getString("T1_COEF") + ", " + rsetRb.getString("T2_COEF") + "\"\n"
								+ "                 TTFPrimeCoefs=\"" + rsetRb.getString("TP0_COEF") + ", " + rsetRb.getString("TP1_COEF") + ", " + rsetRb.getString("TP2_COEF") + "\"\n"
								+ "                 STFCoefs=\"" + rsetRb.getString("S0_COEF") + ", " + rsetRb.getString("S1_COEF") + ", " + rsetRb.getString("S2_COEF") + "\"\n"
								+ "                 STFPrimeCoefs=\"" + rsetRb.getString("SP0_COEF") + ", " + rsetRb.getString("SP1_COEF") + ", " + rsetRb.getString("SP2_COEF") + "\"\n"
								+ "                 TTF_endCoefs=\"" + rsetRb.getString("T0_END_COEF") + ", " + rsetRb.getString("T1_END_COEF") + ", " + rsetRb.getString("T2_END_COEF") + "\"\n"
								+ "                 TTFPrime_EndCoefs=\"" + rsetRb.getString("TP0_END_COEF") + ", " + rsetRb.getString("TP1_END_COEF") + ", " + rsetRb.getString("TP2_END_COEF") + "\"\n"
								+ "                 STF_endCoefs=\"" + rsetRb.getString("S0_END_COEF") + ", " + rsetRb.getString("S1_END_COEF") + ", " + rsetRb.getString("S2_END_COEF") + "\"\n"
								+ "                 STFPrime_endCoefs=\"" + rsetRb.getString("SP0_END_COEF") + ", " + rsetRb.getString("SP1_END_COEF") + ", " + rsetRb.getString("SP2_END_COEF") + "\"\n"
								+ "                 structureMode=\"" + rsetRb.getString("STRUCT_TYPE_IND") + "\"\n");
							//对于超导腔还有TTF和Q loaded熟悉
//							if (sequences[k].substring(0, 3).equals("SCL"))
//								str = str.concat("                 structureTTF=\"" + rsetRb.getString("TTF") + "\"\n                 qLoaded=\"" + getNumericString( rsetRb.getString("Q_LD") ) + "\"\n");

							rbID = rbName;
						} 
						else {
							if ( rsetRb.getString("SETTING_ID").equals("Amp") ) {
								str = str.concat("                 amp=\"" + rsetRb.getString("SETTING_VALUE") + "\"");
							}
							else if ( rsetRb.getString("SETTING_ID").equals("Phase") )
								str = str.concat("                 phase=\"" + rsetRb.getString("SETTING_VALUE") + "\"");
							rbAttTag = true;
						}

						if (rbAttTag) {
							str = str.concat(" freq=\"" + rsetRb.getString("DSGN_FREQ") + "\"/>\n       </attributes>\n       <channelsuite name=\"rfsuite\">\n");

							/*
							 * 下面这部分代码为加速腔的channel suite，改成从数据库channel表中获取，方便后续的更新维护
							 * 而不能写死在代码中,改成两行代码即可，大大精简了代码
							 */
							Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, rbName );
							str +=channelEntry(signalTable);
							str = str + "       </channelsuite>\n";

							str = queryRFGaps(str, rsetRfGaps, rbName);

							str = str.concat("    </sequence>\n");

						}
					}

					rsetRfGaps.close();
					rsetRb.close();

				}

				// close the <sequence>
				str = str + "  </sequence>\n";
				wholeText+=str;
				byte buf[] = str.getBytes();
				fout.write(buf);
			}

			str = queryPowerSupplies(fout, conn, stmt, sequences);
		} catch ( Exception exception ) {
			System.err.println("JDBC Error:" + exception.getMessage());
			exception.printStackTrace();
			displayError( "Error", "Exception generating device.", exception );
		}
		wholeText+=str;
		byte buf1[] = str.getBytes();
		fout.write(buf1);
		fout.close();
	}

	private String queryRFGaps(String str, ResultSet rsetRfGaps, String rbName)
			throws SQLException {
		// // get all the rf gaps within this sequence
		// ResultSet rsetRfGaps = stmt1.executeQuery("SELECT
		// * "
		// +"FROM EPICS.RF_GAP "
		// +"where RF_GAP_id like '" + rbName +"%'"
		// );
		//
		rsetRfGaps.first();
		while (rsetRfGaps.next()) {
			//从下面的代码来看，DVC_ID值得是整个buncher的ID，GAP有单独的ID。
			if (rsetRfGaps.getString("DVC_ID").equals(rbName)) {
				System.out.println(rsetRfGaps.getString("RF_GAP_ID"));
				str = str.concat("       <node type=\"RG\" id=\""
								+ rsetRfGaps.getString("RF_GAP_ID") + "\""
								+ " pos=\""
								+ rsetRfGaps.getString("DIST_FROM_START")
								+ "\">\n" + "         <attributes>\n"
								+ "          <rfgap length=\""
								+ rsetRfGaps.getString("GAP_LENGTH") + "\""
								+ " phaseFactor=\""
								+ rsetRfGaps.getString("PHASE_OFFSET") + "\""
								+ " ampFactor=\""
								+ rsetRfGaps.getString("AMP_TILT") + "\""
								+ " TTF=\"" + rsetRfGaps.getString("TTF")
								+ "\"");
				if (rsetRfGaps.getString("END_CELL_IND").equals("Y"))
					str = str.concat(" endCell=\"1\"");
				else if (rsetRfGaps.getString("END_CELL_IND").equals("N"))
					str = str.concat(" endCell=\"0\"");
				str = str.concat(" gapOffset=\""
						+ getNumericString( rsetRfGaps.getString("ELEC_CNTR_OFF") ) + "\"/>\n");
				str = str.concat("         </attributes>\n");
				str = str.concat("       </node>\n");
			}
		}
		return str;
	}

	private String queryPowerSupplies( OutputStream fout, Connection connection, Statement stmt, String[] sequences ) throws IOException, SQLException {
		String str;
		str = "  <powersupplies>\n";
		wholeText+=str;
		byte buf[] = str.getBytes();
		fout.write(buf);
		
		/*
		 * 查询所有的电源类设备的信号
		 */
		final PreparedStatement SIGNAL_FETCH = connection.prepareStatement( "select epics.dvc.dvc_id, "
				+ "epics.channel.* "
				+"from epics.dvc, "
				+ "epics.channel "
				+ "where epics.dvc.dvc_id = ? "
				+ "and epics.dvc.dvc_type_id= epics.channel.dvc_type_id "
				+ "order by epics.channel.signal" );
		
		// query for all systems related to a specified sequence
		//先查询系统的原因是后续的查询没有sequence信息，先把sequence信息放在system id当中
		//但是不允许同一个system id对应不同的sequence，这样通过system id就会把其他sequence的信息包含进来
		final PreparedStatement systemQuery = connection.prepareStatement( "select epics.dvc.sys_id "
				+ "from epics.dvc, "
				+ "epics.beam_line_dvc "
				+ "where epics.dvc.dvc_id = epics.beam_line_dvc.dvc_id "
				+ "and epics.beam_line_dvc.seq_nm = ? group by epics.dvc.sys_id" );
		
		// collect all systems covering all the specified sequences
		final Set<String> systemSet = new HashSet<String>();
		for ( final String sequence : sequences ) {
			systemQuery.setString( 1, sequence );
			final ResultSet resultSet = systemQuery.executeQuery();
			while ( resultSet.next() ) {
				systemSet.add( resultSet.getString( "sys_id" ) );
			}
		}
		// order the systems by name
		final List<String> systems = new ArrayList<String>( systemSet );
		Collections.sort( systems );
		
		// for power supplies
		String tmpSystem = "";
		for ( final String system : systems ) {
			 String queryString = "SELECT DISTINCT EPICS.DVC.DVC_ID, "
					+"EPICS.DVC.DVC_TYPE_ID "
					+ "FROM EPICS.DVC "
					+ "where (EPICS.DVC.DVC_TYPE_ID IN ('PSMain','PSTrim') "
					+ "and EPICS.DVC.SUBSYS_ID='Mag') "
					+ "and EPICS.DVC.sys_id = '" + system + "' "
					+ "order by EPICS.DVC.DVC_ID";
					
			if ( !tmpSystem.equals( system ) ) {
				System.out.println( "---" + system );
				tmpSystem = system;
				ResultSet rsetPS;

				rsetPS = stmt.executeQuery( queryString );

				while (rsetPS.next()) {
                    final String deviceID = rsetPS.getString( "DVC_ID" );
					System.out.println( deviceID );
					if (rsetPS.getString("DVC_TYPE_ID").equals("PSMain")) {
						str = "    <ps type=\"main\" id=\"" + rsetPS.getString("DVC_ID") + "\">\n";
						str = str + "       <channelsuite name=\"pssuite\">\n";
						Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
						str +=channelEntry(signalTable);
						str = str + "       </channelsuite>\n    </ps>\n";
					}else if (rsetPS.getString("DVC_TYPE_ID").equals("PSTrim")) {
						str = "    <ps type=\"trim\" id=\"" + rsetPS.getString("DVC_ID") + "\">\n";
						str = str + "       <channelsuite name=\"pssuite\">\n";
						Map<String,List<String>> signalTable=fetchChannels( SIGNAL_FETCH, deviceID );
						str +=channelEntry(signalTable);
						str = str + "       </channelsuite>\n    </ps>\n";
					}else {
						//do nothing
					}
					wholeText+=str;
					byte bufPS[] = str.getBytes();
					fout.write(bufPS);
				}
			}

		}

		str = " </powersupplies>\n";
		str = str + "</xdxf>";
		return str;
	}

	private String queryRFQ(String str, Statement stmt, String[] sequences,
			int k) throws SQLException {
		// RFQ
		if (sequences[k].equals("RFQ")) {
			ResultSet rsetRfq;
			rsetRfq = stmt.executeQuery("SELECT EPICS.DVC.DVC_ID, "
					+ "EPICS.BEAM_LINE_DVC.DSGN_USAGE_IND, "
					+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_X, "
					+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Y, "
					+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_Z, "
					+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_PITCH, "
					+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_YAW, "
					+ "EPICS.BEAM_LINE_DVC.GLBL_COORD_ROLL, "
					+ "EPICS.BEAM_LINE_DVC.DIST_FROM_START, "
					+ "EPICS.BEAM_LINE_DVC.PHYS_LENGTH "
					+ "FROM EPICS.DVC, " + "EPICS.BEAM_LINE_DVC "
					+ "where (EPICS.DVC.DVC_TYPE_ID IN ('RF')) "
					+ "and EPICS.DVC.Dvc_id = EPICS.BEAM_LINE_DVC.Dvc_id "
					+ "and EPICS.BEAM_LINE_DVC.DSGN_USAGE_IND = 'Y' "
					+ "order by EPICS.BEAM_LINE_DVC.Dist_From_Start");
			while (rsetRfq.next()) {
				System.out.println(rsetRfq.getString("DVC_ID"));
				str = str.concat("    <node type=\"RF\" id=\""
						+ rsetRfq.getString("DVC_ID") + "\"" + " pos=\""
						+ rsetRfq.getString("DIST_FROM_START") + "\">\n"
						+ "    </node>\n");
			}
			rsetRfq.close();
		}
		return str;
	}

	private String queryDTL_CCL_cavs(String str, Statement stmt, String[] sequences, int k) throws SQLException {
		ResultSet rsetDTLs;
		boolean seqAttTag;
		// Select from tables... We have to deal with DTL in a different
		// way because of the PMQs
		if (sequences[k].equals("DTL") || sequences[k].equals("DTL1") || sequences[k].equals("DTL2")) {
			// add DTL sequence attributes
			rsetDTLs = stmt.executeQuery( "SELECT * FROM EPICS.DVC a, EPICS.RF_DVC b, EPICS.BEAM_LINE_DVC c, EPICS.DVC_SET d where a.Dvc_id = b.Dvc_id and a.Dvc_id = d.Dvc_id and c.SEQ_NM='" + sequences[k] + "' and a.Dvc_id = c.Dvc_id and c.DSGN_USAGE_IND = 'Y' " );

			String cavID = null;
			while (rsetDTLs.next()) {
                final String deviceID = rsetDTLs.getString( "DVC_ID" );
//                final String cavityNumber = deviceID.substring( deviceID.indexOf("Cav") + 3 );

				if (!deviceID.equals(cavID)) {
					str = str.concat("      <rfcavity ");
					if (rsetDTLs.getString("SETTING_ID").equals("Amp"))
						str = str.concat(" amp=\"" + rsetDTLs.getString("SETTING_VALUE") + "\"");
					if (rsetDTLs.getString("SETTING_ID").equals("Phase"))
						str = str.concat(" phase=\"" + rsetDTLs.getString("SETTING_VALUE") + "\"");
					seqAttTag = false;
				} 
                else {
					if (rsetDTLs.getString("SETTING_ID").equals("Amp"))
						str = str.concat(" amp=\"" + rsetDTLs.getString("SETTING_VALUE") + "\"");
					if (rsetDTLs.getString("SETTING_ID").equals("Phase"))
						str = str.concat(" phase=\"" + rsetDTLs.getString("SETTING_VALUE") + "\"");
					str = str.concat(" freq=\""
							+ rsetDTLs.getString("DSGN_FREQ") + "\"\n"
							+ "                 TTFCoefs=\""
							+ rsetDTLs.getString("T0_COEF") + ", "
							+ rsetDTLs.getString("T1_COEF") + ", "
							+ rsetDTLs.getString("T2_COEF") + "\"\n"
							+ "                 TTFPrimeCoefs=\""
							+ rsetDTLs.getString("TP0_COEF") + ", "
							+ rsetDTLs.getString("TP1_COEF") + ", "
							+ rsetDTLs.getString("TP2_COEF") + "\"\n"
							+ "                 STFCoefs=\""
							+ rsetDTLs.getString("S0_COEF") + ", "
							+ rsetDTLs.getString("S1_COEF") + ", "
							+ rsetDTLs.getString("S2_COEF") + "\"\n"
							+ "                 STFPrimeCoefs=\""
							+ rsetDTLs.getString("SP0_COEF") + ", "
							+ rsetDTLs.getString("SP1_COEF") + ", "
							+ rsetDTLs.getString("SP2_COEF") + "\"\n"
							+ "                 TTF_endCoefs=\""
							+ rsetDTLs.getString("T0_END_COEF") + ", "
							+ rsetDTLs.getString("T1_END_COEF") + ", "
							+ rsetDTLs.getString("T2_END_COEF") + "\"\n"
							+ "                 TTFPrime_EndCoefs=\""
							+ rsetDTLs.getString("TP0_END_COEF") + ", "
							+ rsetDTLs.getString("TP1_END_COEF") + ", "
							+ rsetDTLs.getString("TP2_END_COEF") + "\"\n"
							+ "                 STF_endCoefs=\""
							+ rsetDTLs.getString("S0_END_COEF") + ", "
							+ rsetDTLs.getString("S1_END_COEF") + ", "
							+ rsetDTLs.getString("S2_END_COEF") + "\"\n"
							+ "                 STFPrime_endCoefs=\""
							+ rsetDTLs.getString("SP0_END_COEF") + ", "
							+ rsetDTLs.getString("SP1_END_COEF") + ", "
							+ rsetDTLs.getString("SP2_END_COEF") + "\"\n"
							+ "                 structureMode=\""
							+ rsetDTLs.getString("STRUCT_TYPE_IND") + "\"/>\n"
							+ "   </attributes>\n");

					seqAttTag = false;
                    final String fcm = rsetDTLs.getString("DVC_ID");//对于XiPAF使用DVC_ID即可，原为RELATED_DVC
                    //后续打算使用channel表格中的数据，这样方便控制使用哪些pv。

					str = str.concat("   <channelsuite name=\"rfsuite\">\n"
									+ "        <channel handle=\"cavAmpSet\" signal=\"" + fcm + ":CtlAmpSet\" settable=\"true\"/>\n"
                                    + "        <channel handle=\"cavPhaseSet\" signal=\"" + fcm + ":CtlPhaseSet\" settable=\"true\"/>\n"
									+ "        <channel handle=\"cavAmpAvg\" signal=\"" + fcm + ":cavAmpAvg\" settable=\"false\"/>\n"
									+ "        <channel handle=\"cavPhaseAvg\" signal=\"" + fcm + ":cavPhaseAvg\" settable=\"false\"/>\n"
									+ "        <channel handle=\"deltaTRFStart\" signal=\"" + fcm + ":deltaTRFStart\" settable=\"true\"/>\n"
									+ "        <channel handle=\"deltaTRFEnd\" signal=\"" + fcm + ":deltaTRFEnd\" settable=\"true\"/>\n"
									+ "        <channel handle=\"tDelay\" signal=\"" + fcm + ":tDelay\" settable=\"true\"/>\n"
                                    + "        <channel handle=\"blankBeam\" signal=\"" + fcm + ":BlnkBeam\" settable=\"true\"/>\n"
									+ "   </channelsuite>\n");

				}

				cavID = rsetDTLs.getString("DVC_ID");

				if (seqAttTag) {
                    final String fcm = rsetDTLs.getString("DVC_ID");//对于XiPAF使用DVC_ID即可，原为RELATED_DVC
					str = str.concat("      <rfcavity "
									+ " freq=\""
									+ rsetDTLs.getString("DSGN_FREQ")
									+ "\"/>\n"
									+ "   </attributes>\n"
									+ "   <channelsuite name=\"rfsuite\">\n"
									+ "        <channel handle=\"cavAmpSet\" signal=\"" + fcm + ":CtlAmpSet\" settable=\"true\"/>\n"
									+ "        <channel handle=\"cavPhaseSet\" signal=\"" + fcm + ":CtlPhaseSet\" settable=\"true\"/>\n"
									+ "        <channel handle=\"cavAmpAvg\" signal=\"" + fcm + ":cavAmpAvg\" settable=\"false\"/>\n"
									+ "        <channel handle=\"cavPhaseAvg\" signal=\"" + fcm + ":cavPhaseAvg\" settable=\"false\"/>\n"
									+ "        <channel handle=\"deltaTRFStart\" signal=\"" + fcm + ":deltaTRFStart\" settable=\"true\"/>\n"
									+ "        <channel handle=\"deltaTRFEnd\" signal=\"" + fcm + ":deltaTRFEnd\" settable=\"true\"/>\n"
									+ "        <channel handle=\"tDelay\" signal=\"" + fcm + ":tDelay\" settable=\"true\"/>\n"
                                    + "        <channel handle=\"blankBeam\" signal=\"" + fcm + ":BlnkBeam\" settable=\"true\"/>\n"
									+ "   </channelsuite>\n");

				}

			}

		}

		else {
			str = str + "   </attributes>\n";

		}
		return str;
	}
	
	
	/** convert a raw value to a valid numeric value by converting "null" to "0.0" */
	static private String getNumericString( final String rawValue ) {
		return rawValue != null && !rawValue.trim().equalsIgnoreCase( "null" ) ? rawValue : "0.0";
	}
	

	/*    *//**
			 * Convenience method for getting the main window cast to the proper
			 * subclass of XalWindow. This allows me to avoid casting the window
			 * every time I reference it.
			 * 
			 * @return The main window cast to its dynamic runtime class
			 */
	/*
	 * private Db2XalWindow myWindow() { return (Db2XalWindow)mainWindow; }
	 * 
	 */
	/**
	 * Instantiate a new PlainDocument that servers as the document for the text
	 * pane. Create a handler of text actions so we can determine if the
	 * document has changes that should be saved.
	 */
	private void makeTextDocument() {
		textDocument = new PlainDocument();
		textDocument.addDocumentListener(new DocumentListener() {
			public void changedUpdate(javax.swing.event.DocumentEvent evt) {
				setHasChanges(true);
			}

			public void removeUpdate(DocumentEvent evt) {
				setHasChanges(true);
			}

			public void insertUpdate(DocumentEvent evt) {
				setHasChanges(true);
			}
		});
	}

	/**
	 * Make a main window by instantiating the my custom window. Set the text
	 * pane to use the textDocument variable as its document.
	 */
	public void makeMainWindow() {
		mainWindow = new Db2XalWindow(this);
		if (getSource() != null) {
			XmlDataAdaptor xda = XmlDataAdaptor.adaptorForUrl(getSource(),
					false);
			update(xda.childAdaptor("db2xal"));
			setHasChanges(false);
		}
		setHasChanges(false);
	}

	/**
	 * Save the document to the specified URL.
	 * 
	 * @param url
	 *            The URL to which the document should be saved.
	 */
	public void saveDocumentAs(URL url) {
		try {
			XmlDataAdaptor xda = XmlDataAdaptor.newEmptyDocumentAdaptor();
			xda.writeNode(this);
			xda.writeToUrl(url);
			setHasChanges(false);
		} catch (XmlDataAdaptor.WriteException exception) {
			exception.printStackTrace();
			displayError("Save Failed!",
					"Save failed due to an internal write exception!",
					exception);
		} catch (Exception exception) {
			exception.printStackTrace();
			displayError("Save Failed!",
					"Save failed due to an internal exception!", exception);
		}
	}

	public String dataLabel() {
		return "db2xal";
	}

	public void update(DataAdaptor adaptor) {
		if (getSource() != null) {
			// set up the right sequence combo from selected primaries:
			List<DataAdaptor> temp = adaptor.childAdaptors("sequences");
			if (temp.isEmpty())
				return; // bail out, nothing left to do

			seqList = new ArrayList<Object>();
			DataAdaptor da2a = adaptor.childAdaptor("sequences");
			// String seqName = da2a.stringValue("name");

			temp = da2a.childAdaptors("seq");
			Iterator<DataAdaptor> itr = temp.iterator();
			while (itr.hasNext()) {
				DataAdaptor da = itr.next();
				seqList.add((da.stringValue("name")));
			}
			// setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
		}
	}

	public void write(DataAdaptor adaptor) {
		// save date
		adaptor.setValue("date", new java.util.Date().toString());
		// save selected sequences
		DataAdaptor daSeq = adaptor.createChild("sequences");

		Iterator<Object> itr = seqList.iterator();

		while (itr.hasNext()) {
			DataAdaptor daSeqComponents = daSeq.createChild("seq");
			daSeqComponents.setValue("name", itr.next());
		}

	}

}
