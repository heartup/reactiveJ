package io.reactivej;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class Envelope implements Serializable {

	private ReactiveRef receiver;
	private Serializable message;
	private ReactiveRef sender;
	
	public Envelope() {
	}

	public Envelope(ReactiveRef receiver, Serializable message, ReactiveRef sender) {
		this.receiver = receiver;
		this.message = message;
		this.sender = sender;
	}
	
	public ReactiveRef getReceiver() {
		return receiver;
	}
	
	public Serializable getMessage() {
		return message;
	}
	
	public ReactiveRef getSender() {
		return sender;
	}

	public void setReceiver(ReactiveRef receiver) {
		this.receiver = receiver;
	}

	public void setSender(ReactiveRef sender) {
		this.sender = sender;
	}

	@Override
	public String toString() {
		return getMessage().toString() + " "
				+ (getSender() == null ? "?" : getSender().toString()) + "->" + getReceiver().toString();
	}
}
