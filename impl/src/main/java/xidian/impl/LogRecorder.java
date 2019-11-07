/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.AssertionsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ByzantineListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.LogNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.assertions.info.ControllerAssertions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.assertions.info.ControllerAssertionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.assertions.info.ControllerAssertionsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.assertions.info.controller.assertions.Assertions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.assertions.info.controller.assertions.AssertionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.assertions.info.controller.assertions.AssertionsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import xidian.impl.util.HttpUtils;

public class LogRecorder implements ByzantineListener {

	private final DataBroker dataBroker;

	public LogRecorder(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}

	@Override
	public void onLogNotification(LogNotification notification) {
		String message = notification.getMessage();
		Boolean jugement = notification.isJugement();
		String timestamp = notification.getTime();
		String sender = notification.getSender();
		WriteTransaction write = dataBroker.newWriteOnlyTransaction();
		String hostIP = null;
		try {
			hostIP = HttpUtils.getHostIp();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		KeyedInstanceIdentifier<ControllerAssertions, ControllerAssertionsKey> pathAssertion = InstanceIdentifier
				.create(AssertionsInfo.class).child(ControllerAssertions.class, new ControllerAssertionsKey(hostIP));
		ControllerAssertionsBuilder controllerAssertionBuilder = new ControllerAssertionsBuilder();
		controllerAssertionBuilder.setIp(hostIP);
		List<Assertions> assertions = new ArrayList<>();
		AssertionsBuilder builder = new AssertionsBuilder();
		builder.setJugement(jugement);
		builder.setSender(sender);
		builder.setMessage(message);
		builder.setTime(timestamp);
		builder.setKey(new AssertionsKey(timestamp));
		assertions.add(builder.build());
		controllerAssertionBuilder.setAssertions(assertions);
		controllerAssertionBuilder.setKey(new ControllerAssertionsKey(hostIP));
		write.merge(LogicalDatastoreType.OPERATIONAL, pathAssertion, controllerAssertionBuilder.build());
		write.submit();
	}

}
