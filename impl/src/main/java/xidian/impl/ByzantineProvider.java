/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package xidian.impl;

import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.OpendaylightDirectStatisticsService;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.echo.service.rev150305.SalEchoService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ByzantineService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.CalTimeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.CalTimeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ControllersInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ControllersTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.FindMaxOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.FindMaxOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.GetVerificationTimeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.GetVerificationTimeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.LogNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ReceiveMessageInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ReceiveMessageOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.ReceiveMessageOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.SendMessageInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.SendMessageOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.SendMessageOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.SendPortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.SendPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.SendPortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.TopologyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.info.Controllers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.info.Controllers.Flag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.info.ControllersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.controllers.time.AllTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.find.max.output.DoneTimes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.find.max.output.DoneTimesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.find.max.output.DoneTimesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.receive.message.input.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.TopologyMessages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.TopologyMessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.TopologyMessagesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.topology.messages.Verifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.topology.messages.VerifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.byzantine.rev150105.topology.message.topology.messages.VerifiersKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import xidian.impl.message.RpcInputMessage;
import xidian.impl.message.SenderMessage;
import xidian.impl.message.SwitchStatus;
import xidian.impl.util.HttpUtils;
import xidian.impl.util.SignAndVerify;
import xidian.impl.util.SwitchesUtil;

public class ByzantineProvider implements ByzantineService {

	private static final Logger LOG = LoggerFactory.getLogger(ByzantineProvider.class);
	public static final String FLOW_ID_PREFIX = "USEC-";
	public static int flowNo = 0;

	private final DataBroker dataBroker;
	private final NotificationPublishService notificationPublishService;
	private final SalEchoService salEchoService;
	private final OpendaylightDirectStatisticsService directStatisticsService;
	private Gson gson;

	public ByzantineProvider(final DataBroker dataBroker, NotificationPublishService notificationPublishService,
			SalEchoService salEchoService, OpendaylightDirectStatisticsService directStatisticsService) {
		this.dataBroker = dataBroker;
		this.notificationPublishService = notificationPublishService;
		this.salEchoService = salEchoService;
		this.directStatisticsService = directStatisticsService;
	}

	/**
	 * Method called when the blueprint container is created.
	 */
	public void init() {
		LOG.info("ByzantineProvider Session Initiated");
		gson = new Gson();
	}

	/**
	 * Method called when the blueprint container is destroyed.
	 */
	public void close() {
		LOG.info("ByzantineProvider Closed");
	}

	// sendmessage 原来用的是rpc现在要改成clusterdatastore

