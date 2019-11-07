/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl.message;

public class LinkMessage {

	private String srcNodeKey;
	private String dstNodeKey;
	private String srcSwitchPort;
	private String dstSwitchPort;
	public String getSrcNodeKey() {
		return srcNodeKey;
	}
	public void setSrcNodeKey(String srcNodeKey) {
		this.srcNodeKey = srcNodeKey;
	}
	public String getDstNodeKey() {
		return dstNodeKey;
	}
	public void setDstNodeKey(String dstNodeKey) {
		this.dstNodeKey = dstNodeKey;
	}
	public String getSrcSwitchPort() {
		return srcSwitchPort;
	}
	public void setSrcSwitchPort(String srcSwitchPort) {
		this.srcSwitchPort = srcSwitchPort;
	}
	public String getDstSwitchPort() {
		return dstSwitchPort;
	}
	public void setDstSwitchPort(String dstSwitchPort) {
		this.dstSwitchPort = dstSwitchPort;
	}
	
}
