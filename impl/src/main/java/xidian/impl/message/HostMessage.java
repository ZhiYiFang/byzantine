/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package xidian.impl.message;

public class HostMessage {

	private String ip;
	private String node;
	private String connector;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getNode() {
		return node;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public String getConnector() {
		return connector;
	}
	public void setConnector(String connector) {
		this.connector = connector;
	}
	public HostMessage(String ip, String node, String connector) {
		super();
		this.ip = ip;
		this.node = node;
		this.connector = connector;
	}
	
}
