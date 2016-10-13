/*
 * Db2XalWindow.java
 *
 * Created on March 14, 2003, 10:25 AM
 */
 
package xal.app.db2xal;

import javax.swing.*;
import xal.extension.application.*;
import xal.tools.apputils.NonConsecutiveSeqSelector;

/**
 * Db2XalWindow is a subclass of XalWindow for Db2Xal application.
 *
 * @author  Paul Chu
 */
public class Db2XalWindow extends XalWindow {
    /** serialization ID */
    private static final long serialVersionUID = 1L;
    protected JTextArea textView;
    private NonConsecutiveSeqSelector seqSel;
    
    
    /** Creates a new instance of MainWindow */
    public Db2XalWindow(XalDocument aDocument) {
        super(aDocument);
        setSize(1000, 600);
        makeContent();
    }
    
    
    /** 
     * Getter of the text view that displays the document content.
     * @return The text area that displays the document text.
     */
    public JTextArea getTextView() {
        return textView;
    }
    
    /** 
     * Getter of the sequence selector that displays all squences in XiPAF.
     * @return The sequence selector that displays all squences in XiPAF..
     */
    public NonConsecutiveSeqSelector getSeqSelector() {
        return seqSel;
    }
    
    
    /**
     * Create the main window subviews.
     */
    protected void makeContent() {
        textView = new JTextArea();
        textView.setLineWrap(true);
        textView.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textView);
        getContentPane().add(scrollPane);
        
        /*
         * add a jdiag in the main window
         * by yangye in 20160816
         */
        seqSel = new NonConsecutiveSeqSelector();
//		JDialog jDialog=seqSel.selectSequence();
        JScrollPane scrollPaneTable = new JScrollPane(seqSel.geTable(),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPaneTable, "West");
		
        JTextField textField = new JTextField();
        getContentPane().add(textField, "South");
    }    
}
