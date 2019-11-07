/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package xidian.impl.message;

public class FlowTable {

	private String srcIP;
	private String dstIP;
	private String inPort;
	private String outPort;
	private String nodeKey;// new NodeKey(nodeKey) 
	
	public FlowTable(String srcIP, String dstIP, String inPort, String outPort, String nodeKey) {
		super();
		this.srcIP = srcIP;
		this.dstIP = dstIP;
		this.inPort = inPort;
		this.outPort = outPort;
		this.nodeKey = nodeKey;
	}
	public String getSrcIP() {
		return srcIP;
	}
	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}
	public String getDstIP() {
		return dstIP;
	}
	public void setDstIP(String dstIP) {
		this.dstIP = dstIP;
	}
	public String getInPort() {
		return inPort;
	}
	public void setInPort(String inPort) {
		this.inPort = inPort;
	}
	public String getOutPort() {
		return outPort;
	}
	public void setOutPort(String outPort) {
		this.outPort = outPort;
	}
	public String getNodeKey() {
		return nodeKey;
	}
	public void setNodeKey(String nodeKey) {
		this.nodeKey = nodeKey;
	}
	
}
