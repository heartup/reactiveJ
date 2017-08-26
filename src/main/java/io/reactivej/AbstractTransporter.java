package io.reactivej;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransporter {
	private static Logger logger = LoggerFactory.getLogger(AbstractTransporter.class);

	private ReactiveSystem system;

	public AbstractTransporter(ReactiveSystem system) {
		this.system = system;
	}

	public ReactiveSystem getSystem() {
		return system;
	}

	public abstract void send(String host, int port, Envelope envlop);

	/***
	 *
	 * @param receiver alway remote ref
	 * @param envlop
	 */
	public void sendMessage(ReactiveRef receiver, Envelope envlop) {
		send(receiver.getHost(), receiver.getPort(), envlop);
	}

	/***
	 *
	 * @param receiver always local ref
	 * @param envlop
	 */
	public void receiveMessage(ReactiveRef receiver, Envelope envlop) {
//		if (logger.isDebugEnabled()) {
			// 系统消息暂不打印日志
			if (!(envlop.getMessage() instanceof SystemMessage)
					&& !(envlop.getMessage() instanceof ClusterClient.ClusterMessage)) {
				logger.info("接受消息[" + envlop.toString() + "]");
			}
			else {
				logger.debug("接受消息[" + envlop.toString() + "]");
			}
//		}

		final ReactiveCell compCell = receiver.getCell();
		if (compCell == null) {
			throw new ReactiveException("不存在的组件");
		}

		compCell.getDispatcher().dispatch(compCell, envlop);
	}

	public void receive(Envelope env, Channel channel) {
		ReactiveRef receiver = getSystem().findComponent(env.getReceiver().getHost(), env.getReceiver().getPort(), env.getReceiver().getPath());
		ReactiveRef sender = getSystem().findComponent(env.getSender().getHost(), env.getSender().getPort(), env.getSender().getPath());

		env.setReceiver(receiver);
		env.setSender(sender);
		env.setFromChannel(channel);

		receiveMessage(receiver, env);
	}

	public abstract void suspendReadingMessage();

	public abstract void resumeReadingMessage();
}
