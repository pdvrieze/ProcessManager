/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.messaging

import net.devrieze.util.security.AuthenticationNeededException
import net.devrieze.util.security.PermissionDeniedException
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.rest.annotations.HttpMethod
import nl.adaptivity.util.HttpMessage
import nl.adaptivity.ws.rest.RestMessageHandler
import nl.adaptivity.ws.soap.SoapMessageHandler

import jakarta.servlet.ServletConfig
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.io.FileNotFoundException
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger


/**
 * A servlet that serves up web services provided by a [GenericEndpoint]
 *
 * @author Paul de Vrieze
 */
open class EndpointServlet : HttpServlet {

    /**
     * Override this to override the endpoint used by this servlet to serve
     * connections. In most cases it's better to provide the endpoint to the
     * constructor instead, or as a servlet parameter.
     *
     * @return A GenericEndpoint that implements the needed services.
     * @see {@link .init
     */
    protected var endpointProvider: GenericEndpoint? = null
        private set

    /**
     * Get a soap handler that does the work for us. As the handler caches objects
     * instead of repeatedly using reflection it needs to be an object and is not
     * just a set of static methods.
     *
     * @return The soap handler.
     */
    private val soapMessageHandler: SoapMessageHandler by lazy { SoapMessageHandler.newInstance(endpointProvider!!) }

    /**
     * Get a rest handler that does the work for us. As the handler caches objects
     * instead of repeatedly using reflection it needs to be an object and is not
     * just a set of static methods.
     *
     * @return The rest handler.
     */
    private val restMessageHandler: RestMessageHandler by lazy { RestMessageHandler.newInstance(endpointProvider!!) }

    /**
     * Default constructor that allows this servlet to be instantiated directly in
     * tomcat. This will set the endpoint to the object itself if the object
     * implements GenericEndpoint. This allows servlets to provide services by
     * deriving from [EndpointServlet]
     */
    constructor() {
        if (this is GenericEndpoint) {
            endpointProvider = this
        }
    }

    /**
     * A constructor for subclasses that provide an endpoint to use.
     *
     * @param endpoint The endpoint to provide.
     */
    protected constructor(endpoint: GenericEndpoint) {
        endpointProvider = endpoint
    }

    /**
     * Handle DELETE requests.
     */
    @Throws(ServletException::class, IOException::class)
    override fun doDelete(req: HttpServletRequest, resp: HttpServletResponse) {
        processRestSoap(HttpMethod.DELETE, req, resp)
    }

    /**
     * Handle GET requests.
     */
    @Throws(ServletException::class, IOException::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        processRestSoap(HttpMethod.GET, req, resp)
    }

    /**
     * Handle HEAD requests.
     */
    @Throws(ServletException::class, IOException::class)
    override fun doHead(req: HttpServletRequest, resp: HttpServletResponse) {
        processRestSoap(HttpMethod.HEAD, req, resp)
    }

    /**
     * Handle POST requests.
     */
    @Throws(ServletException::class, IOException::class)
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        processRestSoap(HttpMethod.POST, req, resp)
    }

    /**
     * Handle PUT requests.
     */
    @Throws(ServletException::class, IOException::class)
    override fun doPut(req: HttpServletRequest, resp: HttpServletResponse) {
        processRestSoap(HttpMethod.PUT, req, resp)
    }

    /**
     * Method that does the actual work of processing requests. It will, based on
     * the Content-Type header deterimine whether it's a rest or soap request, and
     * use a [SoapMessageHandler] or [RestMessageHandler] to actually
     * process the message.
     *
     * @param method The HTTP method invoked.
     * @param request The request.
     * @param response The response object on which responses are written.
     * @todo In case we have a soap request, respond with a proper SOAP fault, not
     * a generic error message.
     */
    private fun processRestSoap(method: HttpMethod, request: HttpServletRequest, response: HttpServletResponse) {
        try {
            request.authenticate(response) // Try to authenticate
            val message = HttpMessage(request)
            var soap = false
            try {
                try {
                    if (!SoapMessageHandler.isSoapMessage(request)) {
                        val restHandler = restMessageHandler
                        if (!restHandler!!.processRequest(method, message, response)) {
                            logger.warning("Error processing rest request " + request.requestURI)
                        }
                    } else {
                        soap = true
                        val soapHandler = soapMessageHandler
                        if (!soapHandler!!.processRequest(message, response)) {
                            logger.warning("Error processing soap request " + request.requestURI)
                        }
                    }
                } catch (e: MessagingException) {
                    if (e.cause is Exception) {
                        //            getLogger().log(Level.WARNING, "MessagingException "+e.getMessage(), e);
                        throw e.cause as Exception
                    } else {
                        throw e
                    }
                }

            } catch (e: AuthenticationNeededException) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No authenticated user.")
                logger.log(Level.WARNING, "Access attempted without authentication for " + request.requestURI)
            } catch (e: PermissionDeniedException) {
                response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "This user is not allowed to perform the requested action."
                                  )
                logger.log(
                    Level.WARNING,
                    "Access attempted without authorization by " + request.userPrincipal + " for " + request.requestURI,
                    e
                          )
            } catch (e: FileNotFoundException) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested resource is not available.")
                val logger = logger
                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Access to an invalid resource attempted: " + request.requestURI, e)
                } else {
                    logger.log(Level.WARNING, "Access to an invalid resource attempted: " + request.requestURI)
                }
            } catch (e: HttpResponseException) {
                response.sendError(e.responseCode, e.message)
                logger.log(Level.SEVERE, "Error in processing the request for " + request.requestURI, e)
            }

        } catch (e: Exception) {
            try {
                logger.log(Level.WARNING, "Error when processing REST/SOAP (" + request.requestURI + ")", e)
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.message)
            } catch (e1: Exception) {
                e1.addSuppressed(e)
                logger.log(Level.WARNING, "Failure to notify client of error", e)
            }

        }

    }

    /**
     * Initialize the servlet. If there is an `endpoint` parameter to
     * the servlet this will update the [endpoint][.getEndpointProvider]
     * used. If getEndpointProvider is overridden, that will still reflect the
     * actually used endpoint.
     */
    @Throws(ServletException::class)
    override fun init(config: ServletConfig) {
        super.init(config)
        val className = config.getInitParameter("endpoint")
        if (className == null && endpointProvider === null) {
            throw ServletException("The EndpointServlet needs to be configured with an endpoint parameter.")
        }
        if (endpointProvider === null || className != null) {
            val clazz: Class<out GenericEndpoint>
            try {
                clazz = Class.forName(className).asSubclass(GenericEndpoint::class.java)
            } catch (e: ClassNotFoundException) {
                throw ServletException(e)
            } catch (e: ClassCastException) {
                throw ServletException(
                    "The endpoint for an EndpointServlet needs to implement " + GenericEndpoint::class.java.name
                        + " the class given is " + className, e
                                      )
            }

            try {
                endpointProvider = clazz.newInstance()
                endpointProvider!!.initEndpoint(config)
            } catch (e: InstantiationException) {
                throw ServletException(e)
            } catch (e: IllegalAccessException) {
                throw ServletException(e)
            }

        } else {
            endpointProvider!!.initEndpoint(config)
        }
    }

    override fun destroy() {
        endpointProvider?.destroy()

        super.destroy()
    }

    companion object {

        private val LOGGER_NAME = EndpointServlet::class.java.name

        private val serialVersionUID = 5882346515807438320L

        /**
         * Get a logger object for this servlet.
         *
         * @return A logger to use to log messages.
         */
        private val logger: Logger
            get() = Logger.getLogger(LOGGER_NAME)
    }

}
