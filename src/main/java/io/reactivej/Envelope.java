package io.reactivej;

import io.netty.channel.Channel;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class Envelope implements Serializable {

	private ReactiveRef receiver;
	private Serializable message;
	private ReactiveRef sender;

	private transient Channel fromChannel;
	
	public Envelope() {
	}

	public Envelope(ReactiveRef receiver, Serializable message, ReactiveRef sender) {
		this.receiver = receiver;
		this.message = message;
		this.sender = sender;
	}

	public Envelope(ReactiveRef receiver, Serializable message, ReactiveRef sender, Channel fromChannel) {
		this(receiver, message, sender);
		this.fromChannel = fromChannel;
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

	public Channel getFromChannel() {
		return fromChannel;
	}

	public void setFromChannel(Channel fromChannel) {
		this.fromChannel = fromChannel;
	}

	@Override
	public String toString() {
		return getMessage().toString() + " "
				+ (getSender() == null ? "?" : getSender().toString()) + "->" + getReceiver().toString();
	}
}
