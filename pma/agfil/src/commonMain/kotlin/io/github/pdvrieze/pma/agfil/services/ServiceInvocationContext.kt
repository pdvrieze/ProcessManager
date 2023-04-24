package io.github.pdvrieze.pma.agfil.services

import nl.adaptivity.process.engine.pma.PmaAuthToken
import nl.adaptivity.process.engine.pma.models.Service

class ServiceInvocationContext<S: Service>(val service: S, val serviceAccessToken: PmaAuthToken)
