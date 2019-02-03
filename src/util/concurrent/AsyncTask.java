/*
 * Modified from Project Chromium
 *
 * @author Tomahawkd
 */

package util.concurrent;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncTask<Result> extends Observable {

	private AsyncTask.Delegate<Result> delegate;

	private FutureTask<Result> mFuture;

	private volatile Status mStatus = Status.PENDING;

	private final AtomicBoolean mCancelled = new AtomicBoolean();
	private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

	/**
	 * Indicates the current status of the task. Each status will be set only once
	 * during the lifetime of a task.
	 */
	public enum Status {
		/**
		 * Indicates that the task has not been executed yet.
		 */
		PENDING,
		/**
		 * Indicates that the task is running.
		 */
		RUNNING,
		/**
		 * Indicates that {@link AsyncTask.Delegate#onPostExecute} has finished.
		 */
		FINISHED,
	}

	public interface Delegate<Result> {
		/**
		 * Override this method to perform a computation on a background thread.
		 *
		 * @return A result, defined by the subclass of this task.
		 * @see #onPreExecute()
		 * @see #onPostExecute
		 */
		Result doInBackground() throws Exception;

		/**
		 * Runs on the main thread before {@link #doInBackground}.
		 *
		 * @see #onPostExecute
		 * @see #doInBackground
		 */
		default void onPreExecute() {}

		/**
		 * <p>Runs on the main thread after {@link #doInBackground}. The
		 * specified result is the value returned by {@link #doInBackground}.</p>
		 *
		 * <p>This method won't be invoked if the task was cancelled.</p>
		 *
		 * @param result The result of the operation computed by {@link #doInBackground}.
		 * @see #onPreExecute
		 * @see #doInBackground
		 * @see #onCancelled(Object)
		 */
		@SuppressWarnings("UnusedParameters")
		default void onPostExecute(Result result) {}

		/**
		 * <p>Runs on the main thread after {@link #cancel(boolean)} is invoked and
		 * {@link #doInBackground()} has finished.</p>
		 *
		 * <p>The default implementation simply invokes {@link #onCancelled()} and
		 * ignores the result. If you write your own implementation, do not call
		 * <code>super.onCancelled(result)</code>.</p>
		 *
		 * @param result The result, if any, computed in
		 *               {@link #doInBackground()}, can be null
		 * @see #cancel(boolean)
		 * @see #isCancelled()
		 */
		@SuppressWarnings("UnusedParameters")
		default void onCancelled(Result result) {
			onCancelled();
		}

		/**
		 * <p>Applications should preferably override {@link #onCancelled(Object)}.
		 * This method is invoked by the default implementation of
		 * {@link #onCancelled(Object)}.</p>
		 *
		 * <p>Runs on the main thread after {@link #cancel(boolean)} is invoked and
		 * {@link #doInBackground()} has finished.</p>
		 *
		 * @see #onCancelled(Object)
		 * @see #cancel(boolean)
		 * @see #isCancelled()
		 */
		default void onCancelled() {}
	}

	public AsyncTask(AsyncTask.Delegate<Result> delegate) {
		initializeTask(delegate);
	}

	/**
	 * <p>Initialize task for reuse convenience.</p>
	 * <p>Warning: Check status first, if the task is running, you cannot change
	 * the delegate using initialize method.</p>
	 * @param delegate task delegate {@link AsyncTask.Delegate}
	 */
	private void initializeTask(AsyncTask.Delegate<Result> delegate) {
		if (mStatus != Status.RUNNING) {
			this.delegate = delegate;

			Callable<Result> mWorker = () -> {
				mTaskInvoked.set(true);
				Result result = null;
				try {
					result = delegate.doInBackground();
				} catch (Exception tr) {
					mCancelled.set(true);
					throw tr;
				} finally {
					postResult(result);
				}

				return result;
			};

			mFuture = new NamedFutureTask(mWorker);
		} else {
			throw new IllegalStateException("Cannot Initialize task: the task is still running");
		}
	}

	/**
	 * <p>Initialize task for reuse convenience. Returns self for chaining.</p>
	 * <p>Warning: Check status first, if the task is running, you cannot change
	 * the delegate using initialize method.</p>
	 * @param delegate task delegate {@link AsyncTask.Delegate}.
	 *
	 * @return self for chaining.
	 */
	@SuppressWarnings("UnusedReturnValue")
	public AsyncTask<Result> initializeTaskForChaining(AsyncTask.Delegate<Result> delegate) {
		initializeTask(delegate);
		return this;
	}

	/**
	 * Returns the current status of this task.
	 *
	 * @return The current status.
	 */
	public final Status getStatus() {
		return mStatus;
	}

	/**
	 * Returns <tt>true</tt> if this task was cancelled before it completed
	 * normally. If you are calling {@link #cancel(boolean)} on the task,
	 * the value returned by this method should be checked periodically from
	 * {@link AsyncTask.Delegate#doInBackground()} to end the task as soon as possible.
	 *
	 * @return <tt>true</tt> if task was cancelled before it completed
	 * @see #cancel(boolean)
	 */
	public final boolean isCancelled() {
		return mCancelled.get();
	}

	/**
	 * <p>Attempts to cancel execution of this task.  This attempt will
	 * fail if the task has already completed, already been cancelled,
	 * or could not be cancelled for some other reason. If successful,
	 * and this task has not started when <tt>cancel</tt> is called,
	 * this task should never run. If the task has already started,
	 * then the <tt>mayInterruptIfRunning</tt> parameter determines
	 * whether the thread executing this task should be interrupted in
	 * an attempt to stop the task.</p>
	 *
	 * <p>Calling this method will result in {@link AsyncTask.Delegate#onCancelled(Object)}
	 * being invoked on the main thread after {@link AsyncTask.Delegate#doInBackground()}
	 * returns. Calling this method guarantees that {@link AsyncTask.Delegate#onPostExecute(Object)}
	 * is never invoked. After invoking this method, you should check the
	 * value returned by {@link #isCancelled()} periodically from
	 * {@link AsyncTask.Delegate#doInBackground()} to finish the task as early as
	 * possible.</p>
	 *
	 * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
	 *                              task should be interrupted; otherwise, in-progress tasks are allowed
	 *                              to complete.
	 * @return <tt>false</tt> if the task could not be cancelled,
	 * typically because it has already completed normally;
	 * <tt>true</tt> otherwise
	 * @see #isCancelled()
	 * @see AsyncTask.Delegate#onCancelled(Object)
	 */
	public final boolean cancel(boolean mayInterruptIfRunning) {
		mCancelled.set(true);
		return mFuture.cancel(mayInterruptIfRunning);
	}

	/**
	 * Executes the task with the specified parameters. The task returns
	 * itself (this) so that the caller can keep a reference to it.
	 *
	 * <p>This method is typically used with {@link Executor} to allow
	 * multiple tasks to run in parallel on a pool of threads managed by
	 * AsyncTask.
	 *
	 * <p><em>Warning:</em> Allowing multiple tasks to run in parallel from
	 * a thread pool is generally <em>not</em> what one wants, because the order
	 * of their operation is not defined.  For example, if these tasks are used
	 * to modify any state in common (such as writing a file due to a button click),
	 * there are no guarantees on the order of the modifications.
	 * Without careful work it is possible in rare cases for the newer version
	 * of the data to be over-written by an older one, leading to obscure data
	 * loss and stability issues.
	 *
	 * <p>This method must be invoked on the main thread.
	 *
	 * @param exec The executor to use.
	 * @throws IllegalStateException If {@link #getStatus()} returns either
	 *                               {@link AsyncTask.Status#RUNNING} or {@link AsyncTask.Status#FINISHED}.
	 */
	@SuppressWarnings({"MissingCasesInEnumSwitch"})
	public final void executeOnExecutor(Executor exec) {
		if (mStatus != Status.PENDING) {
			switch (mStatus) {
				case RUNNING:
					throw new IllegalStateException("Cannot execute task:"
							+ " the task is already running.");
				case FINISHED:
					throw new IllegalStateException("Cannot execute task:"
							+ " the task has already been executed "
							+ "(a task can be executed only once)");
			}
		}

		mStatus = Status.RUNNING;

		delegate.onPreExecute();

		exec.execute(mFuture);
	}

	/**
	 * <p>Convenient for chaining call.</p>
	 * <p>eg. <code>asyncTask.addObserverForChaining(o).executeOnExecutor(exec);</code></p>
	 *
	 * @param o observer
	 * @return self for chaining
	 */
	@Contract("_ -> this")
	public final AsyncTask<Result> addObserverForChaining(Observer o) {
		addObserver(o);
		return this;
	}

	/**
	 * <p>Convenient for chaining call.</p>
	 * <p>eg. <code>asyncTask.addObserverForChaining(o).executeOnExecutor(exec);</code></p>
	 *
	 * @param collection collections of observers
	 * @return self for chaining
	 */
	@Contract("_ -> this")
	public final AsyncTask<Result> addObserverForChaining(@NotNull Collection<Observer> collection) {
		collection.forEach(this::addObserver);
		return this;
	}

	/**
	 * <p>Convenient for chaining call.</p>
	 * <p>eg. <code>asyncTask.addObserverForChaining(o).executeOnExecutor(exec);</code></p>
	 *
	 * @param collection collections of observers
	 * @return self for chaining
	 */
	@Contract("_ -> this")
	public final AsyncTask<Result> addObserverForChaining(@NotNull Observer[] collection) {
		Arrays.stream(collection).forEach(this::addObserver);
		return this;
	}

	private void finish(Result result) {
		if (isCancelled()) {
			delegate.onCancelled(result);
		} else {
			delegate.onPostExecute(result);
		}
		mStatus = Status.FINISHED;
		notifyObservers(result);

	}

	private void postResult(Result result) {
		finish(result);
	}

	private void postResultIfNotInvoked(Result result) {
		final boolean wasTaskInvoked = mTaskInvoked.get();
		if (!wasTaskInvoked) {
			postResult(result);
		}
	}

	class NamedFutureTask extends FutureTask<Result> {
		NamedFutureTask(Callable<Result> c) {
			super(c);
		}

		Class getBlamedClass() {
			return AsyncTask.this.getClass();
		}

		@Override
		protected void done() {
			try {
				postResultIfNotInvoked(get());
			} catch (InterruptedException e) {
				// TODO Log implement
			} catch (ExecutionException e) {
				throw new RuntimeException(
						"An error occurred while executing doInBackground()", e.getCause());
			} catch (CancellationException e) {
				postResultIfNotInvoked(null);
			}
		}
	}
}
