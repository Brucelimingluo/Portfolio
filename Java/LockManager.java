package edu.berkeley.cs186.database.concurrency;

import edu.berkeley.cs186.database.TransactionContext;

import java.util.*;

/**
 * LockManager maintains the bookkeeping for what transactions have what locks
 * on what resources and handles queuing logic. The lock manager should generally
 * NOT be used directly: instead, code should call methods of LockContext to
 * acquire/release/promote/escalate locks.
 *
 * The LockManager is primarily concerned with the mappings between
 * transactions, resources, and locks, and does not concern itself with multiple
 * levels of granularity. Multigranularity is handled by LockContext instead.
 *
 * Each resource the lock manager manages has its own queue of LockRequest
 * objects representing a request to acquire (or promote/acquire-and-release) a
 * lock that could not be satisfied at the time. This queue should be processed
 * every time a lock on that resource gets released, starting from the first
 * request, and going in order until a request cannot be satisfied. Requests
 * taken off the queue should be treated as if that transaction had made the
 * request right after the resource was released in absence of a queue (i.e.
 * removing a request by T1 to acquire X(db) should be treated as if T1 had just
 * requested X(db) and there were no queue on db: T1 should be given the X lock
 * on db, and put in an unblocked state via Transaction#unblock).
 *
 * This does mean that in the case of:
 *    queue: S(A) X(A) S(A)
 * only the first request should be removed from the queue when the queue is
 * processed.
 */
public class LockManager {
    // transactionLocks is a mapping from transaction number to a list of lock
    // objects held by that transaction.
    private Map<Long, List<Lock>> transactionLocks = new HashMap<>();

    // resourceEntries is a mapping from resource names to a ResourceEntry
    // object, which contains a list of Locks on the object, as well as a
    // queue for requests on that resource.
    private Map<ResourceName, ResourceEntry> resourceEntries = new HashMap<>();

    // A ResourceEntry contains the list of locks on a resource, as well as
    // the queue for requests for locks on the resource.
    private class ResourceEntry {
        // List of currently granted locks on the resource.
        List<Lock> locks = new ArrayList<>();
        // Queue for yet-to-be-satisfied lock requests on this resource.
        Deque<LockRequest> waitingQueue = new ArrayDeque<>();

        // Below are a list of helper methods we suggest you implement.
        // You're free to modify their type signatures, delete, or ignore them.

