/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl;

import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.OpendaylightDirectStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SalEchoService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ControllersInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.LogNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.TopologyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.info.Controllers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.info.Controllers.Flag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.info.ControllersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.TopologyMessages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.topology.messages.Verifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.topology.messages.VerifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.topology.messages.VerifiersKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import xidian.impl.message.HostMessage;
import xidian.impl.message.LinkMessage;
import xidian.impl.message.RpcInputMessage;
import xidian.impl.message.SenderMessage;
import xidian.impl.message.SwitchStatus;
import xidian.impl.message.TopologyChangeListener;
import xidian.impl.util.HttpUtils;
import xidian.impl.util.SignAndVerify;
import xidian.impl.util.SwitchesUtil;

public class VerifyListener implements ClusteredDataTreeChangeListener<TopologyMessage> {

	private Logger LOG = LoggerFactory.getLogger(TopologyChangeListener.class);
	private final DataBroker dataBroker;
	private final SalEchoService salEchoService;
	private final NotificationPublishService notificationPublishService;
	private final OpendaylightDirectStatisticsService opendaylightDirectStatisticsService;
	private String thisIP = "";
	private Gson gson = new Gson();
	ListenerRegistration<TopologyChangeListener> listenerReg;

	public VerifyListener(DataBroker dataBroker, SalEchoService salEchoService,
			NotificationPublishService notificationPublishService,
			OpendaylightDirectStatisticsService opendaylightDirectStatisticsService) {
		this.dataBroker = dataBroker;
		this.salEchoService = salEchoService;
		this.notificationPublishService = notificationPublishService;
		this.opendaylightDirectStatisticsService = opendaylightDirectStatisticsService;
	}

	public void init() {
		InstanceIdentifier<TopologyMessage> id = InstanceIdentifier.create(TopologyMessage.class);
		listenerReg = dataBroker
				.registerDataTreeChangeListener(new DataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, id), this);
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

