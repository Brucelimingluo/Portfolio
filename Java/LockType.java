package edu.berkeley.cs186.database.concurrency;

/**
 * Utility methods to track the relationships between different lock types.
 */
public enum LockType {
    S,   // shared
    X,   // exclusive
    IS,  // intention shared
    IX,  // intention exclusive
    SIX, // shared intention exclusive
    NL;  // no lock held

    /**
     * This method checks whether lock types A and B are compatible with
     * each other. If a transaction can hold lock type A on a resource
     * at the same time another transaction holds lock type B on the same
     * resource, the lock types are compatible.
     */
    public static boolean compatible(LockType a, LockType b) {
        if (a == null || b == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement
        switch (a) {
            case IS:
                switch(b) {
                    case X: return false;
                    default: return true;
                }

            case X:
                switch(b) {
                    case NL: return true;
                    default: return false;
                }
            case S:
                switch(b) {
                    case NL: return true;
                    case IS: return true;
                    case S: return true;
                    default: return false;
                }
            case IX:
                switch(b) {
                    case IS: return true;
                    case IX: return true;
                    case NL: return true;
                    default: return false;

                }
            case SIX:
                switch(b) {
                    case NL: return true;
                    case IS: return true;
                    default:return false;
                }
            case NL: return true;
            default: return true;
//            default: throw new UnsupportedOperationException("bad lock type");
        }
//        switch (a) {
//            case IS:
//                switch (b) {
//                    case IS:
//                        return true;
//                    case IX:
//                        return true;
//
//
//
//            }
        }

//        if (a == IX && ( b == IS || b == IX)) {
//            return true;
//        }
//        if (a == IS && (b == IS || b == IX)) {
//            return true;
//        }
//        return false;


    /**
     * This method returns the lock on the parent resource
     * that should be requested for a lock of type A to be granted.
     */
    public static LockType parentLock(LockType a) {
        if (a == null) {
            throw new NullPointerException("null lock type");
        }
        switch (a) {
        case S: return IS;
        case X: return IX;
        case IS: return IS;
        case IX: return IX;
        case SIX: return IX;
        case NL: return NL;
        default: throw new UnsupportedOperationException("bad lock type");
        }
    }

    /**
     * This method returns if parentLockType has permissions to grant a childLockType
     * on a child.
     */
    public static boolean canBeParentLock(LockType parentLockType, LockType childLockType) {
        if (parentLockType == null || childLockType == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement
        switch (parentLockType) {
            case IS:
                switch(childLockType) {
                    case S: return true;
                    case IS: return true;
                    case NL: return true;
                    default: return false;
                }

            case X:
                switch(childLockType) {
                    case NL: return true;
                    default: return false;
                }
            case S:
                switch(childLockType) {
                    case NL: return true;
                    default: return false;
                }
            case IX:
                return true;
            case SIX:
                switch(childLockType) {
                    case IX: return true;
                    case NL: return true;
                    default:return false;
                }
            case NL:
                switch(childLockType) {
                    case NL:
                        return true;
                    default:
                        return false;
                }
        }
        return false;
    }

    /**
     * This method returns whether a lock can be used for a situation
     * requiring another lock (e.g. an S lock can be substituted with
     * an X lock, because an X lock allows the transaction to do everything
     * the S lock allowed it to do).
     */
    public static boolean substitutable(LockType substitute, LockType required) {
        if (required == null || substitute == null) {
            throw new NullPointerException("null lock type");
        }
        // TODO(proj4_part1): implement
        switch (required) {
            case IS:
                switch(substitute) {
                    case NL: return false;
                    case S: return false;
                    default: return true;
                }
            case X:
                switch(substitute) {
                    case X: return true;
                    default: return false;
                }
            case S:
                switch(substitute) {
                    case S: return true;
                    case SIX: return true;
                    case X: return true;
                    default: return false;
                }
            case IX:
                switch(substitute) {
                    case IX:
                        return true;
                    default:
                        return false;
                }
            case SIX:
                switch(substitute) {
                    case IX: return true;
                    case SIX: return true;
                    case S: return true;
                    default: return false;
                }
            case NL:
                switch(substitute) {
                    case NL: return true;
                    default: return false;
                }
        }


        return true;
    }

    /**
     * @return True if this lock is IX, IS, or SIX. False otherwise.
     */
    public boolean isIntent() {
        return this == LockType.IX || this == LockType.IS || this == LockType.SIX;
    }

    @Override
    public String toString() {
        switch (this) {
        case S: return "S";
        case X: return "X";
        case IS: return "IS";
        case IX: return "IX";
        case SIX: return "SIX";
        case NL: return "NL";
        default: throw new UnsupportedOperationException("bad lock type");
        }
    }
}

