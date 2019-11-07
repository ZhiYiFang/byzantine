/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl.message;

public class RpcInputMessage {

	private String contentType;
	private SenderMessage message;
	public RpcInputMessage(String contentType, SenderMessage message) {
		super();
		this.contentType = contentType;
		this.message = message;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public SenderMessage getMessage() {
		return message;
	}
	public void setMessage(SenderMessage message) {
		this.message = message;
	}
	
}
