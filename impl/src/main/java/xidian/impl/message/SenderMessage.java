/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl.message;

public class SenderMessage {

	private String id;
	private String m;
	private String time;
	private String sig;
	
	public SenderMessage(String id, String m, String time, String sig) {
		super();
		this.id = id;
		this.m = m;
		this.time = time;
		this.sig = sig;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getM() {
		return m;
	}
	public void setM(String m) {
		this.m = m;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getSig() {
		return sig;
	}
	public void setSig(String sig) {
		this.sig = sig;
	}
	
}
