package nl.adaptivity.process.processModel

/**
 * Interface to represent the information needed to authenticate the message to the receiver.
 */
interface AuthorizationInfo {
    interface IdSecret : AuthorizationInfo {
        val id: String
        val secret: String
    }

    interface Token : AuthorizationInfo {
        val token: String
    }
}
