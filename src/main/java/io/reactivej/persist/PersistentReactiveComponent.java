package io.reactivej.persist;

import io.reactivej.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public abstract class PersistentReactiveComponent extends ReactiveComponent {

	private static Logger logger = LoggerFactory.getLogger(PersistentReactiveComponent.class);

	public class MessageAndHandler<T extends Serializable> {
		private T message;
		private Procedure<T> handler;

		public MessageAndHandler(T msg, Procedure<T> handler) {
			this.message = msg;
			this.handler = handler;
		}

		public Procedure<T> getHandler() {
			return handler;
		}

		public T getMessage() {
			return message;
		}
	}

	private long persistSequence = 0;

	private List<MessageAndHandler> pendingInvocations = new LinkedList<>();
	private List<MessageAndHandler> pendingSnapshotCallbacks = new LinkedList<>();
	private List<AtomicWrite> batchWriteEvents = new LinkedList<>();
	private List<Snapshot> batchSnapshots = new LinkedList<>();

	private ReactiveRef store;
	private ReactiveRef journal;
	private List<Envelope> stashedMessages = new LinkedList<>();

	private PersistentComponentState recoveringState = new RecoveringState(this);
	private PersistentComponentState persistingState = new PersistingState(this);
	private PersistentComponentState processingCommandState = new ProcessingCommandState(this);

	private PersistentComponentState currentState;
	
	public abstract Serializable getPersistentId();

	@Override
	public void preStart() {
		setCurrentState(getRecoveringState());
		getStore().tell(new StartRecovery(getPersistentId()), getSelf());
	}

	@Override
	public void receiveMessage(Serializable msg) throws Exception {
		getCurrentState().onMessage(msg);
	}

	public abstract AbstractComponentBehavior getRecoverBehavior();

	public void takeSnapshot(Serializable snapshot) {
		getStore().tell(new Snapshot(getPersistentId(), getPersistSequence(), snapshot, getSender()), getSelf());
	}

	public <T extends Serializable> void takeSnapshot(Serializable snapshot, final Procedure<T> handler) {
		pendingSnapshotCallbacks.add(new MessageAndHandler(snapshot, handler));
		Snapshot snapshotMsg = new Snapshot(getPersistentId(), getPersistSequence(), snapshot, getSender());
		batchSnapshots.add(snapshotMsg);
	}

	public <T extends Serializable> void persist(final T msg, final Procedure<T> handler) {
		List<T> msgs = new ArrayList<>();
		msgs.add(msg);

		persistAll(msgs, handler);
	}

	public <T extends Serializable> void persistAll(final List<T> batchMsg, final Procedure<T> handler) {
		for (Serializable msg : batchMsg) {
			pendingInvocations.add(new MessageAndHandler(msg, handler));
		}
		AtomicWrite writeEvent = new AtomicWrite(getPersistentId(), getPersistSequence(), (List<Serializable>) batchMsg, getSender());
		batchWriteEvents.add(writeEvent);
		setPersistSequence(getPersistSequence() + 1);
	}

	public void flushWriteEvents() {
		if (!batchWriteEvents.isEmpty()) {
			getJournal().tell(new WriteMessages(batchWriteEvents), getSelf());
			batchWriteEvents = new ArrayList<>();
		}
	}

	public void flushSnapshots() {
		if (!batchSnapshots.isEmpty()) {
			for (Snapshot ss : batchSnapshots) {
				getStore().tell(ss, getSelf());
			}
			batchSnapshots = new ArrayList<>();
		}
	}

	protected void stash() {
		Envelope msg = ((ReactiveCell) getContext()).getCurrentMessage();
		stashedMessages.add(msg);
		if (logger.isDebugEnabled())
			logger.debug("stash message [" + msg.toString() + "]");
	}

	protected void unstash() {
		if (stashedMessages.size() > 0) {
			Envelope msg = stashedMessages.remove(0);
			((ReactiveCell) getContext()).getMailbox().getQueue().addFirst(msg);
			if (logger.isDebugEnabled())
				logger.debug("unstash message [" + msg.toString() + "]");
		}
	}

	protected void unstashAll() {
		Collections.reverse(stashedMessages);
		while (stashedMessages.size() > 0) {
			unstash();
		}
	}

	public List<MessageAndHandler> getPendingInvocations() {
		return pendingInvocations;
	}

	public List<MessageAndHandler> getPendingSnapshotCallbacks() {
		return pendingSnapshotCallbacks;
	}

	public PersistentComponentState getRecoveringState() {
		return recoveringState;
	}

	public PersistentComponentState getPersistingState() {
		return persistingState;
	}

	public PersistentComponentState getProcessingCommandState() {
		return processingCommandState;
	}

	public long getPersistSequence() {
		return persistSequence;
	}

	public void setPersistSequence(long persistSequence) {
		this.persistSequence = persistSequence;
	}

	public void setCurrentState(PersistentComponentState currentState) {
		if (logger.isDebugEnabled())
			logger.debug(toString() + " state change to " + currentState.toString());
		this.currentState = currentState;
	}

	public PersistentComponentState getCurrentState() {
		return currentState;
	}

	public ReactiveRef getStore() {
		return store;
	}

	public void setStore(ReactiveRef store) {
		this.store = store;
	}

	public ReactiveRef getJournal() {
		return journal;
	}

	public void setJournal(ReactiveRef journal) {
		this.journal = journal;
	}
}
