package io.github.pdvrieze.pma.agfil.services

import nl.adaptivity.process.engine.pma.models.UseAuthScope

sealed class AgfilPermissions {

    object PICK_GARAGE: AgfilPermissions(), UseAuthScope

}
