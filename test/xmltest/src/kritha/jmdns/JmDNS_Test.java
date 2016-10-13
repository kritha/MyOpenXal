package kritha.jmdns;
import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.*;
public class JmDNS_Test implements ServiceListener, ServiceTypeListener {
	public static void main(String[] args) {
        try {
        	JmDNS_Test ls = new JmDNS_Test();
			
			String serviceType = null;
			
			if( args.length == 1 ) {
				serviceType = args[ 0 ];
			} else {
				//serviceType = "_http._tcp.local.";
				
				serviceType = "_xal_extension_application_applicationstatus._tcp.local.";
				//serviceType = "_services._dns-sd._udp.local.";
			}
			
			JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
			jmdns.addServiceTypeListener( ls );
			jmdns.addServiceListener(serviceType,ls);
			while (true) {
				ServiceInfo[] infos = jmdns.list( serviceType );
				
				try {
					for( int i = 0; i < infos.length; i++ ) {
						//System.out.println("SERVICE: " + infos[i].getName()+" Type: "+infos[i].getType()+" Host: "+infos[i].getHostAddresses()+" port: "+infos[i].getPort());
						System.out.println("SERVICE: " + infos[i]);
					}
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					break;
				}
			}
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

	@Override
	public void serviceTypeAdded(ServiceEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("serviceTypeAdded: " + arg0 );
	}

	@Override
	public void subTypeForServiceTypeAdded(ServiceEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("subTypeForServiceTypeAdded: " + arg0 );
	}

	@Override
	public void serviceAdded(ServiceEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("serviceAdded: " + arg0 );
	}

	@Override
	public void serviceRemoved(ServiceEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("serviceRemoved: " + arg0 );
	}

	@Override
	public void serviceResolved(ServiceEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("serviceResolved: " + arg0 );
	}
}