	private Flag getControllerFlag(DataBroker dataBroker, String ip) {
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		KeyedInstanceIdentifier<Controllers, ControllersKey> path = InstanceIdentifier.create(ControllersInfo.class)
				.child(Controllers.class, new ControllersKey(ip));
		Flag flag = null;
		try {
			flag = read.read(LogicalDatastoreType.CONFIGURATION, path).get().get().getFlag();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<TopologyMessage>> changes) {
		for (final DataTreeModification<TopologyMessage> change : changes) {
			final DataObjectModification<TopologyMessage> rootNode = change.getRootNode();
			switch (rootNode.getModificationType()) {
			case SUBTREE_MODIFIED:
				List<TopologyMessages> topologyMessages = rootNode.getDataAfter().getTopologyMessages();
				List<TopologyMessages> topologyMessagesB = rootNode.getDataBefore().getTopologyMessages();
				if(topologyMessagesB.size() == topologyMessages.size()) {
					return;
				}
				// 获取最新的需要验证的消息，并获取验证者
				Collections.sort(topologyMessages, new Comparator<TopologyMessages>() {
					@Override
					public int compare(TopologyMessages o1, TopologyMessages o2) {
						long o1T = o1.getKey().getTimeLong();
						long o2T = o2.getKey().getTimeLong();
						if(o2T-o1T<0) {
							return 1;
						}else if(o2T-o1T>0) {
							return -1;
						}else {
							return 0;
						}
					}
				});
				int newMessageIndex = topologyMessages.size() - 1;
				TopologyMessages newMessage = topologyMessages.get(newMessageIndex);
				SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
				List<Verifiers> verifiers = newMessage.getVerifiers();
				
				for (Verifiers verifier : verifiers) {
					// 遍历验证者如果发现了与自己的ip一致,就进行验证
					if (thisIP.equals(verifier.getIp())) {
						WriteTransaction writeResult = dataBroker.newWriteOnlyTransaction();

						KeyedInstanceIdentifier<Verifiers, VerifiersKey> pathToWrite = InstanceIdentifier
								.create(TopologyMessage.class).child(TopologyMessages.class, newMessage.getKey())
								.child(Verifiers.class, new VerifiersKey(thisIP));// 填写验证结果的路径

						// 填写的内容
						VerifiersBuilder verifiersBuilder = new VerifiersBuilder();

						verifiersBuilder.setIp(thisIP);
						verifiersBuilder.setKey(new VerifiersKey(thisIP));

						String inputJson = newMessage.getUpdateMessage();// 获取这个消息的内容
						RpcInputMessage inputMessage = gson.fromJson(inputJson, RpcInputMessage.class);
						String contentType = inputMessage.getContentType();
						SenderMessage message = inputMessage.getMessage();
						String fromIP = message.getId();
						String content = message.getM();// content是flowtable或者是topology
						String timestamp = message.getTime();
						String sig = message.getSig();

						LOG.info("Receive message from:" + fromIP);
						Flag fromFlag = getControllerFlag(dataBroker, fromIP);
						String thisIP = null;
						try {
							thisIP = HttpUtils.getHostIp();
						} catch (SocketException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Flag thisFlag = getControllerFlag(dataBroker, thisIP);
						LOG.info("This controller's flag is:" + thisFlag);
						LOG.info("From controller's flag is:" + fromFlag);
						
						switch (thisFlag) {
						case Benign:
							// 1. 读取公钥验证签名
							Date dateBegin = new Date();// 单纯验证的开始时间
							Date doneTime = null;// 单纯验证的结束时间
							
							ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
							KeyedInstanceIdentifier<Controllers, ControllersKey> path = InstanceIdentifier
									.create(ControllersInfo.class).child(Controllers.class, new ControllersKey(fromIP));
							String publicKey = null;
							try {
								publicKey = read.read(LogicalDatastoreType.CONFIGURATION, path).get().get()
										.getPublicKey();
							} catch (InterruptedException | ExecutionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							// 验证一下签名
							boolean result = SignAndVerify.verify(fromIP + content + timestamp, sig, publicKey);
							
//							if (!result) {
//								verifiersBuilder.setResult(false);
//								verifiersBuilder.setDoneTime(doneTime.toString());
//								doneTime = new Date();
//								verifiersBuilder.setTimeConsume(doneTime.getTime() - dateBegin.getTime());
//								writeResult.merge(LogicalDatastoreType.CONFIGURATION, pathToWrite,
//										verifiersBuilder.build());
//								writeResult.submit();
//								return;
//							}
							
							
							// 2. 根据contentType来进行特定的验证
							if (contentType.equals("flow")) {
								// nothing
							} else if (contentType.equals("switch")) {
								// 检查发送过来的交换机的状态是否正确
								SwitchStatus switchStatus = gson.fromJson(content, SwitchStatus.class);
								String status = switchStatus.getStatus();
								if (status.equals("down")) {
									// 发送echo查看其是否存活
									NodeKey nodeKey = new NodeKey(new NodeId(switchStatus.getNodeKey()));
									boolean isAlive = SwitchesUtil.isAlive(salEchoService, switchStatus.getNodeKey());
									result = !isAlive;
									
								} else {
									// 产看拓扑信息看是否确实有这个交换机
									boolean isExist = SwitchesUtil.isAlive(salEchoService, switchStatus.getNodeKey());
									result = isExist;
								}
								
							} else if (contentType.equals("addLink")) {
								// link
								LinkMessage linkMessage = gson.fromJson(content, LinkMessage.class);
								result = SwitchesUtil.isAddLinkLegal(dataBroker,
										this.opendaylightDirectStatisticsService, linkMessage);
								
							} else if (contentType.equals("removeLink")) {
								LinkMessage linkMessage = gson.fromJson(content, LinkMessage.class);
								result = SwitchesUtil.isRemoveLinkLegal(dataBroker,
										this.opendaylightDirectStatisticsService, linkMessage);
								
							} else if (contentType.equals("addHost")) {
								HostMessage hostMessage = gson.fromJson(content, HostMessage.class);
								result = SwitchesUtil.isAddHostLegal(dataBroker, salEchoService,
										opendaylightDirectStatisticsService, hostMessage);
								
							} else if (contentType.equals("removeHost")) {
								HostMessage hostMessage = gson.fromJson(content, HostMessage.class);
								result = SwitchesUtil.isRemoveHostLegal(dataBroker, opendaylightDirectStatisticsService,
										hostMessage);
								
							} else {
								// nothing
							}
							read.close();
							
							// 将判断写入日志
							LogNotificationBuilder logNotificationBuilder = new LogNotificationBuilder();
							logNotificationBuilder.setJugement(result);
							logNotificationBuilder.setMessage(content);
							logNotificationBuilder.setTime(timestamp);
							notificationPublishService.offerNotification(logNotificationBuilder.build());
							
							// 将结果写入到verifiers列表的后边
							doneTime = new Date();
							
							verifiersBuilder.setResult(result);
							verifiersBuilder.setDoneTime(doneTime.toString());
							
							verifiersBuilder.setTimeConsume(doneTime.getTime() - dateBegin.getTime());//单纯验证的时间消耗
							verifiersBuilder.setTotalTime(doneTime.getTime() - newMessage.getTimeLong());// 从消息产生到完成认证的时间
							
							writeResult.merge(LogicalDatastoreType.CONFIGURATION, pathToWrite,
									verifiersBuilder.build());
							writeResult.submit();
							return;
							
							
						case FaultyInactive:
							// 会drop所有的消息
							return;
							
							
						case FaultyActive:
							
							dateBegin = new Date();// 单纯验证的开始时间
							doneTime = null;// 单纯验证的结束时间
							
							if (fromFlag.equals(Flag.Benign)) {
								// 诬陷好的节点
								boolean result2 = false;
								LogNotificationBuilder logNotificationBuilder2 = new LogNotificationBuilder();
								logNotificationBuilder2.setJugement(result2);
								logNotificationBuilder2.setMessage(content);
								logNotificationBuilder2.setTime(timestamp);
								notificationPublishService.offerNotification(logNotificationBuilder2.build());

								verifiersBuilder.setResult(false);
								verifiersBuilder.setDoneTime(doneTime.toString());
								doneTime = new Date();
								verifiersBuilder.setTimeConsume(doneTime.getTime() - dateBegin.getTime());
								writeResult.merge(LogicalDatastoreType.CONFIGURATION, pathToWrite,
										verifiersBuilder.build());
								writeResult.submit();
								return;

							} else if (fromFlag.equals(Flag.FaultyActive)) {
								// 和坏的节点组队
								boolean result3 = true;
								LogNotificationBuilder logNotificationBuilder3 = new LogNotificationBuilder();
								logNotificationBuilder3.setJugement(result3);
								logNotificationBuilder3.setMessage(content);
								logNotificationBuilder3.setTime(timestamp);
								notificationPublishService.offerNotification(logNotificationBuilder3.build());

								verifiersBuilder.setResult(true);
								verifiersBuilder.setDoneTime(doneTime.toString());
								doneTime = new Date();
								verifiersBuilder.setTimeConsume(doneTime.getTime() - dateBegin.getTime());
								writeResult.merge(LogicalDatastoreType.CONFIGURATION, pathToWrite,
										verifiersBuilder.build());
								writeResult.submit();
								return;

							} else {
								// 来的节点是InactiveFaulty
								boolean result3 = true;
								LogNotificationBuilder logNotificationBuilder3 = new LogNotificationBuilder();
								logNotificationBuilder3.setJugement(result3);
								logNotificationBuilder3.setMessage(content);
								logNotificationBuilder3.setTime(timestamp);
								notificationPublishService.offerNotification(logNotificationBuilder3.build());

								verifiersBuilder.setResult(true);
								verifiersBuilder.setDoneTime(doneTime.toString());
								doneTime = new Date();
								verifiersBuilder.setTimeConsume(doneTime.getTime() - dateBegin.getTime());
								writeResult.merge(LogicalDatastoreType.CONFIGURATION, pathToWrite,
										verifiersBuilder.build());
								writeResult.submit();
								return;
							}
						}
					}
				}
				break;
			case WRITE:

				break;
			case DELETE:

				break;
			}
		}
	}

}
