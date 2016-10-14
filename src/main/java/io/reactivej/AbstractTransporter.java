package io.reactivej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * @author heartup@gmail.com
 */
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

	void sendMessage(ReactiveRef receiver, Envelope envlop) {
		send(receiver.getHost(), receiver.getPort(), envlop);
	}

	void receiveMessage(ReactiveRef receiver, Envelope envlop) {
		if (logger.isDebugEnabled()) {
				logger.debug("receive message [" + envlop.toString() + "]");
		}

		final ReactiveCell compCell = receiver.getCell();
		if (compCell == null) {
			throw new ReactiveException("non existing component");
		}

		compCell.getDispatcher().dispatch(compCell, envlop);
	}

	public void receive(Envelope env) {
		ReactiveRef receiver = getSystem().findComponent(env.getReceiver().getHost(), env.getReceiver().getPort(), env.getReceiver().getPath());
		ReactiveRef sender = getSystem().findComponent(env.getSender().getHost(), env.getSender().getPort(), env.getSender().getPath());

		env.setReceiver(receiver);
		env.setSender(sender);

		receiveMessage(receiver, env);
	}

	public abstract void suspendReadingMessage();

	public abstract void resumeReadingMessage();
}
