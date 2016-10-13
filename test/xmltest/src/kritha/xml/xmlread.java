package kritha.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

import xal.tools.data.DataAdaptor;
import xal.tools.xml.XmlDataAdaptor;
import xal.tools.xml.XmlDataAdaptor.ParseException;
import xal.tools.xml.XmlDataAdaptor.ResourceNotFoundException;

public class xmlread {
	public static void main(String[] args) {
		File file=new File("E:/xal/documents/Pasta/DTL1_103015_0.2%w0.03.pst");
		XmlDataAdaptor document_adaptor = null;
		try {
			document_adaptor = XmlDataAdaptor.adaptorForFile(file,false);
		} catch (ParseException | ResourceNotFoundException | MalformedURLException e1) {
			// TODO Auto-generated catch block
			System.out.println("file access error");
			e1.printStackTrace();
		}
		DataAdaptor PastaAdaptor = document_adaptor.childAdaptor("PastaSetup");
		DataAdaptor ScanAdaptor = PastaAdaptor.childAdaptor("scan");
		DataAdaptor MPvsAdaptor = ScanAdaptor.childAdaptor("measure_PVs");
		//DataAdaptor MPvsAdaptor = ScanAdaptor.childAdaptor("measure_PVs");
		List<DataAdaptor> MPvAdaptors = MPvsAdaptor.childAdaptors("MeasuredPV"); 
//		Iterator<DataAdaptor>  iterator  =  MPvAdaptors.iterator() ;
//		while(iterator.hasNext()){
//			DataAdaptor XX=(DataAdaptor)iterator.next();
//		}

		DataAdaptor BPM1Phase=MPvAdaptors.get(0);
		DataAdaptor BPM2Phase=MPvAdaptors.get(2);
		List<DataAdaptor> BPM1Graphs = BPM1Phase.childAdaptors("Graph_For_scanPV"); 
		List<DataAdaptor> BPM2Graphs = BPM2Phase.childAdaptors("Graph_For_scanPV");
		int i=0,j=0;
		int amnum=BPM1Graphs.size();
		int startphase=-65;
		int endphase=-25;
		int phasenum=endphase-startphase+1;
		double[][] phase1=new double[amnum][phasenum];
		double[][] phase2=new double[amnum][phasenum];
		for(DataAdaptor graph:BPM1Graphs){
			List<DataAdaptor> XYErrs = graph.childAdaptors("XYErr"); 
			j=0;
			for(DataAdaptor XYErr:XYErrs){
				phase1[i][j]=XYErr.doubleValue("y");
				j++;
			}
			i++;
		}
		i=0;
		for(DataAdaptor graph:BPM2Graphs){
			List<DataAdaptor> XYErrs = graph.childAdaptors("XYErr"); 
			j=0;
			for(DataAdaptor XYErr:XYErrs){
				phase2[i][j]=XYErr.doubleValue("y");
				j++;
			}
			i++;
		}
		
		File outfile = new File("E:/xal/documents/Pasta/scandata0315/phase1.txt");  //存放数组数据的文件	  
		FileWriter out = null;
		try {
			out = new FileWriter(outfile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  //文件写入流		  
		//将数组中的数据写入到文件中。每行各数据之间TAB间隔
		for(int i1=0;i1<phasenum;i1++){
			for(int j1=0;j1<amnum;j1++){
				try {
					out.write(phase1[j1][i1]+"\t");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				out.write("\r\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File outfile2 = new File("E:/xal/documents/Pasta/scandata0315/phase2.txt");  //存放数组数据的文件	  
		FileWriter out2 = null;
		try {
			out2 = new FileWriter(outfile2);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  //文件写入流		  
		//将数组中的数据写入到文件中。每行各数据之间TAB间隔
		for(int i1=0;i1<phasenum;i1++){
			for(int j1=0;j1<amnum;j1++){
				try {
					out2.write(phase2[j1][i1]+"\t");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				out2.write("\r\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		File outfile3 = new File("E:/xal/documents/Pasta/scandata0315/diff.txt");  //存放数组数据的文件	  
		FileWriter out3 = null;
		try {
			out3 = new FileWriter(outfile3);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  //文件写入流		  
		//将数组中的数据写入到文件中。每行各数据之间TAB间隔
		for(int i1=0;i1<phasenum;i1++){
			for(int j1=0;j1<amnum;j1++){
				try {
					double diff=phase2[j1][i1]-phase1[j1][i1];
					if(diff>180){
						diff-=360;
					}
					if(diff<-180){
						diff+=360;
					}
					out3.write(diff+"\t");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				out3.write("\r\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			out3.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
