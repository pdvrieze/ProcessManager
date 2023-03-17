package nl.adaptivity.process.engine

import net.devrieze.util.security.SecurityProvider

enum class ProcessEnginePermissions : SecurityProvider.Permission {
    /** Attempt to find a model */
    FIND_MODEL,
    ADD_MODEL,
    ASSIGN_OWNERSHIP,
    VIEW_ALL_INSTANCES,
    CANCEL_ALL,
    UPDATE_MODEL,
    LIST_MODELS,
    CHANGE_OWNERSHIP,
    VIEW_INSTANCE,
    CANCEL,
    LIST_INSTANCES,
    TICKLE_INSTANCE,
    TICKLE_NODE,
    START_PROCESS,
    /** The user is allowed to be assigned to the activity */
    ASSIGNED_TO_ACTIVITY
    ;
}
