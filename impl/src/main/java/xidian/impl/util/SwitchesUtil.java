/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl.util;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.OpendaylightDirectStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SalEchoService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SendEchoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SendEchoOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;

import xidian.impl.message.FlowTable;
import xidian.impl.message.HostMessage;
import xidian.impl.message.LinkMessage;

public class SwitchesUtil {

	public static boolean isRemoveHostLegal(DataBroker dataBroker,
			OpendaylightDirectStatisticsService directStatisticsService, HostMessage hostMessage) {
		// 原来那里有电脑
		String nodeKey = hostMessage.getNode();
		String connector = hostMessage.getConnector();
		KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> pathIn = InstanceIdentifier.create(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId(nodeKey)))
				.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(connector)));
		ReadTransaction read = dataBroker.newReadOnlyTransaction();
		try {
			// 没有的
			String inIp = read.read(LogicalDatastoreType.OPERATIONAL, pathIn).get().get()
					.getAugmentation(AddressCapableNodeConnector.class).getAddresses().get(0).getIp().getIpv4Address()
					.getValue();
		} catch (Exception e) {
		}

		// 现在的状态是down的
		GetNodeConnectorStatisticsInputBuilder builder = new GetNodeConnectorStatisticsInputBuilder();
		NodeKey srcKey = new NodeKey(new NodeId(nodeKey));
		InstanceIdentifier<Node> srcId = InstanceIdentifier.builder(Nodes.class).child(Node.class, srcKey).build();
		builder.setNode(new NodeRef(srcId));
		builder.setNodeConnectorId(new NodeConnectorId(connector));
		builder.setStoreStats(true);
		try {
			GetNodeConnectorStatisticsOutput result = directStatisticsService
					.getNodeConnectorStatistics(builder.build()).get().getResult();
			int receivedPacket = result.getNodeConnectorStatisticsAndPortNumberMap().get(0).getPackets().getReceived()
					.intValue();
			if (receivedPacket <= 0) {
				return false;
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public static boolean isAddHostLegal(DataBroker dataBroker, SalEchoService salEchoService,
			OpendaylightDirectStatisticsService directStatisticsService, HostMessage hostMessage) {
		String nodeKey = hostMessage.getNode();
		String connector = hostMessage.getConnector();
		KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> pathIn = InstanceIdentifier.create(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId(nodeKey)))
				.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(connector)));
		ReadTransaction read = dataBroker.newReadOnlyTransaction();

		// 原来的里面没有
		try {
			List<Addresses> test = read.read(LogicalDatastoreType.OPERATIONAL, pathIn).get().get()
					.getAugmentation(AddressCapableNodeConnector.class).getAddresses();
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}

		// 那个位置的端口状态up且received包的数量>0
		GetNodeConnectorStatisticsInputBuilder builder = new GetNodeConnectorStatisticsInputBuilder();
		NodeKey srcKey = new NodeKey(new NodeId(nodeKey));
		InstanceIdentifier<Node> srcId = InstanceIdentifier.builder(Nodes.class).child(Node.class, srcKey).build();
		builder.setNode(new NodeRef(srcId));
		builder.setNodeConnectorId(new NodeConnectorId(connector));
		builder.setStoreStats(true);
		try {
			GetNodeConnectorStatisticsOutput result = directStatisticsService
					.getNodeConnectorStatistics(builder.build()).get().getResult();
			int receivedPacket = result.getNodeConnectorStatisticsAndPortNumberMap().get(0).getPackets().getReceived()
					.intValue();
			if (receivedPacket <= 0) {
				return false;
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return false;
		}
		// echo这个位置的主机没有反应
		SendEchoInputBuilder echoInputBuilder = new SendEchoInputBuilder();
		byte[] datas = { 1, 2 };
		echoInputBuilder.setData(datas);
		echoInputBuilder.setNode(new NodeRef(srcId));
		try {
			salEchoService.sendEcho(echoInputBuilder.build()).get().getResult();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean isRemoveLinkLegal(DataBroker dataBroker,
			OpendaylightDirectStatisticsService directStatisticsService, LinkMessage link) {
		String srcPort = link.getSrcSwitchPort();
		String dstPort = link.getDstSwitchPort();
		// 有这样的link(但其实是有的)
		boolean isExistLink = false;
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
		NetworkTopology networkTopology = null;
		try {
			Optional<NetworkTopology> op = read.read(LogicalDatastoreType.OPERATIONAL, path).get();
			if (!op.isPresent()) {
				return false;
			}
			networkTopology = op.get();
		} catch (InterruptedException | ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		List<Link> links = networkTopology.getTopology().get(0).getLink();
		for (Link l : links) {
			String source = l.getSource().getSourceTp().getValue();
			String destination = l.getDestination().getDestTp().getValue();
			if (srcPort.equals(source) && dstPort.equals(dstPort)) {
				isExistLink = true;
				break;
			}
		}
		if (!isExistLink) {
			return false;
		}
		// 查看两个端口状态是否是down, 仅模拟发送行为
		String srcNode = link.getSrcNodeKey();
		String dstNode = link.getDstNodeKey();
		GetNodeConnectorStatisticsInputBuilder builder = new GetNodeConnectorStatisticsInputBuilder();

		NodeKey srcKey = new NodeKey(new NodeId(srcNode));
		NodeKey dstKey = new NodeKey(new NodeId(dstNode));

		InstanceIdentifier<Node> srcId = InstanceIdentifier.builder(Nodes.class).child(Node.class, srcKey).build();
		InstanceIdentifier<Node> dstId = InstanceIdentifier.builder(Nodes.class).child(Node.class, dstKey).build();
		builder.setNode(new NodeRef(srcId));
		builder.setNodeConnectorId(new NodeConnectorId(srcPort));
		builder.setStoreStats(true);
		try {
			GetNodeConnectorStatisticsOutput result1 = directStatisticsService
					.getNodeConnectorStatistics(builder.build()).get().getResult();
			builder.setNode(new NodeRef(dstId));
			builder.setNodeConnectorId(new NodeConnectorId(dstPort));
			builder.setStoreStats(true);
			GetNodeConnectorStatisticsOutput result2 = directStatisticsService
					.getNodeConnectorStatistics(builder.build()).get().getResult();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean isAddLinkLegal(DataBroker dataBroker,
			OpendaylightDirectStatisticsService directStatisticsService, LinkMessage link) {

		String srcNodeKey = link.getSrcNodeKey();
		String dstNodeKey = link.getDstNodeKey();

		String srcPort = link.getSrcSwitchPort();
		String dstPort = link.getDstSwitchPort();

		NodeKey srcKey = new NodeKey(new NodeId(srcNodeKey));
		NodeKey dstKey = new NodeKey(new NodeId(dstNodeKey));

		InstanceIdentifier<Node> srcId = InstanceIdentifier.builder(Nodes.class).child(Node.class, srcKey).build();
		InstanceIdentifier<Node> dstId = InstanceIdentifier.builder(Nodes.class).child(Node.class, dstKey).build();

		// 没有这样的link(但其实是有的)
		boolean isExistLink = false;
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		
		InstanceIdentifier<NetworkTopology> path = InstanceIdentifier.create(NetworkTopology.class);
		NetworkTopology networkTopology = null;
		try {
			Optional<NetworkTopology> op = read.read(LogicalDatastoreType.OPERATIONAL, path).get();
			if (!op.isPresent()) {
				return false;
			}
			networkTopology = op.get();
		} catch (InterruptedException | ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		List<Link> links = networkTopology.getTopology().get(0).getLink();

		for (Link l : links) {
			String source = l.getSource().getSourceTp().getValue();
			String destination = l.getDestination().getDestTp().getValue();
			if (srcPort.equals(source) && dstPort.equals(dstPort)) {
				isExistLink = true;
				break;
			}
		}
		// 发包=收包且都是up
		GetNodeConnectorStatisticsInputBuilder inputBuilder = new GetNodeConnectorStatisticsInputBuilder();
		inputBuilder.setNode(new NodeRef(srcId));
		inputBuilder.setNodeConnectorId(new NodeConnectorId(srcPort));
		Future<RpcResult<GetNodeConnectorStatisticsOutput>> srcPortStatusReply = directStatisticsService
				.getNodeConnectorStatistics(inputBuilder.build());
		Future<RpcResult<GetNodeConnectorStatisticsOutput>> dstPortStatusReply = directStatisticsService
				.getNodeConnectorStatistics(inputBuilder.build());

		try {
			GetNodeConnectorStatisticsOutput srcPortStatus = srcPortStatusReply.get().getResult();
			GetNodeConnectorStatisticsOutput dstPortStatus = dstPortStatusReply.get().getResult();

			BigInteger transmitted = srcPortStatus.getNodeConnectorStatisticsAndPortNumberMap().get(0).getPackets()
					.getTransmitted();// 一方的发包数
			BigInteger reiceived = dstPortStatus.getNodeConnectorStatisticsAndPortNumberMap().get(0).getPackets()
					.getReceived();// 一方的收包数
			return transmitted.equals(reiceived);

		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}


	public static boolean isAlive(SalEchoService salEchoService, String nodeKeyString) {
		SendEchoInputBuilder echoInputBuilder = new SendEchoInputBuilder();
		NodeKey nodeKey = new NodeKey(new NodeId(nodeKeyString));
		InstanceIdentifier<Node> nodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).build();
		echoInputBuilder.setNode(new NodeRef(nodeId));
		byte[] bytesSend = { 1, 2 };
		echoInputBuilder.setData(bytesSend);
		Future<RpcResult<SendEchoOutput>> echoResult = salEchoService.sendEcho(echoInputBuilder.build());
		SendEchoOutput isExist = null;
		try {
			isExist = echoResult.get().getResult();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		byte[] bytesReturn = isExist.getData();
		boolean isAlive = isBytesEqual(bytesSend, bytesReturn);
		return isAlive;
	}

	private static boolean isBytesEqual(byte[] bytes1, byte[] bytes2) {
		if (bytes1.length != bytes2.length) {
			return false;
		}
		for (int i = 0; i < bytes1.length; i++) {
			if (bytes1[i] != bytes2[i]) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public static boolean isFlowTableRight(DataBroker dataBroker, FlowTable flowTable) {
		String nodeKey = flowTable.getNodeKey();
		String srcIP = flowTable.getSrcIP();
		String dstIP = flowTable.getDstIP();
		String inPort = flowTable.getInPort();
		String outPort = flowTable.getOutPort();
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		// 获取特定的switch
		KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> pathIn = InstanceIdentifier.create(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId(nodeKey)))
				.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeKey + ":" + inPort)));
		KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> pathOut = InstanceIdentifier.create(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId(nodeKey)))
				.child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeKey + ":" + outPort)));

		try {
			String inIp = read.read(LogicalDatastoreType.OPERATIONAL, pathIn).get().get()
					.getAugmentation(AddressCapableNodeConnector.class).getAddresses().get(0).getIp().getIpv4Address()
					.getValue();
			if (!inIp.equals(srcIP)) {
				return false;
			}
			String outIp = read.read(LogicalDatastoreType.OPERATIONAL, pathOut).get().get()
					.getAugmentation(AddressCapableNodeConnector.class).getAddresses().get(0).getIp().getIpv4Address()
					.getValue();
			if (!outIp.equals(dstIP)) {
				return false;
			}

		} catch (InterruptedException | ExecutionException e) {
			return false;
			// e.printStackTrace();
		}
		return true;
	}

	public static boolean isExist(DataBroker dataBroker, String nodeKeyString) {
		NodeKey nodeKey = new NodeKey(new NodeId(nodeKeyString));
		List<Node> nodes = getAllNodes(dataBroker);
		for (Node node : nodes) {
			if (nodeKey.equals(node.getKey())) {
				return true;
			}
		}
		return false;
	}

	public static List<Node> getAllNodes(DataBroker dataBroker) {

		// 读取inventory数据库
		InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier
				.<Nodes>builder(Nodes.class);
		// 两种构建instanceIdentifier的方式
		// InstanceIdentifier<Nodes> nodesInsIdBuilder = InstanceIdentifier.
		// create(Nodes.class);

		// 所有节点信息
		Nodes nodes = null;
		// 创建读事务
		try (ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction()) {

			Optional<Nodes> dataObjectOptional = readOnlyTransaction
					.read(LogicalDatastoreType.OPERATIONAL, nodesInsIdBuilder.build()).get();
			// 如果数据不为空，获取到nodes
			if (dataObjectOptional.isPresent()) {
				nodes = dataObjectOptional.get();
			}
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
		}

		return nodes.getNode();
	}
}
