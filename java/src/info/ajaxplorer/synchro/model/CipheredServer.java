package info.ajaxplorer.synchro.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Property;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.PassManager;

public class CipheredServer extends Server {

	public CipheredServer(Node n) throws URISyntaxException{
		super(n);
		try {
			this.setPassword(PassManager.decrypt(this.getPassword()));
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public CipheredServer(String label, String url, String user, String password, boolean trustSSL, boolean legacyServer) throws URISyntaxException{
		super(label, url, user, password, trustSSL, legacyServer);
	}
	
	public Node createDbNode(Dao<Node, String> nodeDao) throws SQLException{
		Node n = new Node(Node.NODE_TYPE_SERVER, this.getLabel(), null);
		nodeDao.create(n);
		n.properties = nodeDao.getEmptyForeignCollection("properties");
		n.addProperty("url", this.getUrl());
		n.addProperty("user", this.getUser());
		try {
			n.addProperty("password", PassManager.encrypt(this.getPassword()));
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		n.addProperty("trust_ssl", Boolean.toString(this.shouldTrustSSL()));
		n.addProperty("legacy_server", Boolean.toString(this.isLegacyServer()));
		this.setServerNode(n);
		return n;		
	}	
	
	public void updateDbNode(Dao<Node, String> nodeDao, Dao<Property, String> propertyDao) throws SQLException{
		Node n = this.getServerNode();
		for(Property p:n.properties){
			if(p.getName().equals("url")) p.setValue(this.getUrl());
			else if(p.getName().equals("user")) p.setValue(this.getUser());
			else if(p.getName().equals("password"))
				try {
					p.setValue(PassManager.encrypt(this.getPassword()));
				} catch (GeneralSecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else if(p.getName().equals("trust_ssl")) p.setValue(Boolean.toString(shouldTrustSSL()));
			else if(p.getName().equals("legacy_server")) p.setValue(Boolean.toString(isLegacyServer()));	
			propertyDao.update(p);
		}
		if(!n.getLabel().equals(this.getLabel())){
			n.setLabel(this.getLabel());
		}
		nodeDao.update(n);
	}		
}
