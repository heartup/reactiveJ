package io.reactivej;

import java.io.Serializable;

public class ReactiveRef implements Serializable {
	
	private String path;
	private String host;
	private int port;

	private transient ReactiveCell cell;
	
	public ReactiveRef(String path, String host, int port) {
		this.path = path;
		this.host = host;
		this.port = port;
	}

	public void setCell(ReactiveCell cell) {
		this.cell = cell;
	}

	public ReactiveCell getCell() {
		return cell;
	}

	public String getPath() {
		return path;
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void tell(Serializable message, ReactiveRef sender) {
		getCell().tell(message, sender);
	}

	public ReactiveRef createReactiveComponent(String childName, boolean useGlobalDispatcher, String className) {
		return getCell().createChild(childName, useGlobalDispatcher, className);
	}

	@Override
	public String toString() {
		return host + ":" + port + path;
	}
}
