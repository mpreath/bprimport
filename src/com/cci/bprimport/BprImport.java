// BprImport - parses a pipe deliminted file, connects to Cisco BAC
// RDU, and imports the records into the system
// Copyright (C) 2012  Matt Reath

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
package com.cci.bprimport;

import com.cisco.provisioning.cpe.*;
import com.cisco.provisioning.cpe.api.*;
import java.io.*;
import java.util.*;

/**
 * BprImport reads in a | delimineted file that contains information about DOCSIS
 * cable modems that need to be imported into BAC. BprImport initializes a connection
 * to the BAC RDU component, creates a batch, adds the modems to the batch, posts the batch,
 * verifies it was successful, and then closes the connection to the RDU.
 * <p>
 * Usage: java bprimport <rdu address> <port> <username> <password> <file name>
 * <p>
 * @author Matt Reath
 * @version 0.1
 * 
 */
public class BprImport {

	/**
	 * Entry point into the BprImport application.
	 * 
	 * @param args	Contains the CLI arguments
	 */
	public static void main(String[] args) {
		
		BprImport bpr = new BprImport();
		
		// Step 1 - Connect to the RDU
		System.out.print("Connecting to the RDU...");
		bpr.initializeConnectionToRDU(args[1], Integer.parseInt(args[2]), args[3], args[4]);
		System.out.print("Success\n\n");
		
		// Step 2 - Create a batch to use
		//System.out.print("Creating the batch...");
		
		//System.out.print("Success\n\n");
		
		// Step 3 - Loop through devices in file and add each one
		// 12345|1,6,00:11:22:33:44:55|silver|provisioned-docsis
		// ownerID|macAddress|classOfService|dhcp-criteria
		
		System.out.print("Adding devices to the batch");
		try {
			FileReader input = new FileReader(args[5]);
			
			BufferedReader bufRead  = new BufferedReader(input);
			
			String line;
			
			line = bufRead.readLine();
			
			while(line != null) {
				//System.out.println(line);
				
				String temp [] = null;
				
				temp = line.split("\\|");
				
				// temp[0] = Owner ID
				// temp[1] = MAC Address
				// temp[2] = Class of Service
				// temp[3] = DHCP Criteria
				
				/*
				System.out.println("ID: " + temp[0]);
				System.out.println("MAC: " + temp[1]);
				System.out.println("COS: " + temp[2]);
				System.out.println("DHCP:" + temp[3]);
				System.out.println("---------------");
				*/
				bpr.startBatch();
				
				// need to put a flag in here to select either MTA or DOCSIS
				if(args[0].equals("-d")) {
					bpr.addCableModem(temp[0], temp[1], temp[2], temp[3]);
				} else if(args[0].equals("-m")) {
					bpr.addPacketCableMTA(temp[0], temp[1], temp[2], temp[3]);
				}
				
				bpr.postBatch();
				bpr.endBatch();
				//System.out.print(".");
				
				// Get next line
				line = bufRead.readLine();
			}
			
		} catch (FileNotFoundException fnfe) {
			System.out.println(fnfe.getMessage());
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		
		// System.out.print("Success\n");
		// 		
		// 		System.out.print("Sending batch to the RDU (this may take several minutes)...");
		// 		// Step 4 - Post the batch to the RDU
		// 		bpr.postBatch();
		// 		System.out.print("Success\n\n");
		// 		
		// 		System.out.print("Disconnecting...");
		// Step 5 - Verify the batch.
		
		// Step 6 - Disconnect
		bpr.disconnect();
		System.out.print("Success\n");
		
	}
	
	public BprImport() {
		
	}
	
	/**
	 * Initializes a connection to the BAC RDU server.
	 * 
	 * @param hostname	The FQDN or IP of the BAC RDU server
	 * @param port		The port on which to communicate to the server. The default is 49187.
	 * @param userName	User name used to connect to the RDU server
	 * @param password	Password used to connect to the RDU server
	 */
	public void initializeConnectionToRDU(String hostname, int port, String userName, String password) {
		connection = null;
		
		try
		{
			connection =
				PACEConnectionFactory.getInstance(hostname, port, userName, password);
		}
		catch(PACEConnectionException e)
		{
			// Connection failed
			System.out.println(e.getMessage());
			System.exit(0);
		}
		catch(AuthenticationException e)
		{
			// Authentication failure
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Inserts an add cable modem command into the batch.
	 * 
	 * @param ownerID			Customer/Account ID for this cable modem
	 * @param macAddress		MAC address of the cable modem
	 * @param classOfService	The class of service to assign to this modem
	 */
	public void addCableModem(String ownerID, String macAddress, String classOfService, String dhcpCriteria) {
		if(batch != null) {
			
			if(ownerID.equals("null")){
				ownerID = null;
			}
			
			if(classOfService.equals("null")) {
				classOfService = null;
			}
			
			if(dhcpCriteria.equals("null")) {
				dhcpCriteria = null;
			}
				
			
			List devIds = new ArrayList();
			devIds.add(new MACAddress(macAddress));
			batch.add(
					DeviceType.DOCSIS, 
					devIds,
					//macAddress,
					null,
					null,
					ownerID,
					classOfService,
					dhcpCriteria,
					null);
		}
	}
	
	public void addPacketCableMTA(String ownerID, String macAddress, String classOfService, String dhcpCriteria) {
		if(batch != null) {
			
			if(ownerID.equals("null")){
				ownerID = null;
			}
			
			if(classOfService.equals("null")) {
				classOfService = null;
			}
			
			if(dhcpCriteria.equals("null")) {
				dhcpCriteria = null;
			}
			
			String hostname = macAddress.replace(':','-');
			hostname = hostname.replace(',','-');
			
			String domainName = "duovoip.loc";
			
			if(dhcpCriteria != null && dhcpCriteria.equals("provisioned-packet-cable")) {
				dhcpCriteria = "provisioned-packet-cable-mta";
			}
				
			
			List devIds = new ArrayList();
			devIds.add(new MACAddress(macAddress));
			batch.add(
					DeviceType.PACKET_CABLE_MTA, 
					devIds,
					//macAddress,
					hostname,
					domainName,
					ownerID,
					classOfService,
					dhcpCriteria,
					null);
		}
	}
	
	/**
	 * Initializes the batch using the connection object.
	 */
	public void startBatch() {
		if(connection != null) {
			batch = connection.newBatch();
		}
	}
	
	/**
	 * Posts the batch to the RDU server.
	 */
	public void postBatch() {
		status = null;
		
		try {
			status = batch.post();
		}
		catch(ProvisioningException e)
		{
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Verifies the status of the batch.
	 */
	public void endBatch() {
		
		CommandStatus cStatus = null;
		
		if(status.isError()) {
			System.out.println("There were errors during batch processing.");
		
			cStatus = status.getFailedCommandStatus();
			
			if (cStatus != null && cStatus.getErrorMessage() != null) {
				System.out.println(cStatus.getErrorMessage());
			}
			else
			{
				System.out.println(status.getBatchID() + ": " + status.getErrorMessage());
			}
			
		} else {
			System.out.println("Batch was processed successfully.");
		}
	}
	
	/**
	 * Disconnects from the RDU server.
	 */
	public void disconnect() {
		connection.releaseConnection();
	}
	
	
	private PACEConnection connection;
	private Batch batch;
	private BatchStatus status;

}
