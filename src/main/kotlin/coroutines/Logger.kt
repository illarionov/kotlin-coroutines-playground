package coroutines

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal val log: Logger = LoggerFactory.getLogger("CScopes")

internal fun log(msg: String?) = log.info(msg)

