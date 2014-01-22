package org.openstreetmap.josm.plugins.jppostalcode;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * @author Tsuyoshi Kinoshita
 *
 */
class JPPostalcode2AddressDialog extends JDialog
{
  static private JPPostalcode2AddressDialog instance = null;
  public JLabel jLabel = new JLabel();
  
  public JPPostalcode2AddressDialog()
  {
  	build();
  }
  
  
  static public JPPostalcode2AddressDialog getInstance() {
    if (instance == null) {
        instance = new JPPostalcode2AddressDialog();
    }
    return instance;
  }
  
  protected void build()
  {
  	getContentPane().setLayout(new BorderLayout());
  	getContentPane().add(jLabel);
  	getContentPane().setBounds(new Rectangle(100, 100));
  }
}

class LaunchAction extends JosmAction
{
	public LaunchAction()
	{
		super("郵便番号から住所追加", null, null, null, false);
	}
	
	@Override
	public void actionPerformed(ActionEvent event)
	{
		//KanaPostDialog dialog = KanaPostDialog.getInstance();
		
		DataSet dataSet = Main.main.getCurrentDataSet();
		if (dataSet != null)
		{
			for (OsmPrimitive osm : dataSet.getSelected())
			{
				String postalCode = osm.getKeys().get("addr:postcode");
				
				if (postalCode != null)
				{
					postalCode = postalCode.replaceAll("-","");
					//dialog.jLabel.setText(dialog.jLabel.getText() + "\n" + osm.getKeys().get("postal_code"));
					
					try {
						URLConnection conn = new URL("http://zip.cgis.biz/xml/zip.php?zn=" + postalCode).openConnection();
						DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						Document doc = docBuilder.parse(conn.getInputStream());
						NodeList nodeList = doc.getElementsByTagName("ADDRESS_value").item(0).getChildNodes();
						String state = null, city = null, address = null; 
						
						for (int i=0; i<nodeList.getLength(); i++) 
						{
							Node node = nodeList.item(i);
							if (node.getNodeName().equals("value"))
							{
								NamedNodeMap nnm = node.getAttributes();
								if (nnm.getNamedItem("state") != null)
								{
									state = nnm.getNamedItem("state").getNodeValue();
								}
								if (nnm.getNamedItem("city") != null)
								{
									city = nnm.getNamedItem("city").getNodeValue();
								}
								if (nnm.getNamedItem("address") != null)
								{
									address = nnm.getNamedItem("address").getNodeValue();
								}
							}
						}
						
						StringBuilder full = new StringBuilder();
						
						osm.put("addr:country", "JP");
						
						if (state != null)
						{
							osm.put("addr:province", state);
							full.append(state);
						}
						if (city != null)
						{
							osm.put("addr:city", city);
							full.append(city);
						}
						if (address != null)
						{
							osm.put("addr:quarter", address);
							full.append(address);
						}
						
						osm.put("addr:full", full.toString());

						//dialog.jLabel.setText(dialog.jLabel.getText() + "\n" + " OK ");
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		//dialog.setVisible(true);
		//dialog.setSize(200, 200);
	}
}

public class JPPostalcode2AddressPlugin extends Plugin
{
	LaunchAction action;
	
	public JPPostalcode2AddressPlugin(PluginInformation info)
	{
		super(info);
		action = new LaunchAction();
		MainMenu.add(Main.main.menu.dataMenu, action);
	}
}
