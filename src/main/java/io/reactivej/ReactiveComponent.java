package io.reactivej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public abstract class ReactiveComponent {

	private static Logger logger = LoggerFactory.getLogger(ReactiveComponent.class);

	private ReactiveContext context;

	private AbstractComponentBehavior behavior;

	public void preStart() {
		become(getDefaultBehavior());
	}

	public void become(AbstractComponentBehavior behavior) {
		this.behavior = behavior;
	}

	public AbstractComponentBehavior getBehavior() {
		return behavior;
	}

	public void onSupervise(SystemMessage msg) {
		if (msg instanceof Failure) {
			logger.error("", ((Failure) msg).getCause());
		}
	}

	public void receiveMessage(Serializable msg) throws Exception {
		getBehavior().onMessage(msg);
	}

	public void setContext(ReactiveContext context) {
		this.context = context;
	}

	public ReactiveContext getContext() {
		return context;
	}

	public ReactiveRef getSender() {
		return context.getSender();
	}
	
	public ReactiveRef getSelf() { return context.getSelf(); }

	public abstract AbstractComponentBehavior getDefaultBehavior();
}
