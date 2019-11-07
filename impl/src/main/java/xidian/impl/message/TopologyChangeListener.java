/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl.message;

import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ControllersTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.time.AllTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.time.AllTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.time.AllTimeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xidian.impl.util.HttpUtils;

public class TopologyChangeListener implements ClusteredDataTreeChangeListener<Nodes> {

	private Logger LOG = LoggerFactory.getLogger(TopologyChangeListener.class);
	private final DataBroker dataBroker;
	private String thisIP = "";
	private int initialSize = 0;
	private int iThE = 1;// 第几次实验
	ListenerRegistration<TopologyChangeListener> listenerReg;

	public TopologyChangeListener(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}

	public void init() {
		InstanceIdentifier<Nodes> id = InstanceIdentifier.create(Nodes.class);
		listenerReg = dataBroker
				.registerDataTreeChangeListener(new DataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, id), this);
		try {
			thisIP = HttpUtils.getHostIp();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close() {
		listenerReg.close();
	}

	@Override
	public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Nodes>> collection) {
		for (final DataTreeModification<Nodes> change : collection) {
			final DataObjectModification<Nodes> rootNode = change.getRootNode();
			switch (rootNode.getModificationType()) {
			case SUBTREE_MODIFIED:
				Date date = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
				int changedSize = rootNode.getDataAfter().getNode().size();
				if (changedSize != initialSize) {
					WriteTransaction write = dataBroker.newWriteOnlyTransaction();
					KeyedInstanceIdentifier<AllTime, AllTimeKey> path = InstanceIdentifier.create(ControllersTime.class)
							.child(AllTime.class, new AllTimeKey(thisIP));
					AllTimeBuilder allTimeBuilder = new AllTimeBuilder();
					allTimeBuilder.setIp(thisIP);
					allTimeBuilder.setIth(Integer.toString(iThE++));
					allTimeBuilder.setKey(new AllTimeKey(thisIP));
					allTimeBuilder.setTime(dateFormat.format(date));
					write.put(LogicalDatastoreType.CONFIGURATION, path, allTimeBuilder.build());
					write.submit();
					LOG.info("Nodes Size changed:" + dateFormat.format(date));
					initialSize = changedSize;
				}
				break;
			case WRITE:
				// if(rootNode.getDataBefore() == null) {
				// Date date2 = new Date();
				// DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss.SSS");
				// String time2 = dateFormat2.format(date2);
				// LOG.info("IP {} controller remove {} and time is{}",
				// thisIP,rootNode.getDataAfter(),time2);
				// LOG.debug("node {} deleted", rootNode.getIdentifier());
				// }
				break;
			case DELETE:
				// Date date3 = new Date();
				// DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
				// String time = dateFormat.format(date3);
				// LOG.info("IP {} controller remove {} and time is{}",
				// thisIP,rootNode.getDataAfter(),time);
				// LOG.debug("node {} deleted", rootNode.getIdentifier());
				break;
			}
		}
	}

}
