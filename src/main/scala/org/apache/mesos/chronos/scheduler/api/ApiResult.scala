package org.apache.mesos.chronos.scheduler.api

import javax.ws.rs.core.Response

class ApiResult(
                 val message: String,
                 val status: String = Response.Status.BAD_REQUEST.toString
               ) {
}


