package kritha.xml;
import java.io.File;
import java.io.IOException;

import xal.tools.data.DataAdaptor;
import xal.tools.xml.*;

public class xmltest {
	
	public static void main(String[] args) {
		XmlDataAdaptor document_adaptor = XmlDataAdaptor.newEmptyDocumentAdaptor();
		DataAdaptor configAdaptor=document_adaptor.createChild("dbconfig");
		
		//adaptors
		DataAdaptor dbAdaptors=configAdaptor.createChild("adaptors");
		dbAdaptors.setValue("default", "Oracle");
		
			//adaptor
			DataAdaptor dbAdaptor=dbAdaptors.createChild("adaptor");
			dbAdaptor.setValue("name", "Oracle");
			dbAdaptor.setValue("class", "xal.plugin.oracle.OracleDatabaseAdaptor");
		
		//servers
		DataAdaptor serverGroup=configAdaptor.createChild("servers");
		serverGroup.setValue("default", "Oracle");
		
			//server
			DataAdaptor serverAdaptor=serverGroup.createChild("server");
			serverAdaptor.setValue("name", "Oracle");
			serverAdaptor.setValue("url", "jdbc:oracle:thin:@localhost:1521:orcl");
			serverAdaptor.setValue("adaptor", "Oracle");//这是adaptor name 不是adaptor class name

		//accounts
		DataAdaptor accountGroup=configAdaptor.createChild("accounts");
		accountGroup.setValue("default", "test");
		
			//account
			DataAdaptor accountAdaptor=accountGroup.createChild("account");
			accountAdaptor.setValue("name", "test");
			accountAdaptor.setValue("user", "test");
			accountAdaptor.setValue("password", "697452");
			accountAdaptor.setValue("server", "Oracle");//这是server name 不是url
			
			//account
			DataAdaptor accountAdaptor2=accountGroup.createChild("account");
			accountAdaptor2.setValue("name", "sys");
			accountAdaptor2.setValue("user", "sys");
			accountAdaptor2.setValue("password", "697452");
			accountAdaptor2.setValue("server", "Oracle");
			
		//write to config.xml in E:/xal/config.xml
		try {
			document_adaptor.writeTo( new File("E:/xal/config.xml") );
			System.out.println("xml file created successfully");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("file access error");
			e.printStackTrace();
		} 

	}
	
	

}