        /**
         * Check if `lockType` is compatible with preexisting locks. Allows
         * conflicts for locks held by transaction with id `except`, which is
         * useful when a transaction tries to replace a lock it already has on
         * the resource.
         */
        public boolean checkCompatible(LockType lockType, long except) {
            // TODO(proj4_part1): implement
            if (!this.locks.isEmpty()) {
                for (Lock lock: this.locks) {
                    if (!LockType.compatible(lockType, lock.lockType) && lock.transactionNum != except) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Gives the transaction the lock `lock`. Assumes that the lock is
         * compatible. Updates lock on resource if the transaction already has a
         * lock.
         */
        public void grantOrUpdateLock(Lock lock) {
            // TODO(proj4_part1): implement
            Lock currLock = null;
            for (Lock tempLock: this.locks) { //check if trans has a lock
                if (tempLock.transactionNum == lock.transactionNum) {
                    currLock = tempLock;
                    break;
                }
            }

            if (currLock != null) {
                currLock.lockType = lock.lockType;
            } else {
                this.locks.add(lock);
                Long currTrans = lock.transactionNum;
                if (!LockManager.this.transactionLocks.containsKey(currTrans)) {
                    List<Lock> newLock = new ArrayList<>();
                    LockManager.this.transactionLocks.put(currTrans, newLock);
                }
                LockManager.this.transactionLocks.get(currTrans).add(lock);
            }
        }

        /**
         * Releases the lock `lock` and processes the queue. Assumes that the
         * lock has been granted before.
         */
        public void releaseLock(Lock lock) {
            // TODO(proj4_part1): implement
            if (this.locks.isEmpty()) { //No Null case
                return;
            }

            this.locks.remove(lock);
            Long currTrans = lock.transactionNum;
            LockManager.this.transactionLocks.get(currTrans).remove(lock);
            if (LockManager.this.transactionLocks.get(currTrans).isEmpty()) {
                LockManager.this.transactionLocks.remove(currTrans);
            }

            processQueue();
        }

        /**
         * Adds `request` to the front of the queue if addFront is true, or to
         * the end otherwise.
         */
        public void addToQueue(LockRequest request, boolean addFront) {
            // TODO(proj4_part1): implement
            if (this.waitingQueue == null) {
                this.waitingQueue = new ArrayDeque<>();
            }

            if (addFront) {
                this.waitingQueue.addFirst(request);
            } else {
                this.waitingQueue.addLast(request);
            }
        }

        /**
         * Grant locks to requests from front to back of the queue, stopping
         * when the next lock cannot be granted. Once a request is completely
         * granted, the transaction that made the request can be unblocked.
         */
        private void processQueue() {
            Iterator<LockRequest> requests = waitingQueue.iterator();

            // TODO(proj4_part1): implement
            for (LockRequest request: this.waitingQueue) {
                long currTrans = request.transaction.getTransNum();
                for (Lock lockInQueue: request.releasedLocks) {
                    if (lockInQueue.transactionNum == currTrans) {
                        if(!checkCompatible(request.lock.lockType, currTrans)) {
                            return;
                        }
                        break;
                    }
                }
                if(!checkCompatible(request.lock.lockType, -1)) {
                    return;
                } else {
                    this.waitingQueue.remove(request);
                }
                for (Lock existedLock: request.releasedLocks) {
                    Long currTransNum = existedLock.transactionNum;
                    LockManager.this.transactionLocks.get(currTransNum).remove(existedLock);
                    if (LockManager.this.transactionLocks.get(currTransNum).isEmpty()) {
                        LockManager.this.transactionLocks.remove(currTransNum);
                    }
                    getResourceEntry(existedLock.name).locks.remove(existedLock);
                }
                this.grantOrUpdateLock(request.lock);
                request.transaction.unblock();
            }

        }

        /**
         * Gets the type of lock `transaction` has on this resource.
         */
        public LockType getTransactionLockType(long transaction) {
            // TODO(proj4_part1): implement
            for(Lock lock: this.locks) {
                if(lock.transactionNum == transaction) {
                    return lock.lockType;
                }
            }

            return LockType.NL;
        }

        @Override
        public String toString() {
            return "Active Locks: " + Arrays.toString(this.locks.toArray()) +
                    ", Queue: " + Arrays.toString(this.waitingQueue.toArray());
        }
    }

    // You should not modify or use this directly.
    private Map<String, LockContext> contexts = new HashMap<>();

    /**
     * Helper method to fetch the resourceEntry corresponding to `name`.
     * Inserts a new (empty) resourceEntry into the map if no entry exists yet.
     */
    private ResourceEntry getResourceEntry(ResourceName name) {
        resourceEntries.putIfAbsent(name, new ResourceEntry());
        return resourceEntries.get(name);
    }

    /**
     * Acquire a `lockType` lock on `name`, for transaction `transaction`, and
     * releases all locks on `releaseNames` held by the transaction after
     * acquiring the lock in one atomic action.
     *
     * Error checking must be done before any locks are acquired or released. If
     * the new lock is not compatible with another transaction's lock on the
     * resource, the transaction is blocked and the request is placed at the
     * FRONT of the resource's queue.
     *
     * Locks on `releaseNames` should be released only after the requested lock
     * has been acquired. The corresponding queues should be processed.
     *
     * An acquire-and-release that releases an old lock on `name` should NOT
     * change the acquisition time of the lock on `name`, i.e. if a transaction
     * acquired locks in the order: S(A), X(B), acquire X(A) and release S(A),
     * the lock on A is considered to have been acquired before the lock on B.
     *
     * @throws DuplicateLockRequestException if a lock on `name` is already held
     * by `transaction` and isn't being released
     * @throws NoLockHeldException if `transaction` doesn't hold a lock on one
     * or more of the names in `releaseNames`
     */
    public void acquireAndRelease(TransactionContext transaction, ResourceName name,
                                  LockType lockType, List<ResourceName> releaseNames)
            throws DuplicateLockRequestException, NoLockHeldException {
        // TODO(proj4_part1): implement
        // You may modify any part of this method. You are not required to keep
        // all your code within the given synchronized block and are allowed to
        // move the synchronized block elsewhere if you wish.
        boolean shouldBlock = false;
        synchronized (this) {
            ResourceEntry entryName = getResourceEntry(name);
            if (!releaseNames.contains(name) && entryName.getTransactionLockType(transaction.getTransNum()) != LockType.NL) {
                throw new DuplicateLockRequestException("not processing, a lock is held");
            }

            Lock currLock = new Lock(name, lockType, transaction.getTransNum());
            LockRequest currRequest = new LockRequest(transaction, currLock);

            if (!entryName.waitingQueue.isEmpty() || !entryName.checkCompatible(lockType, transaction.getTransNum())) {
                List<Lock> locks = getLocks(transaction);
                currRequest.releasedLocks = new ArrayList<>();
                for (ResourceName rsName: releaseNames) {
                    for (Lock lock: locks) {
                        if (rsName == lock.name) {
                            currRequest.releasedLocks.add(lock);
                            break;
                        }
                    }
                }
                entryName.addToQueue(currRequest, true);
                entryName.processQueue();
                shouldBlock = true;
                transaction.prepareBlock();
            } else {
                ResourceEntry toRemoveLockEntry = null;
                boolean run = false;
                List<Lock> locks = this.getLocks(transaction);
                entryName.grantOrUpdateLock(currLock);
                for (ResourceName currName: releaseNames) {
                    if (currName.equals(currLock.name)) {
                        continue;
                    }
                    Lock targetLock = null;
                    for (Lock lock: locks) {
                        if (lock.name.equals(currName)) {
                            if (!currName.equals(currLock.name)) {
                                targetLock = lock;
                                run = true;
                                toRemoveLockEntry = getResourceEntry(currName);
                            }
                        }
                    }
                    if (!run && !currName.equals(currLock.name)) {
                        throw new NoLockHeldException("No hold lock on curr transaction");
                    }
                    if (toRemoveLockEntry != null) {
                        toRemoveLockEntry.releaseLock(targetLock);
                    }
                }
            }
        }
        if (shouldBlock) {
            transaction.block();
        }
    }

    /**
     * Acquire a `lockType` lock on `name`, for transaction `transaction`.
     *
     * Error checking must be done before the lock is acquired. If the new lock
     * is not compatible with another transaction's lock on the resource, or if there are
     * other transaction in queue for the resource, the transaction is
     * blocked and the request is placed at the **back** of NAME's queue.
     *
     * @throws DuplicateLockRequestException if a lock on `name` is held by
     * `transaction`
     */
    public void acquire(TransactionContext transaction, ResourceName name,
                        LockType lockType) throws DuplicateLockRequestException {
        // TODO(proj4_part1): implement
        // You may modify any part of this method. You are not required to keep all your
        // code within the given synchronized block and are allowed to move the
        // synchronized block elsewhere if you wish.

        boolean shouldBlock = false;
        synchronized (this) {
            ResourceEntry rsEntry = getResourceEntry(name);
            Lock currLock = new Lock(name, lockType, transaction.getTransNum());

            if (rsEntry.getTransactionLockType(transaction.getTransNum()) != LockType.NL) {
                throw (new DuplicateLockRequestException("Hold exactly same lock on the resource"));
            }

            if (!rsEntry.waitingQueue.isEmpty() || !rsEntry.checkCompatible(lockType, -1)) {
                shouldBlock = true;
                LockRequest currRequest = new LockRequest(transaction, currLock);
                rsEntry.addToQueue(currRequest, false);
                transaction.prepareBlock();
            } else {
                rsEntry.grantOrUpdateLock(currLock);
            }
        }

        if (shouldBlock) {
            transaction.block();
        }
    }

    /**
     * Release `transaction`'s lock on `name`. Error checking must be done
     * before the lock is released.
     *
     * The resource name's queue should be processed after this call. If any
     * requests in the queue have locks to be released, those should be
     * released, and the corresponding queues also processed.
     *
     * @throws NoLockHeldException if no lock on `name` is held by `transaction`
     */
    public void release(TransactionContext transaction, ResourceName name)
            throws NoLockHeldException {
        // TODO(proj4_part1): implement
        // You may modify any part of this method.
        synchronized (this) {
            Boolean release = false;
            Lock releaseEle = null;
            List<Lock> allCurrLock = this.getLocks(transaction);
            for (Lock ele: allCurrLock) {
                if (ele.name.equals(name)) {
                    release = true;
                    releaseEle = ele;
                }
            }

            if (release) {
                ResourceEntry resourceEntry = getResourceEntry(name);
                resourceEntry.releaseLock(releaseEle);
            } else {
                throw (new NoLockHeldException("No held lock on the resource"));
            }
        }
    }

    /**
     * Promote a transaction's lock on `name` to `newLockType` (i.e. change
     * the transaction's lock on `name` from the current lock type to
     * `newLockType`, if its a valid substitution).
     *
     * Error checking must be done before any locks are changed. If the new lock
     * is not compatible with another transaction's lock on the resource, the
     * transaction is blocked and the request is placed at the FRONT of the
     * resource's queue.
     *
     * A lock promotion should NOT change the acquisition time of the lock, i.e.
     * if a transaction acquired locks in the order: S(A), X(B), promote X(A),
     * the lock on A is considered to have been acquired before the lock on B.
     *
     * @throws DuplicateLockRequestException if `transaction` already has a
     * `newLockType` lock on `name`
     * @throws NoLockHeldException if `transaction` has no lock on `name`
     * @throws InvalidLockException if the requested lock type is not a
     * promotion. A promotion from lock type A to lock type B is valid if and
     * only if B is substitutable for A, and B is not equal to A.
     */
    public void promote(TransactionContext transaction, ResourceName name,
                        LockType newLockType)
            throws DuplicateLockRequestException, NoLockHeldException, InvalidLockException {
        // TODO(proj4_part1): implement
        // You may modify any part of this method.
        boolean shouldBlock = false;
        synchronized (this) {
            Boolean promote = false;
            Lock promoteEle = null;
            List<Lock> allCurrLock = this.getLocks(transaction);
            for (Lock ele: allCurrLock) {
                if (ele.name.equals(name)) {
                    promote = true;
                    promoteEle = ele;
                }
            }
            LockType currLock = this.getLockType(transaction, name);
            if (currLock== newLockType) {
                throw (new DuplicateLockRequestException("Same type lock already granted on the resource"));
            } if (!promote) {
                throw(new NoLockHeldException("No lock on the transaction"));
            } if (LockType.substitutable(newLockType, promoteEle.lockType) == false) {
                throw( new InvalidLockException("Not Substitutable with the current lock"));
            }

            ResourceEntry resourceEntry = getResourceEntry(name);
            if (resourceEntry.checkCompatible(newLockType, transaction.getTransNum())) {
                shouldBlock = true;
            }
            if (shouldBlock) {
                Lock requestNewLock = new Lock(name, newLockType, transaction.getTransNum());
                LockRequest  requestLock = new LockRequest(transaction, requestNewLock);
                resourceEntry.addToQueue(requestLock, true);
                transaction.prepareBlock();
            }
        }
        if (shouldBlock) {
            transaction.block();
        }
    }

    /**
     * Return the type of lock `transaction` has on `name` or NL if no lock is
     * held.
     */
    public synchronized LockType getLockType(TransactionContext transaction, ResourceName name) {
        // TODO(proj4_part1): implement

        ResourceEntry resourceEntry = getResourceEntry(name);
        if (resourceEntry != null) {
            return resourceEntry.getTransactionLockType(transaction.getTransNum());
        }
        return LockType.NL;
    }

    /**
     * Returns the list of locks held on `name`, in order of acquisition.
     */
    public synchronized List<Lock> getLocks(ResourceName name) {
        return new ArrayList<>(resourceEntries.getOrDefault(name, new ResourceEntry()).locks);
    }

    /**
     * Returns the list of locks held by `transaction`, in order of acquisition.
     */
    public synchronized List<Lock> getLocks(TransactionContext transaction) {
        return new ArrayList<>(transactionLocks.getOrDefault(transaction.getTransNum(),
                Collections.emptyList()));
    }

    /**
     * Creates a lock context. See comments at the top of this file and the top
     * of LockContext.java for more information.
     */
    public synchronized LockContext context(String name) {
        if (!contexts.containsKey(name)) {
            contexts.put(name, new LockContext(this, null, name));
        }
        return contexts.get(name);
    }

    /**
     * Create a lock context for the database. See comments at the top of this
     * file and the top of LockContext.java for more information.
     */
    public synchronized LockContext databaseContext() {
        return context("database");
    }
}
