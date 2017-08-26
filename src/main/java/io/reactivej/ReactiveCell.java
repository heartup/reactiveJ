package io.reactivej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactiveCell implements ReactiveContext {

	public static Logger logger = LoggerFactory.getLogger(ReactiveCell.class);
	
	private ReactiveComponent component;

	private ReactiveSystem system;

	private Dispatcher dispatcher;

	private ReactiveRef self;

	private ReactiveRef parent;

	private Map<String, ReactiveRef> children = new ConcurrentHashMap<String, ReactiveRef>();
	
	private Mailbox mailbox;

	private ReactiveRef sender;

	private Envelope currentMessage;

	public ReactiveCell(ReactiveComponent component, ReactiveRef ref, ReactiveRef parent, ReactiveSystem system, boolean useGlobalDispatcher) {
		this.component = component;
		this.self = ref;
		this.parent = parent;
		this.system  = system;
		if (useGlobalDispatcher)
			dispatcher = system.getDispatcher();
		else {
			dispatcher = new Dispatcher(Executors.newFixedThreadPool(1));  // only one thread
		}
		this.mailbox = new Mailbox(this);
	}

	public void destroyChild(String childName) {
		ReactiveRef child = getChild(childName);
		for (String cn : child.getCell().getChildren().keySet()) {
			child.getCell().destroyChild(cn);
		}

		getChildren().remove(childName);

		if (logger.isDebugEnabled())
			logger.debug("组件[{}]被销毁", getSelf().getPath() + ReactiveSystem.componentSplitter + childName);
	}

	public ReactiveComponent getComponent() {
		return component;
	}

	public void setSender(ReactiveRef sender) {
		this.sender = sender;
	}

	public Dispatcher getDispatcher() {
		return dispatcher;
	}

	public Mailbox getMailbox() {
		return mailbox;
	}

	@Override
	public Map<String, ReactiveRef> getChildren() {
		return children;
	}

	@Override
	public ReactiveRef getChild(String childName) {
		return children.get(childName);
	}

	@Override
	public ReactiveRef getParent() {
		return parent;
	}

	@Override
	public ReactiveRef getSelf() {
		return self;
	}

	@Override
	public ReactiveRef getSender() {
		return sender;
	}

	public Envelope getCurrentMessage() {
		return currentMessage;
	}

	public void setCurrentMessage(Envelope currentMessage) {
		this.currentMessage = currentMessage;
	}

	@Override
	public ReactiveSystem getSystem() {
		return system;
	}

	@Override
	public ReactiveRef createChild(String childName) {
		return system.createReactiveComponent(getSelf(), childName);
	}

	@Override
	public ReactiveRef createChild(String childName, boolean useGlobalDispatcher, String className) {
		return system.createReactiveComponent(getSelf(), childName, useGlobalDispatcher, className);
	}

	@Override
	public ReactiveRef createChild(String childName, boolean useGlobalDispatcher, String className, Object... params) {
		return system.createReactiveComponent(getSelf(), childName, useGlobalDispatcher, className, params);
	}

	public void tell(Serializable message, ReactiveRef sender) {
		system.sendMessage(getSelf(), message, sender);
	}

	@Override
	public ReactiveRef findComponent(String path) {
		return system.findComponent(path);
	}

	@Override
	public ReactiveRef findComponent(String host, int port, String path) {
		return system.findComponent(host, port, path);
	}

	@Override
	public ReactiveRef findSingleton(String singletonName) {
		return system.findSingleton(singletonName);
	}
}