	@Override
	public Future<RpcResult<SendMessageOutput>> sendMessage(SendMessageInput sendMessageInput) {
		// this.executorService = Executors.newFixedThreadPool(30);
		String quorum = sendMessageInput.getQuorum();// 发送给哪个仲裁
		String content = sendMessageInput.getContent();// Json 格式是FlowTable或者TopologyUpdate
		String contentType = sendMessageInput.getContentType();
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		String _ip = null;// 本机ip
		try {
			_ip = HttpUtils.getHostIp();
			LOG.info("The IP the this host" + _ip);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		KeyedInstanceIdentifier<Controllers, ControllersKey> path = InstanceIdentifier.create(ControllersInfo.class)
				.child(Controllers.class, new ControllersKey(_ip));
		String privateKey = null;
		try {
			privateKey = read.read(LogicalDatastoreType.CONFIGURATION, path).get().get().getPrivateKey();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Date time = new Date();// 产生这个消息的时间

		String sig = SignAndVerify.sign(_ip + content + time, privateKey);
		SenderMessage senderMessage = new SenderMessage(_ip, content, time.toString(), sig);
		RpcInputMessage input = new RpcInputMessage(contentType, senderMessage);
		String inputJson = gson.toJson(input);
		// 根据选择的仲裁发送出去
		InstanceIdentifier<ControllersInfo> pathQuo = InstanceIdentifier.create(ControllersInfo.class);
		ControllersInfo controllersToFind = null;
		try {
			controllersToFind = read.read(LogicalDatastoreType.CONFIGURATION, pathQuo).get().get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, String> mapResult = new ConcurrentHashMap<>();

		List<Controllers> allControllers = controllersToFind.getControllers();
		List<String> quorumControllers = new ArrayList<>();
		// 写入datastore来实现消息的传递
		WriteTransaction sendWrite = dataBroker.newWriteOnlyTransaction();
		KeyedInstanceIdentifier<TopologyMessages, TopologyMessagesKey> sendPath = InstanceIdentifier
				.create(TopologyMessage.class).child(TopologyMessages.class, new TopologyMessagesKey(time.getTime()));

		for (Controllers con : allControllers) {
			// 如果属于选择的仲裁的话
			if (con.getBelong().contains(quorum)) {
				quorumControllers.add(con.getIp());
			}
		}

		TopologyMessagesBuilder topologyMessagesBuilder = new TopologyMessagesBuilder();
		topologyMessagesBuilder.setTimeLong(time.getTime());
		topologyMessagesBuilder.setKey(new TopologyMessagesKey(time.getTime()));
		topologyMessagesBuilder.setTime(time.toString());
		topologyMessagesBuilder.setUpdateMessage(inputJson);
		topologyMessagesBuilder.setContentType(contentType);
		List<Verifiers> verifiers = new ArrayList<>();
		VerifiersBuilder verifiersBuilder = new VerifiersBuilder();
		for (String ipToSend : quorumControllers) {
			verifiersBuilder.setDoneTime("0");
			verifiersBuilder.setIp(ipToSend);
			verifiersBuilder.setKey(new VerifiersKey(ipToSend));
			verifiers.add(verifiersBuilder.build());
		}
		topologyMessagesBuilder.setVerifiers(verifiers);
		sendWrite.merge(LogicalDatastoreType.CONFIGURATION, sendPath, topologyMessagesBuilder.build());
		sendWrite.submit();
		return RpcResultBuilder.success(new SendMessageOutputBuilder().setResult("success").build()).buildFuture();
		// int quorumSize = quorumControllers.size();
		// for (String ipToSend : quorumControllers) {
		// Date dateX = new Date();
		// LOG.info("Send Message" + inputJson + ",to:" + ipToSend);
		// String result = HttpUtils.sendHttpPostJson(
		// HttpUtils.getBasicURL(ipToSend, 8181,
		// "/restconf/operations/byzantine:receive-message"), inputJson);
		// String jugement = null;
		// try {
		// jugement =
		// jsonParser.parse(result).getAsJsonObject().get("output").getAsJsonObject().get("result")
		// .getAsString();
		// } catch (Exception json) {
		// jugement = "error";
		// LOG.info("Get Message from:" + ipToSend + " error");
		// }
		// Date dateE = new Date();
		// long oneT = dateE.getTime() - dateX.getTime();
		// mapResult.put(ipToSend, jugement + ":" + oneT);
		// // 多线程访问仲裁中的每一个成员
		// // QuorumThread quorumThread = new QuorumThread(ipToSend, mapResult,
		// inputJson);
		// // executorService.execute(quorumThread);
		// }
		// // executorService.shutdown();
		// // while (!executorService.isTerminated()) {
		// //
		// // }
		//
		// int totalNum = controllersToFind.getTotalNum();
		// int faultyNum = controllersToFind.getTotalFaulty();
		// int correctNum = 0;
		// int falseNum = 0;
		// int noResponse = 0;
		// List<ResultMap> resultMap = new ArrayList<>();
		//
		// for (String ipKey : mapResult.keySet()) {
		//
		// String judge = mapResult.get(ipKey);
		// ResultMapBuilder resultMapBuilder = new ResultMapBuilder();
		// resultMapBuilder.setIp(ipKey);
		// resultMapBuilder.setJudgement(judge);
		// resultMap.add(resultMapBuilder.build());
		//
		// if (judge.contains("true")) {
		// correctNum++;
		// } else if (judge.contains("false")) {
		// falseNum++;
		// } else {
		// noResponse++;
		// }
		// }
		// boolean finalResult = false;
		// if (correctNum >= faultyNum) {
		// finalResult = true;
		// }
		// if (finalResult == true && contentType.equals("flow")) {
		// // Date date1 = new Date();
		// // // 下发流表
		// // // 首先创建流表
		// // FlowTable flowTable = gson.fromJson(content, FlowTable.class);
		// // Flow flow = createProgibitFlow(content);// 下发一个特殊的流表
		// // NodeKey nodeKey = new NodeKey(new NodeId(flowTable.getNodeKey()));//
		// // 看Yang文件NodeKey的变量是NodeId
		// // // 寻找Nodes根节点下的子节点，由NodeKey来寻找Nodes下的子节点
		// // InstanceIdentifier<Node> nodeId =
		// // InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey)
		// // .build();
		// // // 对每个节点下发流表
		// // addProhibitFlow(nodeId, flow);
		// // Date date2 = new Date();
		// // long timeC = date2.getTime() - date1.getTime();
		// // LOG.info("Send Flow time consume:" + timeC);
		// }
		// Date time2 = new Date();
		// long cha = time2.getTime() - time.getTime();
		// SendMessageOutputBuilder messageOutputBuilder = new
		// SendMessageOutputBuilder();
		// messageOutputBuilder.setResult(Boolean.toString(finalResult));
		// messageOutputBuilder.setTime(Long.toString(cha));
		// messageOutputBuilder.setResultMap(resultMap);
		//
		// LOG.info("Total time consume:" + cha);
		// return RpcResultBuilder.success(messageOutputBuilder.build()).buildFuture();
	}

	// private Future<RpcResult<AddFlowOutput>> writeFlow(InstanceIdentifier<Node>
	// nodeInstanceId,
	// InstanceIdentifier<Table> tableInstanceId, InstanceIdentifier<Flow> flowPath,
	// Flow flow) {
	// Date dateKai = new Date();
	// // 创建一个AddflowInputBuilder
	// AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
	// // 指定一个节点
	// builder.setNode(new NodeRef(nodeInstanceId));
	// // flow的路径
	// builder.setFlowRef(new FlowRef(flowPath));
	// // table的路径
	// builder.setFlowTable(new FlowTableRef(tableInstanceId));
	// builder.setTransactionUri(new Uri(flow.getId().getValue()));
	// return salFlowService.addFlow(builder.build());
	// }
	//
	// private InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node>
	// nodeId) {
	//
	// // get flow table key
	// // 获取0号流表
	// short tableId = 0;
	// TableKey flowTableKey = new TableKey(tableId);
	// return nodeId.augmentation(FlowCapableNode.class).child(Table.class,
	// flowTableKey);
	// }
	//
	// private void addProhibitFlow(InstanceIdentifier<Node> nodeId, Flow flow) {
	// // node 是遍历datastore中nodeids中的每一个nodeid
	// LOG.info("Adding prohibit flows for node {} ", nodeId);
	// // 根据nodeId获取tableId
	// InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
	//
	// // 创建一个FlowKey
	// FlowKey flowKey = new FlowKey(new FlowId(FLOW_ID_PREFIX +
	// String.valueOf(flowNo++)));
	//
	// // 在datastore中创建一个子路经
	// InstanceIdentifier<Flow> flowId = tableId.child(Flow.class, flowKey);
	//
	// // 在这个子路经下添加一个流
	// Future<RpcResult<AddFlowOutput>> result = writeFlow(nodeId, tableId, flowId,
	// flow);
	// AddFlowOutput output = null;
	// try {
	// output = result.get().getResult();
	// } catch (InterruptedException | ExecutionException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// LOG.info("Added prohibit flows for node {} ", nodeId);
	// }
	//
	// private Flow createProgibitFlow(String content) {
	//
	// String srcMAC = "FF:FF:FF:FF:FF:FF";
	// String dstMAC = "FF:FF:FF:FF:FF:FF";
	// // 设置名字和tableID以及flowID
	// FlowBuilder builder = new FlowBuilder();
	// builder.setFlowName("prohibitFlow").setTableId(Short.valueOf("0"));
	// builder.setId(new FlowId(Long.toString(builder.hashCode())));
	// // 设置匹配域
	// MatchBuilder matchBuilder = new MatchBuilder();
	// // 以太网的匹配
	// EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder();
	// // 以太网的目的地址
	// EthernetDestinationBuilder ethernetDestinationBuilder = new
	// EthernetDestinationBuilder();
	// ethernetDestinationBuilder.setAddress(new MacAddress(dstMAC));
	// ethernetMatchBuilder.setEthernetDestination(ethernetDestinationBuilder.build());
	// EthernetSourceBuilder ethernetSourceBuilder = new EthernetSourceBuilder();
	// ethernetSourceBuilder.setAddress(new MacAddress(srcMAC));
	// ethernetMatchBuilder.setEthernetSource(ethernetSourceBuilder.build());
	// matchBuilder.setEthernetMatch(ethernetMatchBuilder.build());
	// // 设置匹配域
	// builder.setMatch(matchBuilder.build());
	//
	// // 设置指令
	// InstructionsBuilder instructionsBuilder = new InstructionsBuilder();
	// InstructionBuilder instructionBuilder = new InstructionBuilder();
	// ApplyActionsCaseBuilder actionsCaseBuilder = new ApplyActionsCaseBuilder();
	// ApplyActionsBuilder actionsBuilder = new ApplyActionsBuilder();
	// ActionBuilder actionBuilder = new ActionBuilder();
	// actionBuilder.setAction(new DropActionCaseBuilder().setDropAction(new
	// DropActionBuilder().build()).build());
	// List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>
	// action = new ArrayList<>();
	// action.add(actionBuilder.build());
	// actionsBuilder.setAction(action);
	// actionsCaseBuilder.setApplyActions(actionsBuilder.build());
	// instructionBuilder.setInstruction(actionsCaseBuilder.build());
	// List<Instruction> instructions = new ArrayList<>();
	// instructions.add(instructionBuilder.build());
	// instructionsBuilder.setInstruction(instructions);
	// // 设置指令
	// builder.setInstructions(instructionsBuilder.build());
	// // 设置其他项
	// builder.setPriority(50);
	// builder.setHardTimeout(9999);
	// builder.setIdleTimeout(9999);
	// return builder.build();
	// }

	@Override
	public Future<RpcResult<ReceiveMessageOutput>> receiveMessage(ReceiveMessageInput input) {
		Date dateKai = new Date();
		Message message = input.getMessage();
		String contentType = input.getContentType();
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
			ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
			KeyedInstanceIdentifier<Controllers, ControllersKey> path = InstanceIdentifier.create(ControllersInfo.class)
					.child(Controllers.class, new ControllersKey(fromIP));
			String publicKey = null;
			try {
				publicKey = read.read(LogicalDatastoreType.CONFIGURATION, path).get().get().getPublicKey();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean result = SignAndVerify.verify(fromIP + content + timestamp, sig, publicKey);
			if (!result) {
				return RpcResultBuilder.success(new ReceiveMessageOutputBuilder().setResult("" + result).build())
						.buildFuture();
			}
			// 2. 根据contentType来进行特定的验证
			if (contentType.equals("flow")) {
				// // 检查流表是否正确
				// FlowTable flowTable = gson.fromJson(content, FlowTable.class);
				// // 查看源ip主机连接在交换机的端口和下发的流表的入端口以及目的ip主机连接在交换机的端口和流表的出端口是否一致
				// // 通过读取topo信息吧
				// result = SwitchesUtil.isFlowTableRight(dataBroker, flowTable);
				// Date dateFlow = new Date();
				// long timeConsumeFlow = dateFlow.getTime() - dateKai.getTime();
				// LOG.info("Verification for flow time consume is:" + timeConsumeFlow);
			} else if (contentType.equals("switch")) {
				// 检查发送过来的交换机的状态是否正确
				SwitchStatus switchStatus = gson.fromJson(content, SwitchStatus.class);
				String status = switchStatus.getStatus();
				if (status.equals("down")) {
					// 发送echo查看其是否存活
					NodeKey nodeKey = new NodeKey(new NodeId(switchStatus.getNodeKey()));
					boolean isAlive = SwitchesUtil.isAlive(salEchoService, switchStatus.getNodeKey());
					// boolean isAlive = SwitchesUtil.isExist(dataBroker,
					// switchStatus.getNodeKey());
					result = !isAlive;
					Date dateTopo = new Date();
					long timeConsumeTopo = dateTopo.getTime() - dateKai.getTime();
					LOG.info("Verfification for switch down time consume is:" + timeConsumeTopo);
				} else {
					// 产看拓扑信息看是否确实有这个交换机
					// boolean isExist = SwitchesUtil.isExist(dataBroker,
					// switchStatus.getNodeKey());
					boolean isExist = SwitchesUtil.isAlive(salEchoService, switchStatus.getNodeKey());
					result = isExist;
					Date dateTopo = new Date();
					long timeConsumeTopo = dateTopo.getTime() - dateKai.getTime();
					LOG.info("Verfification for switch add time consume is:" + timeConsumeTopo);
				}
			} else {
				// link
			}
			read.close();
			// 将判断写入日志
			LogNotificationBuilder logNotificationBuilder = new LogNotificationBuilder();
			logNotificationBuilder.setJugement(result);
			logNotificationBuilder.setMessage(content);
			logNotificationBuilder.setTime(timestamp);
			notificationPublishService.offerNotification(logNotificationBuilder.build());
			Date dateJie = new Date();
			return RpcResultBuilder.success(new ReceiveMessageOutputBuilder().setResult("" + result).build())
					.buildFuture();
		case FaultyInactive:
			// 会drop所有的消息
			return RpcResultBuilder.success(new ReceiveMessageOutputBuilder().setResult("No Response").build())
					.buildFuture();
		case FaultyActive:
			if (fromFlag.equals(Flag.Benign)) {
				// 诬陷好的节点
				boolean result2 = false;
				LogNotificationBuilder logNotificationBuilder2 = new LogNotificationBuilder();
				logNotificationBuilder2.setJugement(result2);
				logNotificationBuilder2.setMessage(content);
				logNotificationBuilder2.setTime(timestamp);
				notificationPublishService.offerNotification(logNotificationBuilder2.build());
				return RpcResultBuilder.success(new ReceiveMessageOutputBuilder().setResult("" + result2).build())
						.buildFuture();
			} else if (fromFlag.equals(Flag.FaultyActive)) {
				// 和坏的节点组队
				boolean result3 = true;
				LogNotificationBuilder logNotificationBuilder3 = new LogNotificationBuilder();
				logNotificationBuilder3.setJugement(result3);
				logNotificationBuilder3.setMessage(content);
				logNotificationBuilder3.setTime(timestamp);
				notificationPublishService.offerNotification(logNotificationBuilder3.build());
				return RpcResultBuilder.success(new ReceiveMessageOutputBuilder().setResult("" + result3).build())
						.buildFuture();
			} else {
				// 来的节点是InactiveFaulty
				boolean result3 = true;
				LogNotificationBuilder logNotificationBuilder3 = new LogNotificationBuilder();
				logNotificationBuilder3.setJugement(result3);
				logNotificationBuilder3.setMessage(content);
				logNotificationBuilder3.setTime(timestamp);
				notificationPublishService.offerNotification(logNotificationBuilder3.build());
				return RpcResultBuilder.success(new ReceiveMessageOutputBuilder().setResult("" + result3).build())
						.buildFuture();
			}
		}
		return null;
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
	public Future<RpcResult<CalTimeOutput>> calTime() {
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<ControllersTime> path = InstanceIdentifier.create(ControllersTime.class);
		long max = 0;
		long min = 0;
		try {
			List<AllTime> allTime = read.read(LogicalDatastoreType.CONFIGURATION, path).get().get().getAllTime();
			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
			AllTime el = allTime.get(0);
			min = dateFormat.parse(el.getTime()).getTime();
			max = min;
			for (AllTime allTime2 : allTime) {
				long current = dateFormat.parse(allTime2.getTime()).getTime();
				if (current < min) {
					min = current;
				}
				if (current > max) {
					max = current;
				}
			}
		} catch (InterruptedException | ExecutionException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CalTimeOutputBuilder builder = new CalTimeOutputBuilder();
		builder.setTime(Long.toString(max - min));
		return RpcResultBuilder.success(builder.build()).buildFuture();
	}

	@Override
	public Future<RpcResult<SendPortOutput>> sendPort(SendPortInput input) {
		String node = input.getNode();
		String port = input.getPort();
		NodeKey nodeKey = new NodeKey(new NodeId(node));

		InstanceIdentifier<Node> srcId = InstanceIdentifier.builder(Nodes.class).child(Node.class, nodeKey).build();

		GetNodeConnectorStatisticsInputBuilder builder = new GetNodeConnectorStatisticsInputBuilder();
		builder.setNode(new NodeRef(srcId));
		builder.setNodeConnectorId(new NodeConnectorId(port));
		builder.setStoreStats(true);
		GetNodeConnectorStatisticsOutput result = null;
		try {
			result = directStatisticsService.getNodeConnectorStatistics(builder.build()).get().getResult();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return RpcResultBuilder.success(new SendPortOutputBuilder().setResult(result.toString())).buildFuture();
	}

	@Override
	public Future<RpcResult<FindMaxOutput>> findMax() {
		InstanceIdentifier<TopologyMessage> path = InstanceIdentifier.create(TopologyMessage.class);
		ReadWriteTransaction readWrite = dataBroker.newReadWriteTransaction();
		FindMaxOutputBuilder findMaxOutputBuilder = new FindMaxOutputBuilder();
		DoneTimesBuilder doneTimesBuilder = new DoneTimesBuilder();
		List<DoneTimes> doneTimes = new ArrayList<>();
		try {
			int x = 0;
			List<TopologyMessages> topologyMessages = readWrite.read(LogicalDatastoreType.CONFIGURATION, path).get()
					.get().getTopologyMessages();
			for (TopologyMessages messages : topologyMessages) {
				long consume = -1;
				for (Verifiers verifiers : messages.getVerifiers()) {
					Long con = verifiers.getTotalTime();
					if (con == null) {
						continue;
					}
					if (consume < con) {
						consume = con;
					}
				}
				doneTimesBuilder.setKey(new DoneTimesKey(x));
				doneTimesBuilder.setRound(x++);
				doneTimesBuilder.setExperimentType(messages.getContentType());
				doneTimesBuilder.setTime(Long.toString(consume));// -1说明这个消息的验证者都没对这个消息验证
				doneTimes.add(doneTimesBuilder.build());

				// KeyedInstanceIdentifier<TopologyMessages, TopologyMessagesKey> pathWrite =
				// InstanceIdentifier
				// .create(TopologyMessage.class).child(TopologyMessages.class,
				// messages.getKey());
				// TopologyMessagesBuilder topologyMessagesBuilder = new
				// TopologyMessagesBuilder();
				// topologyMessagesBuilder.setKey(messages.getKey());
				// topologyMessagesBuilder.setConsumeTime(Long.toString(consume));
				// readWrite.merge(LogicalDatastoreType.CONFIGURATION, pathWrite,
				// topologyMessagesBuilder.build());
				// readWrite.submit();

				findMaxOutputBuilder.setDoneTimes(doneTimes);
			}
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return RpcResultBuilder.success(findMaxOutputBuilder.build()).buildFuture();
	}

	@Override
	public Future<RpcResult<GetVerificationTimeOutput>> getVerificationTime() {
		ReadOnlyTransaction read = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<TopologyMessage> path = InstanceIdentifier.create(TopologyMessage.class);
		CheckedFuture<Optional<TopologyMessage>, ReadFailedException> result = read
				.read(LogicalDatastoreType.CONFIGURATION, path);
		List<TopologyMessages> messages = null;
		String link = "";
		String host = "";
		String switches = "";
		try {
			messages = result.get().get().getTopologyMessages();
		    for(TopologyMessages m : messages) {
		    	String type = m.getContentType();
		    	List<Verifiers> verifiers = m.getVerifiers();
		    	if(type.contains("ink")) {
		    		for(Verifiers v: verifiers) {
		    			link+=v.getTimeConsume();
		    			link+=",";
		    		}
		    	}else if(type.contains("ost")) {
		    		for(Verifiers v: verifiers) {
		    			host+=v.getTimeConsume();
		    			host+=",";
		    		}
		    	}else {
		    		for(Verifiers v: verifiers) {
		    			switches+=v.getTimeConsume();
		    			switches+=",";
		    		}
		    	}
		    }
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GetVerificationTimeOutputBuilder builder = new GetVerificationTimeOutputBuilder();
	    builder.setHostTimes(host);
	    builder.setLinkTimes(link);
	    builder.setSwitchTimes(switches);
		return RpcResultBuilder.success(builder.build()).buildFuture();
	}

}